package org.larpconnect.events;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

/** Main entry point verticle for the Vert.x instance. */
public final class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) {
    startPromise.complete();
  }
}
