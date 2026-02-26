package com.larpconnect.njall.server.grpc;

import io.grpc.Server;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.grpc.VertxServerBuilder;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrpcVerticle extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(GrpcVerticle.class);
  private final GrpcMessageService grpcMessageService;
  private Server server;

  @Inject
  GrpcVerticle(GrpcMessageService grpcMessageService) {
    this.grpcMessageService = grpcMessageService;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    int port = config().getInteger("grpc.port", 9090);
    logger.info("Starting gRPC server on port {}", port);

    try {
      server =
          VertxServerBuilder.forAddress(vertx, "0.0.0.0", port)
              .addService(grpcMessageService)
              .build()
              .start();
      startPromise.complete();
    } catch (Exception e) {
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
