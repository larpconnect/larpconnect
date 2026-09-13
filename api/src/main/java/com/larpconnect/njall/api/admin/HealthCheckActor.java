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
    for (var entry : results.entrySet()) {
      if (!entry.getValue().isHealthy()) {
        var reason = formatFailure(entry.getKey(), entry.getValue());
        return HealthCheckResponse.unhealthy(reason);
      }
    }
    return HealthCheckResponse.healthy();
  }

  private static String formatFailure(String name, HealthCheck.Result result) {
    var message = result.getMessage();
    return name + ": " + (message != null ? message : "unhealthy");
  }
}
