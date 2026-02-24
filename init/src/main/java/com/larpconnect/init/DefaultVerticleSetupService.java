package com.larpconnect.init;

import com.google.inject.Inject;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default implementation of {@link VerticleSetupService}. */
final class DefaultVerticleSetupService implements VerticleSetupService {
  private final Logger logger = LoggerFactory.getLogger(DefaultVerticleSetupService.class);
  private final ProtoCodecRegistry registry;
  private final VerticleFactory guiceVerticleFactory;

  @Inject
  DefaultVerticleSetupService(
      ProtoCodecRegistry registry, GuiceVerticleFactory guiceVerticleFactory) {
    this.registry = registry;
    this.guiceVerticleFactory = guiceVerticleFactory;
  }

  @Override
  public void setup(Vertx vertx) {
    logger.info("Setting up Vert.x instance");
    registry.register(vertx);
    vertx.registerVerticleFactory(guiceVerticleFactory);
  }
}
