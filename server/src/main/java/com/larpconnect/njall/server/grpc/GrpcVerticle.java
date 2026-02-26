package com.larpconnect.njall.server.grpc;

import io.grpc.Server;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.grpc.VertxServerBuilder;
import jakarta.inject.Inject;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrpcVerticle extends AbstractVerticle {
  private static final int DEFAULT_PORT = 9090;
  private final Logger logger = LoggerFactory.getLogger(GrpcVerticle.class);
  private final GrpcMessageService grpcMessageService;
  private Server server;

  @Inject
  GrpcVerticle(GrpcMessageService grpcMessageService) {
    this.grpcMessageService = grpcMessageService;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    int port = config().getInteger("grpc.port", DEFAULT_PORT);
    logger.info("Starting gRPC server on port {}", port);

    try {
      server =
          VertxServerBuilder.forAddress(vertx, "0.0.0.0", port)
              .addService(grpcMessageService)
              .build()
              .start();
      startPromise.complete();
    } catch (IOException e) {
      startPromise.fail(e);
    }
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (server != null) {
      server.shutdown();
    }
    stopPromise.complete();
  }
}
