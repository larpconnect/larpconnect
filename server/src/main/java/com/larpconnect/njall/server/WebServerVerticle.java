package com.larpconnect.njall.server;

import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
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
  private final Logger logger = LoggerFactory.getLogger(WebServerVerticle.class);
  private static final int PORT = 8080;

  WebServerVerticle() {
    // Default constructor
  }

  @Override
  public void start(Promise<Void> startPromise) {
    Path tempFile;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("openapi.yaml")) {
      if (in == null) {
        startPromise.fail("openapi.yaml not found on classpath");
        return;
      }
      tempFile = Files.createTempFile("openapi", ".yaml");
      Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
      tempFile.toFile().deleteOnExit();
    } catch (IOException e) {
      startPromise.fail(e);
      return;
    }

    OpenAPIContract.from(vertx, tempFile.toAbsolutePath().toString())
        .onFailure(startPromise::fail)
        .onSuccess(
            contract -> {
              RouterBuilder builder = RouterBuilder.create(vertx, contract);
              builder
                  .getRoute("MessageService_GetMessage")
                  .addHandler(
                      ctx -> {
                        Message message = Message.newBuilder().setMessageType("Greeting").build();
                        try {
                          String json = JsonFormat.printer().print(message);
                          ctx.json(new JsonObject(json));
                        } catch (RuntimeException e) {
                          logger.error("Failed to convert message to JSON", e);
                          ctx.fail(e);
                        } catch (java.io.IOException e) {
                          logger.error("Failed to convert message to JSON", e);
                          ctx.fail(e);
                        }
                      });

              Router router = builder.createRouter();
              vertx
                  .createHttpServer()
                  .requestHandler(router)
                  .listen(PORT)
                  .onSuccess(
                      server -> {
                        logger.info("HTTP server started on port {}", PORT);
                        startPromise.complete();
                      })
                  .onFailure(startPromise::fail);
            });
  }
}
