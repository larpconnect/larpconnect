package com.larpconnect.njall.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultMainVerticle extends AbstractVerticle implements MainVerticle {
  private final Logger logger = LoggerFactory.getLogger(DefaultMainVerticle.class);

  DefaultMainVerticle() {}

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("DefaultMainVerticle starting...");
    startPromise.complete();
    logger.info("DefaultMainVerticle started.");
  }
}
