package org.larpconnect.events;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point verticle for the Vert.x instance. */
public final class MainVerticle extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);
  private final Set<VerticleProvider> verticleProviders;
  private final Provider<Vertx> vertxProvider;

  @Inject
  MainVerticle(Set<VerticleProvider> verticleProviders, Provider<Vertx> vertxProvider) {
    this.verticleProviders = verticleProviders;
    this.vertxProvider = vertxProvider;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    logger.info("Starting MainVerticle, deploying registered verticles...");
    Vertx vertxInstance = vertxProvider.get();
    List<Future<String>> deployments =
        verticleProviders.stream()
            .map(provider -> vertxInstance.deployVerticle(provider, new DeploymentOptions()))
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
