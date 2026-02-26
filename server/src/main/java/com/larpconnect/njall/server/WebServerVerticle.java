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
  private final Logger logger = LoggerFactory.getLogger(WebServerVerticle.class);
  private static final int DEFAULT_GRPC_PORT = 8080;
  private static final int DEFAULT_WEB_PORT = 8081;
  private static final String DEFAULT_OPENAPI_PATH = "openapi.yaml";

  @Inject
  WebServerVerticle() {}

  @Override
  public void start(Promise<Void> startPromise) {
    startGrpcServer(startPromise);
  }

  private void startGrpcServer(Promise<Void> startPromise) {
    int grpcPort = config().getInteger("grpc.port", DEFAULT_GRPC_PORT);
    var grpcServer =
        VertxServerBuilder.forAddress(vertx, "0.0.0.0", grpcPort)
            .addService(new GrpcMessageService())
            .build();

    try {
      grpcServer.start(
          (v, err) -> {
            if (err == null) {
              logger.info("gRPC server started on port {}", grpcPort);
              startWebServer(startPromise);
            } else {
              logger.error("Failed to start gRPC server", err);
              startPromise.fail(err);
            }
          });
    } catch (RuntimeException e) {
      logger.error("Failed to start gRPC server", e);
      startPromise.fail(e);
    }
  }

  private void startWebServer(Promise<Void> startPromise) {
    Buffer openApiBuffer;
    String openApiPath = config().getString("openapi.path", DEFAULT_OPENAPI_PATH);
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(openApiPath)) {
      if (is == null) {
        String message = String.format("%s not found in classpath", openApiPath);
        logger.error(message);
        startPromise.fail(message);
        return;
      }
      openApiBuffer = Buffer.buffer(is.readAllBytes());
    } catch (IOException e) {
      logger.error("Failed to read {}", openApiPath, e);
      startPromise.fail(e);
      return;
    }

    Router router = Router.router(vertx);
    router
        .get("/openapi.yaml")
        .handler(
            ctx -> {
              ctx.response().putHeader("content-type", "text/yaml").end(openApiBuffer);
            });

    int webPort = config().getInteger("web.port", DEFAULT_WEB_PORT);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(webPort)
        .onSuccess(
            http -> {
              logger.info("Web server started on port {}", webPort);
              startPromise.complete();
            })
        .onFailure(
            err -> {
              logger.error("Failed to start Web server", err);
              startPromise.fail(err);
            });
  }
}
