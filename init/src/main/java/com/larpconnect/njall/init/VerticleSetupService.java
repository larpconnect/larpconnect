package com.larpconnect.njall.init;

import com.google.inject.Injector;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VerticleSetupService {
  private final Logger logger = LoggerFactory.getLogger(VerticleSetupService.class);
  private final AtomicReference<Vertx> vertxRef = new AtomicReference<>();

  @Inject
  VerticleSetupService(ProtoCodecRegistry protoCodecRegistry) {
    logger.info("Loaded ProtoCodecRegistry: {}", protoCodecRegistry);
  }

  void setup(Vertx vertx, Injector injector) {
    vertx.registerVerticleFactory(new GuiceVerticleFactory(injector));
    vertx.eventBus().registerDefaultCodec(Message.class, new ProtoCodecRegistry());
    vertxRef.set(vertx);
  }

  void deploy(Class<? extends Verticle> verticleClass) {
    var vertx = vertxRef.get();
    if (vertx != null) {
      vertx
          .deployVerticle("guice:" + verticleClass.getName())
          .onSuccess(
              id ->
                  logger.info(
                      "{} deployed successfully with ID: {}", verticleClass.getSimpleName(), id))
          .onFailure(
              err -> {
                logger.error("Failed to deploy verticle", err);
                throw new RuntimeException(
                    "Failed to deploy verticle " + verticleClass.getName(), err);
              });
    } else {
      throw new IllegalStateException("Vertx not initialized");
    }
  }
}
