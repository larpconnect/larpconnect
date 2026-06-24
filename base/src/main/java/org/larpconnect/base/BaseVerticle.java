package org.larpconnect.base;

import com.google.inject.Inject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles core domain events and business logic. */
final class BaseVerticle extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(BaseVerticle.class);

  @Inject
  BaseVerticle() {}

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("BaseVerticle started.");
    startPromise.complete();
  }
}
