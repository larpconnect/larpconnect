package com.larpconnect.njall.server;

import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Web server verticle that serves OpenAPI specification. */
final class WebServerVerticle extends AbstractVerticle {
  private static final int DEFAULT_PORT = 8080;
  private final Logger logger = LoggerFactory.getLogger(WebServerVerticle.class);
  private final int port;
  private final String openApiSpec;
  private final Serializer serializer;

  interface Serializer {
    String print(Message message) throws IOException;
  }

  WebServerVerticle() {
    this(DEFAULT_PORT, "openapi.yaml");
  }

  WebServerVerticle(String openApiSpec) {
    this(DEFAULT_PORT, openApiSpec);
  }

  WebServerVerticle(int port, String openApiSpec) {
    this(port, openApiSpec, m -> JsonFormat.printer().print(m));
  }

  WebServerVerticle(int port, String openApiSpec, Serializer serializer) {
    this.port = port;
    this.openApiSpec = openApiSpec;
    this.serializer = serializer;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    Path tempFile;
    InputStream in = null;
    try {
      in = getClass().getClassLoader().getResourceAsStream(openApiSpec);
      if (in == null) {
        startPromise.fail(openApiSpec + " not found on classpath");
        return;
      }
      tempFile = Files.createTempFile("openapi", ".yaml");
      Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
      tempFile.toFile().deleteOnExit();
    } catch (IOException e) {
      startPromise.fail(e);
      return;
    } finally {
      try {
        com.google.common.io.Closeables.close(in, true);
      } catch (IOException e) {
        logger.warn("Failed to close stream", e);
      }
    }

    OpenAPIContract.from(vertx, tempFile.toAbsolutePath().toString())
        .onFailure(startPromise::fail)
        .onSuccess(
            contract -> {
              var builder = RouterBuilder.create(vertx, contract);
              builder.getRoute("MessageService_GetMessage").addHandler(this::handleGetMessage);

              var router = builder.createRouter();
              vertx
                  .createHttpServer()
                  .requestHandler(router)
                  .listen(port)
                  .onSuccess(
                      server -> {
                        logger.info("HTTP server started on port {}", port);
                        startPromise.complete();
                      })
                  .onFailure(startPromise::fail);
            });
  }

  // Package-private for testing
  void handleGetMessage(RoutingContext ctx) {
    var message = Message.newBuilder().setMessageType("Greeting").build();
    try {
      var json = serializer.print(message);
      ctx.json(new JsonObject(json));
    } catch (RuntimeException e) {
      logger.error("Failed to convert message to JSON", e);
      ctx.fail(e);
    } catch (IOException e) {
      logger.error("Failed to convert message to JSON", e);
      ctx.fail(e);
    }
  }
}
