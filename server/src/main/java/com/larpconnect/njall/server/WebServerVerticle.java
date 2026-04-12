package com.larpconnect.njall.server;

import static com.larpconnect.njall.common.annotations.ContractTag.PURE;

import com.google.common.io.Resources;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.annotations.BuildWith;
import com.larpconnect.njall.proto.LarpConnectConfig;
import com.larpconnect.njall.proto.MessageReply;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.Nodeinfo22;
import com.larpconnect.njall.proto.NodeinfoJrd;
import com.larpconnect.njall.proto.Parameter;
import com.larpconnect.njall.proto.ProtoDef;
import com.larpconnect.njall.proto.WebfingerResponse;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Web server verticle that serves OpenAPI specification. */
@BuildWith(ServerModule.class)
final class WebServerVerticle extends AbstractVerticle {
  private static final int DEFAULT_PORT = 8080;
  private static final String DEFAULT_SPEC = "openapi.yaml";
  private final JsonFormat.Printer printer;
  private final Logger logger = LoggerFactory.getLogger(WebServerVerticle.class);
  private final int port;
  private final String openApiSpec;
  private final Serializer serializer;
  private final Optional<Consumer<Integer>> portListener;
  private HttpServer server;

  @AiContract(
      ensure = "returns JSON representation of message",
      tags = {PURE})
  interface Serializer {
    String print(MessageRequest message) throws IOException;
  }

  WebServerVerticle() {
    this(
        LarpConnectConfig.newBuilder()
            .setWebPort(DEFAULT_PORT)
            .setOpenapiSpec(DEFAULT_SPEC)
            .build(),
        Optional.empty());
  }

  WebServerVerticle(String openApiSpec) {
    this(
        LarpConnectConfig.newBuilder().setWebPort(DEFAULT_PORT).setOpenapiSpec(openApiSpec).build(),
        Optional.empty());
  }

  @Inject
  WebServerVerticle(LarpConnectConfig config, Optional<Consumer<Integer>> portListener) {
    this(config, m -> createPrinter().print(m), portListener);
  }

  WebServerVerticle(int port, String openApiSpec) {
    this(
        LarpConnectConfig.newBuilder().setWebPort(port).setOpenapiSpec(openApiSpec).build(),
        Optional.empty());
  }

  private static JsonFormat.Printer createPrinter() {
    var registry =
        TypeRegistry.newBuilder()
            .add(WebfingerResponse.getDescriptor())
            .add(NodeinfoJrd.getDescriptor())
            .add(Nodeinfo22.getDescriptor())
            .build();
    return JsonFormat.printer().usingTypeRegistry(registry);
  }

  WebServerVerticle(
      LarpConnectConfig config, Serializer serializer, Optional<Consumer<Integer>> portListener) {
    this.port = config.getWebPort();
    this.openApiSpec = config.getOpenapiSpec();
    this.printer = createPrinter();
    this.serializer = serializer;
    this.portListener = portListener;
  }

  public int actualPort() {
    if (server != null) {
      return server.actualPort();
    }
    return port;
  }

  @Override
  @AiContract(
      ensure = {
        "startPromise \\text{ is completed}",
        "startPromise \\text{ succeeded } \\implies server \\neq \\bot",
        "startPromise \\text{ succeeded } \\implies portListener \\text{ was invoked}"
      },
      implementationHint = "Loads OpenAPI spec, configures RouterBuilder, and starts HttpServer")
  public void start(Promise<Void> startPromise) {
    vertx
        .<String>executeBlocking(
            () -> {
              try (InputStream in = Resources.getResource(openApiSpec).openStream()) {
                var tempFile = Files.createTempFile("openapi", ".yaml");
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                tempFile.toFile().deleteOnExit();
                return tempFile.toAbsolutePath().toString();
              }
            })
        .onFailure(startPromise::fail)
        .onSuccess(
            tempFilePath -> {
              OpenAPIContract.from(vertx, tempFilePath)
                  .onFailure(startPromise::fail)
                  .onSuccess(
                      contract -> {
                        var builder = RouterBuilder.create(vertx, contract);
                        builder.getRoute("getMessage").addHandler(this::handleGetMessage);

                        var hc = HealthCheckHandler.create(vertx);
                        builder.getRoute("healthz").addHandler(hc);

                        builder.getRoute("webfinger").addHandler(this::handleWebfinger);
                        builder
                            .getRoute("nodeinfoWellKnown")
                            .addHandler(this::handleNodeinfoWellKnown);
                        builder.getRoute("nodeinfoAdmin").addHandler(this::handleNodeinfoAdmin);

                        var router = builder.createRouter();
                        vertx
                            .createHttpServer()
                            .requestHandler(router)
                            .listen(port)
                            .onSuccess(
                                server -> {
                                  this.server = server;
                                  int actualPort = server.actualPort();
                                  portListener.ifPresent(listener -> listener.accept(actualPort));
                                  logger.info("HTTP server started on port {}", actualPort);
                                  startPromise.complete();
                                })
                            .onFailure(startPromise::fail);
                      });
            });
  }

