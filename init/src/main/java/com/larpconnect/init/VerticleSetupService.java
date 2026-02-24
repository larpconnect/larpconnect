package com.larpconnect.init;

import com.google.inject.Inject;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service to setup Vert.x instance with codecs and verticle factories. */
public class VerticleSetupService {
  private static final Logger logger = LoggerFactory.getLogger(VerticleSetupService.class);
  private final ProtoCodecRegistry registry;
  private final GuiceVerticleFactory guiceVerticleFactory;

  @Inject
  public VerticleSetupService(
      ProtoCodecRegistry registry, GuiceVerticleFactory guiceVerticleFactory) {
    this.registry = registry;
    this.guiceVerticleFactory = guiceVerticleFactory;
  }

  /**
   * Sets up the Vert.x instance.
   *
   * @param vertx the Vertx instance to setup
   */
  public void setup(Vertx vertx) {
    logger.info("Setting up Vert.x instance");
    registry.register(vertx);
    vertx.registerVerticleFactory(guiceVerticleFactory);
  }
}
