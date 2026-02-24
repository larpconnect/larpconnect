package com.larpconnect.njall.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MainVerticle extends AbstractVerticle implements ServerVerticle {
  private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  MainVerticle() {}

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("MainVerticle starting...");
    startPromise.complete();
    logger.info("MainVerticle started.");
  }
}
