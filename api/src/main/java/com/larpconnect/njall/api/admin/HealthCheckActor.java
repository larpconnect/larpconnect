package com.larpconnect.njall.api.admin;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import java.util.Map;
import java.util.Objects;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/** Stateless Apache Pekko Typed actor evaluating Dropwizard health check registries. */
public final class HealthCheckActor {

  private HealthCheckActor() {}

  /**
   * Creates a stateless behavior handling {@link HealthCheckCommand}.
   *
   * @param registry The Dropwizard health check registry to evaluate.
   * @return The configured actor behavior.
   */
  public static Behavior<HealthCheckCommand> create(HealthCheckRegistry registry) {
    Objects.requireNonNull(registry, "registry must not be null");
    return Behaviors.receiveMessage(
        message ->
            switch (message) {
              case HealthCheckCommand.CheckHealth cmd -> onCheckHealth(registry, cmd);
            });
  }

  private static Behavior<HealthCheckCommand> onCheckHealth(
      HealthCheckRegistry registry, HealthCheckCommand.CheckHealth cmd) {
    var results = registry.runHealthChecks();
    var response = evaluateResults(results);
    cmd.replyTo().tell(response);
    return Behaviors.same();
  }

  private static HealthCheckResponse evaluateResults(Map<String, HealthCheck.Result> results) {
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
