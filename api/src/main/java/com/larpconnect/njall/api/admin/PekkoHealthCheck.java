package com.larpconnect.njall.api.admin;

import com.google.inject.Inject;
import io.dropwizard.metrics5.health.HealthCheck;
import io.dropwizard.metrics5.health.HealthCheck.Result;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorSystem;

/** Evaluates the operational health and shutdown status of the Apache Pekko ActorSystem. */
public final class PekkoHealthCheck implements HealthCheck {

  private final ActorSystem<Void> system;

  @Inject
  PekkoHealthCheck(ActorSystem<Void> system) {
    this.system = system;
  }

  @Override
  public Result check() {
    if (system.getWhenTerminated().toCompletableFuture().isDone()) {
      return Result.unhealthy("ActorSystem is terminated");
    }
    var shutdown = CoordinatedShutdown.get(system);
    if (shutdown.shutdownReason().isDefined()) {
      return Result.unhealthy(
          "ActorSystem coordinated shutdown initiated: " + shutdown.shutdownReason().get());
    }
    return Result.healthy();
  }
}
