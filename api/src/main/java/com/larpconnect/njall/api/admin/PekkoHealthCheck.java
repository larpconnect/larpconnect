package com.larpconnect.njall.api.admin;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import java.util.Objects;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorSystem;

/** Evaluates the operational health and shutdown status of the Apache Pekko ActorSystem. */
public final class PekkoHealthCheck extends HealthCheck {

  private final ActorSystem<Void> system;

  @Inject
  public PekkoHealthCheck(ActorSystem<Void> system) {
    this.system = Objects.requireNonNull(system, "system must not be null");
  }

  @Override
  protected Result check() {
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
