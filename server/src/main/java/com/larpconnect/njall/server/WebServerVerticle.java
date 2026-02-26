package com.larpconnect.njall.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.grpc.VertxServerBuilder;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WebServerVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(WebServerVerticle.class);
  private static final int GRPC_PORT = 8080;
  private static final int WEB_PORT = 8081;

  @Inject
  WebServerVerticle() {}

  @Override
  public void start(Promise<Void> startPromise) {
    var grpcServer =
        VertxServerBuilder.forAddress(vertx, "0.0.0.0", GRPC_PORT)
            .addService(new DefaultMessageService())
            .build();

    try {
      grpcServer.start();
      logger.info("gRPC server started on port {}", GRPC_PORT);
      startWebServer(startPromise);
    } catch (IOException e) {
      logger.error("Failed to start gRPC server", e);
      startPromise.fail(e);
    }
  }

  private void startWebServer(Promise<Void> startPromise) {
    Buffer openApiBuffer;
    try (InputStream is = getClass().getClassLoader().getResourceAsStream("openapi.yaml")) {
      if (is != null) {
        openApiBuffer = Buffer.buffer(is.readAllBytes());
      } else {
        openApiBuffer = null;
        logger.warn("openapi.yaml not found in classpath");
      }
    } catch (IOException e) {
      logger.error("Failed to read openapi.yaml", e);
      startPromise.fail(e);
      return;
    }

    Router router = Router.router(vertx);
    if (openApiBuffer != null) {
      router
          .get("/openapi.yaml")
          .handler(
              ctx -> {
                ctx.response().putHeader("content-type", "text/yaml").end(openApiBuffer);
              });
    }

    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(WEB_PORT)
        .onSuccess(
            http -> {
              logger.info("Web server started on port {}", WEB_PORT);
              startPromise.complete();
            })
        .onFailure(
            err -> {
              logger.error("Failed to start Web server", err);
              startPromise.fail(err);
            });
  }
}
