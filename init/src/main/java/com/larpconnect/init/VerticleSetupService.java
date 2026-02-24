package com.larpconnect.init;

import io.vertx.core.Vertx;

/** Service to setup Vert.x instance with codecs and verticle factories. */
public interface VerticleSetupService {
  /**
   * Sets up the Vert.x instance.
   *
   * @param vertx the Vertx instance to setup
   */
  void setup(Vertx vertx);
}
