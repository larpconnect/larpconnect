package com.larpconnect.njall.init;

import com.google.inject.Inject;
import com.google.inject.Injector;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class VerticleSetupService {
  private final Logger logger = LoggerFactory.getLogger(VerticleSetupService.class);

  @Inject
  VerticleSetupService(ProtoCodecRegistry protoCodecRegistry) {
    logger.info("Loaded ProtoCodecRegistry: {}", protoCodecRegistry);
  }

  void setup(Vertx vertx, Injector injector) {
    VerticleFactory guiceFactory = new GuiceVerticleFactory(injector);
    vertx.registerVerticleFactory(guiceFactory);
    vertx.eventBus().registerDefaultCodec(com.larpconnect.njall.proto.Message.class, injector.getInstance(ProtoCodecRegistry.class));
  }
}
