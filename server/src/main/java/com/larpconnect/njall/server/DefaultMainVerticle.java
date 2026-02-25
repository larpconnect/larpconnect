package com.larpconnect.njall.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultMainVerticle extends AbstractVerticle implements MainVerticle {
  private final Logger logger = LoggerFactory.getLogger(DefaultMainVerticle.class);
  private final Set<Verticle> verticles;
  private final List<String> deploymentIds = new ArrayList<>();

  @Inject
  DefaultMainVerticle(Set<Verticle> verticles) {
    this.verticles = verticles;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("DefaultMainVerticle starting...");
    if (verticles.isEmpty()) {
      startPromise.complete();
      logger.info("DefaultMainVerticle started (no additional verticles).");
      return;
    }

    List<Future<?>> futures = new ArrayList<>();
    for (Verticle verticle : verticles) {
      futures.add(vertx.deployVerticle(verticle).onSuccess(deploymentIds::add));
    }

    Future.all(futures)
        .onSuccess(
            v -> {
              logger.info("All verticles deployed successfully.");
              startPromise.complete();
            })
        .onFailure(
            err -> {
              logger.error("Failed to deploy verticles", err);
              startPromise.fail(err);
            });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    logger.info("DefaultMainVerticle stopping...");
    // Vert.x automatically undeploys child verticles when the parent is undeployed.
    deploymentIds.clear();
    stopPromise.complete();
  }
}
