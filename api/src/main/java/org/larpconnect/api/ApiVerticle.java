package org.larpconnect.api;

import com.google.inject.Inject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles REST API routes and requests. */
final class ApiVerticle extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(ApiVerticle.class);

  @Inject
  ApiVerticle() {}

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("ApiVerticle started.");
    startPromise.complete();
  }
}
