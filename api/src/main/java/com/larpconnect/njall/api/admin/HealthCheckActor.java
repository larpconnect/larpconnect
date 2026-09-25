package com.larpconnect.njall.api.admin;

import com.larpconnect.njall.common.telemetry.ApiCall;
import io.dropwizard.metrics5.health.HealthCheck;
import io.dropwizard.metrics5.health.HealthCheckRegistry;
import java.util.Map;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;

/** Object-oriented Apache Pekko Typed actor evaluating Dropwizard health check registries. */
public final class HealthCheckActor extends AbstractBehavior<ApiCall<HealthCheckCommand>> {

  private final HealthCheckRegistry registry;

  /**
   * Creates a {@link Behavior} for {@link HealthCheckActor} decorated with {@code
   * Behaviors.withMdc}.
   *
   * @param registry health check registry
   * @return decorated behavior
   */
  // Safe unchecked cast: Java type erasure prevents ApiCall<HealthCheckCommand>.class literal;
  // Pekko ActorRef typing guarantees message payload type.
  @SuppressWarnings("unchecked")
  public static Behavior<ApiCall<HealthCheckCommand>> create(HealthCheckRegistry registry) {
    Class<ApiCall<HealthCheckCommand>> messageClass =
        (Class<ApiCall<HealthCheckCommand>>) (Class<?>) ApiCall.class;
    return Behaviors.withMdc(
        messageClass,
        Map.of(),
        HealthCheckActor::extractMdc,
        Behaviors.setup(context -> new HealthCheckActor(context, registry)));
  }

  static Map<String, String> extractMdc(ApiCall<? extends HealthCheckCommand> apiCall) {
    return apiCall
        .context()
        .map(tc -> Map.of("trace_id", tc.traceId(), "span_id", tc.spanId()))
        .orElseGet(Map::of);
  }

  public HealthCheckActor(
      ActorContext<ApiCall<HealthCheckCommand>> context, HealthCheckRegistry registry) {
    super(context);
    this.registry = registry;
  }

  @Override
  // Safe unchecked cast: Java type erasure prevents ApiCall<HealthCheckCommand>.class literal;
  // Pekko ActorRef typing guarantees message payload type.
  @SuppressWarnings("unchecked")
  public Receive<ApiCall<HealthCheckCommand>> createReceive() {
    return newReceiveBuilder()
        .onMessage((Class<ApiCall<HealthCheckCommand>>) (Class<?>) ApiCall.class, this::onApiCall)
        .build();
  }

  private Behavior<ApiCall<HealthCheckCommand>> onApiCall(ApiCall<HealthCheckCommand> apiCall) {
    return switch (apiCall.call()) {
      case HealthCheckCommand.CheckHealth checkHealth -> handleCheckHealth(checkHealth);
    };
  }

  private Behavior<ApiCall<HealthCheckCommand>> handleCheckHealth(
      HealthCheckCommand.CheckHealth checkHealth) {
    var results = runHealthChecks();
    var response = evaluateResults(results);
    sendResponse(checkHealth, response);
    return this;
  }

  private Map<String, HealthCheck.Result> runHealthChecks() {
    return registry.runHealthChecks();
  }

  private void sendResponse(
      HealthCheckCommand.CheckHealth checkHealth, HealthCheckResponse response) {
    checkHealth.replyTo().tell(response);
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
