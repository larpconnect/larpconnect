package com.larpconnect.njall.api.admin;

import static java.util.Objects.requireNonNull;
import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

final class DefaultAdminRoute extends AllDirectives implements AdminRoute {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAdminRoute.class);
  private static final Duration ASK_TIMEOUT = Duration.ofSeconds(2);

  private final ActorRef<HealthCheckCommand> healthCheckActor;
  private final ActorSystem<Void> system;

  @Inject
  DefaultAdminRoute(ActorRef<HealthCheckCommand> healthCheckActor, ActorSystem<Void> system) {
    this.healthCheckActor = requireNonNull(healthCheckActor, "healthCheckActor must not be null");
    this.system = requireNonNull(system, "system must not be null");
  }

  @Override
  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/admin/v1/health"),
        () -> pathEndOrSingleSlash(() -> get(this::handleHealth)));
  }

  private Route handleHealth() {
    return onComplete(this::askHealthCheck, this::mapResponseToRoute);
  }

  private CompletionStage<HealthCheckResponse> askHealthCheck() {
    return ask(
        healthCheckActor, HealthCheckCommand.CheckHealth::new, ASK_TIMEOUT, system.scheduler());
  }

  private Route mapResponseToRoute(Try<HealthCheckResponse> responseTry) {
    if (responseTry.isFailure()) {
      LOGGER.error("Health check probe failed or timed out", responseTry.failed().get());
      return complete(StatusCodes.INTERNAL_SERVER_ERROR, "");
    }
    return switch (responseTry.get()) {
      case HealthCheckResponse.Healthy _ -> complete(StatusCodes.OK, "");
      case HealthCheckResponse.Unhealthy u -> {
        LOGGER.warn("Health check probe reported unhealthy: {}", u.reason());
        yield complete(StatusCodes.INTERNAL_SERVER_ERROR, "");
      }
    };
  }
}
