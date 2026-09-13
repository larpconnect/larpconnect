package com.larpconnect.njall.api.admin;

import static java.util.Objects.requireNonNull;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import java.util.Map;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;

/** Object-oriented Apache Pekko Typed actor evaluating Dropwizard health check registries. */
public final class HealthCheckActor extends AbstractBehavior<HealthCheckCommand> {

  private final HealthCheckRegistry registry;

  public HealthCheckActor(ActorContext<HealthCheckCommand> context, HealthCheckRegistry registry) {
    super(context);
    this.registry = requireNonNull(registry, "registry must not be null");
  }

  @Override
  public Receive<HealthCheckCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(HealthCheckCommand.CheckHealth.class, this::onCheckHealth)
        .build();
  }

  private Behavior<HealthCheckCommand> onCheckHealth(HealthCheckCommand.CheckHealth cmd) {
    var results = registry.runHealthChecks();
    var response = evaluateResults(results);
    cmd.replyTo().tell(response);
    return this;
  }

  private HealthCheckResponse evaluateResults(Map<String, HealthCheck.Result> results) {
    return results.entrySet().stream()
        .filter(entry -> !entry.getValue().isHealthy())
        .findFirst()
        .map(
            entry -> HealthCheckResponse.unhealthy(formatFailure(entry.getKey(), entry.getValue())))
        .orElseGet(HealthCheckResponse::healthy);
  }

  private static String formatFailure(String name, HealthCheck.Result result) {
    var message = result.getMessage();
    if (message != null && !message.isBlank()) {
      return name + ": " + message;
    }
    if (result.getError() != null) {
      return name + ": " + result.getError();
    }
    return name + ": unhealthy";
  }
}
