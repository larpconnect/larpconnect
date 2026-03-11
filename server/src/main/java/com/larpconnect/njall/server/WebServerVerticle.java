package com.larpconnect.njall.server;

import static com.larpconnect.njall.common.annotations.ContractTag.PURE;

import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.annotations.BuildWith;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.ProtoDef;
import com.larpconnect.njall.server.annotations.OpenApiSpec;
import com.larpconnect.njall.server.annotations.WebPort;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Web server verticle that serves OpenAPI specification. */
@Singleton
@BuildWith(ServerModule.class)
final class WebServerVerticle extends AbstractVerticle {
  private static final int DEFAULT_PORT = 8080;
  private static final String DEFAULT_SPEC = "openapi.yaml";
  private static final JsonFormat.Printer PRINTER = JsonFormat.printer();
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
    this(DEFAULT_PORT, DEFAULT_SPEC);
  }

  WebServerVerticle(String openApiSpec) {
    this(DEFAULT_PORT, openApiSpec);
  }

  @Inject
  WebServerVerticle(
      @WebPort int port,
      @OpenApiSpec String openApiSpec,
      Optional<Consumer<Integer>> portListener) {
    this(port, openApiSpec, m -> PRINTER.print(m), portListener);
  }

  WebServerVerticle(int port, String openApiSpec) {
    this(port, openApiSpec, m -> PRINTER.print(m), Optional.empty());
  }

  WebServerVerticle(
      int port,
      String openApiSpec,
      Serializer serializer,
      Optional<Consumer<Integer>> portListener) {
    this.port = port;
    this.openApiSpec = openApiSpec;
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
              try (InputStream in = getClass().getClassLoader().getResourceAsStream(openApiSpec)) {
                if (in == null) {
                  throw new IOException(openApiSpec + " not found on classpath");
                }
                FileAttribute<Set<PosixFilePermission>> attr =
                    PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString("rw-------"));
                Path tempFile = Files.createTempFile("openapi", ".yaml", attr);
                try (OutputStream out = Files.newOutputStream(tempFile)) {
                  in.transferTo(out);
                }
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
      ctx.json(new JsonObject(json));
    } catch (RuntimeException | IOException e) {
      logger.error("Failed to convert message to JSON", e);
      ctx.fail(java.net.HttpURLConnection.HTTP_INTERNAL_ERROR, e);
    }
  }

  @AiContract(
      ensure = "ctx.response() \\text{ contains JSON Webfinger response}",
      implementationHint = "Returns a valid JSON response for a webfinger query")
  void handleWebfinger(RoutingContext ctx) {
    var response = new JsonObject().put("subject", "acct:system@localhost");
    ctx.json(response);
  }
}
