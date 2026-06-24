package org.larpconnect.events;

import com.google.inject.Inject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point verticle for the Vert.x instance. */
public final class MainVerticle extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
  private final Set<VerticleProvider> verticleProviders;

  @Inject
  MainVerticle(Set<VerticleProvider> verticleProviders) {
    this.verticleProviders = verticleProviders;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("Starting MainVerticle, deploying registered verticles...");
    List<Future<String>> deployments =
        verticleProviders.stream()
            .map(provider -> vertx.deployVerticle(provider.getVerticle()))
            .collect(Collectors.toList());

    Future.all(deployments)
        .onSuccess(
            ar -> {
              logger.info("All registered verticles deployed successfully.");
              startPromise.complete();
            })
        .onFailure(
            err -> {
              logger.error("Failed to deploy all registered verticles", err);
              startPromise.fail(err);
            });
  }
}
