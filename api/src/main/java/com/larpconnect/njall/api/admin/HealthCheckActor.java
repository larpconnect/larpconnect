package com.larpconnect.njall.api.admin;

import io.dropwizard.metrics5.health.HealthCheck;
import io.dropwizard.metrics5.health.HealthCheckRegistry;
import java.util.Map;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

/** Object-oriented Apache Pekko Typed actor evaluating Dropwizard health check registries. */
public final class HealthCheckActor extends AbstractBehavior<HealthCheckCommand> {

  private final HealthCheckRegistry registry;

  /**
   * Creates a {@link Behavior} for {@link HealthCheckActor} decorated with {@code
   * Behaviors.withMdc}.
   *
   * @param registry health check registry
   * @return decorated behavior
   */
  public static Behavior<HealthCheckCommand> create(HealthCheckRegistry registry) {
    return Behaviors.withMdc(
        HealthCheckCommand.class,
        Map.of(),
        HealthCheckActor::extractMdc,
        Behaviors.setup(context -> new HealthCheckActor(context, registry)));
  }

  static Map<String, String> extractMdc(HealthCheckCommand cmd) {
    return switch (cmd) {
      case HealthCheckCommand.CheckHealth checkHealth ->
          checkHealth
              .traceContext()
              .map(tc -> Map.of("trace_id", tc.traceId(), "span_id", tc.spanId()))
              .orElseGet(Map::of);
    };
  }

  public HealthCheckActor(ActorContext<HealthCheckCommand> context, HealthCheckRegistry registry) {
    super(context);
    this.registry = registry;
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
