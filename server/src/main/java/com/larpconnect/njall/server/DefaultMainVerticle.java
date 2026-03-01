package com.larpconnect.njall.server;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.annotations.BuildWith;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import jakarta.inject.Inject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The primary container component that oversees the deployment of all other Vert.x verticles within
 * the application.
 *
 * <p>This class receives a pre-constructed set of {@link Verticle} instances from the Guice
 * dependency injection framework. During its startup phase, it asynchronously deploys each of these
 * child verticles to the Vert.x instance in parallel, tracking their individual deployment IDs. The
 * deployment is considered successful only when all child verticles have been fully initialized and
 * deployed.
 */
@BuildWith(ServerModule.class)
final class DefaultMainVerticle extends AbstractVerticle implements MainVerticle {
  private final Logger logger = LoggerFactory.getLogger(DefaultMainVerticle.class);
  private final ImmutableSet<Verticle> verticles;
  private final ConcurrentHashMap<Verticle, String> deploymentIds = new ConcurrentHashMap<>();

  @Inject
  DefaultMainVerticle(Set<Verticle> verticles) {
    this.verticles = ImmutableSet.copyOf(verticles);
  }

  @Override
  @AiContract(
      ensure = {
        """
        startPromise \\text{ is completed}
        """,
        """
        startPromise \\text{ succeeded } \\implies \\forall v \\in verticles, v \\text{ is deployed}
        """,
        """
        startPromise \\text{ succeeded } \\implies deploymentIds \\neq \\bot
        """
      },
      invariants = "verticles \\text{ is immutable}",
      implementationHint = "Deploys all injected verticles in parallel")
  public void start(Promise<Void> startPromise) {
    logger.info("DefaultMainVerticle starting…");
    var futures =
        verticles.stream()
            .map(
                verticle ->
                    vertx.deployVerticle(verticle).onSuccess(id -> deploymentIds.put(verticle, id)))
            .collect(toImmutableList());

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
    logger.info("DefaultMainVerticle stopping…");
    // Vert.x automatically undeploys child verticles when the parent is undeployed.
    deploymentIds.clear();
    stopPromise.complete();
  }
}
