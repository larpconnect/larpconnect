package com.larpconnect.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main Verticle for the server application. */
public class MainVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  public MainVerticle() {}

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("MainVerticle started!");
    startPromise.complete();
  }
}
