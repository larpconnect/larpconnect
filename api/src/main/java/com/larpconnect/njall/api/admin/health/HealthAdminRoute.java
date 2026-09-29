package com.larpconnect.njall.api.admin.health;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.google.inject.Inject;
import com.larpconnect.njall.common.telemetry.ApiCall;
import com.larpconnect.njall.common.telemetry.TraceparentParser;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.model.HttpHeader;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

/** HTTP route handling administrative health check queries. */
public final class HealthAdminRoute extends AllDirectives {

  private final Logger logger = LoggerFactory.getLogger(HealthAdminRoute.class);
  private static final Duration HEALTH_ASK_TIMEOUT = Duration.ofSeconds(2);

  private final ActorRef<ApiCall<HealthCheckCommand>> healthCheckActor;
  private final ActorSystem<Void> system;

  @Inject
  HealthAdminRoute(
      ActorRef<ApiCall<HealthCheckCommand>> healthCheckActor, ActorSystem<Void> system) {
    this.healthCheckActor = healthCheckActor;
    this.system = system;
  }

  /**
   * Pure factory method constructing a {@link HealthAdminRoute} instance.
   *
   * @param healthCheckActor The health check actor reference.
   * @param system The ActorSystem.
   * @return A configured route instance.
   */
  public static HealthAdminRoute create(
      ActorRef<ApiCall<HealthCheckCommand>> healthCheckActor, ActorSystem<Void> system) {
    return new HealthAdminRoute(healthCheckActor, system);
  }

  /**
   * Generates the health check HTTP route.
   *
   * @return The configured route.
   */
  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/admin/v1/health"),
        () -> pathEndOrSingleSlash(() -> get(this::handleHealth)));
  }

  private Route handleHealth() {
    return extractRequest(
        request -> onComplete(() -> askHealthCheck(request), this::mapResponseToRoute));
  }

  private CompletionStage<HealthCheckResponse> askHealthCheck(HttpRequest request) {
    var traceContext =
        request.getHeader("traceparent").map(HttpHeader::value).flatMap(TraceparentParser::parse);
    return ask(
        healthCheckActor,
        replyTo -> new ApiCall<>(new HealthCheckCommand.CheckHealth(replyTo), traceContext),
        HEALTH_ASK_TIMEOUT,
        system.scheduler());
  }

  private Route mapResponseToRoute(Try<HealthCheckResponse> responseTry) {
    if (responseTry.isFailure()) {
      logger.error("Health check probe failed or timed out", responseTry.failed().get());
      return complete(StatusCodes.INTERNAL_SERVER_ERROR, "");
    }
    return switch (responseTry.get()) {
      case HealthCheckResponse.Healthy _ -> complete(StatusCodes.OK, "");
      case HealthCheckResponse.Unhealthy u -> {
        logger.warn("Health check probe reported unhealthy: {}", u.reason());
        yield complete(StatusCodes.INTERNAL_SERVER_ERROR, "");
      }
    };
  }
}