  // Package-private for testing
  @AiContract(
      ensure = "ctx.response() \\text{ contains JSON Greeting}",
      implementationHint = "Serializes a Greeting message to JSON and writes it to the response")
  void handleGetMessage(RoutingContext ctx) {
    var message =
        MessageRequest.newBuilder()
            .setProto(ProtoDef.newBuilder().setProtobufName("Greeting").build())
            .build();
    try {
      var json = serializer.print(message);
      ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(json);
    } catch (RuntimeException | IOException e) {
      logger.error("Failed to convert message to JSON", e);
      ctx.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, e);
    }
  }

  @AiContract(
      ensure = "ctx.response() \\text{ contains JSON Webfinger response}",
      implementationHint = "Returns a valid JSON response for a webfinger query")
  void handleWebfinger(RoutingContext ctx) {
    var requestBuilder = MessageRequest.newBuilder();
    ctx.queryParams()
        .forEach(
            (key, value) -> {
              requestBuilder.addParameters(
                  Parameter.newBuilder().setKey(key).setStringValue(value).build());
            });

    vertx
        .eventBus()
        .<MessageReply>request("http.well-known.webfinger.request", requestBuilder.build())
        .onSuccess(
            msg -> {
              try {
                var replyResponse = msg.body();
                var proto = replyResponse.getProto();
                var json = printer.print(proto.getMessage());
                ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(json);
              } catch (RuntimeException | IOException e) {
                logger.error("Failed to serialize webfinger response", e);
                ctx.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, e);
              }
            })
        .onFailure(
            e -> {
              logger.error("Webfinger request failed", e);
              ctx.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, e);
            });
  }

  @AiContract(
      ensure = "ctx.response() \\text{ contains JSON Nodeinfo JRD response}",
      implementationHint = "Returns a valid JSON response for a nodeinfo well-known query")
  void handleNodeinfoWellKnown(RoutingContext ctx) {
    vertx
        .eventBus()
        .<MessageReply>request(
            "http.well-known.nodeinfo.request", MessageRequest.getDefaultInstance())
        .onSuccess(
            msg -> {
              try {
                var replyResponse = msg.body();
                var proto = replyResponse.getProto();
                var json = printer.print(proto.getMessage());
                ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(json);
              } catch (RuntimeException | IOException e) {
                logger.error("Failed to serialize nodeinfo JRD response", e);
                ctx.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, e);
              }
            })
        .onFailure(
            e -> {
              logger.error("Nodeinfo well-known request failed", e);
              ctx.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, e);
            });
  }

  @AiContract(
      ensure = "ctx.response() \\text{ contains JSON Nodeinfo 2.2 response}",
      implementationHint = "Returns a valid JSON response for a nodeinfo admin query")
  void handleNodeinfoAdmin(RoutingContext ctx) {
    vertx
        .eventBus()
        .<MessageReply>request("http.admin.nodeinfo.request", MessageRequest.getDefaultInstance())
        .onSuccess(
            msg -> {
              try {
                var replyResponse = msg.body();
                var proto = replyResponse.getProto();
                var json = printer.print(proto.getMessage());
                ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(json);
              } catch (RuntimeException | IOException e) {
                logger.error("Failed to serialize nodeinfo 2.2 response", e);
                ctx.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, e);
              }
            })
        .onFailure(
            e -> {
              logger.error("Nodeinfo admin request failed", e);
              ctx.fail(HttpURLConnection.HTTP_INTERNAL_ERROR, e);
            });
  }
}
