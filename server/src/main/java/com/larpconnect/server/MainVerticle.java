package com.larpconnect.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main Verticle for the server application. */
final class MainVerticle extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  MainVerticle() {}

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("MainVerticle started!");
    startPromise.complete();
  }
}
