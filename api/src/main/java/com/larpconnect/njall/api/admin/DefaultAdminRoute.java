package com.larpconnect.njall.api.admin;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;
import scala.util.Try;

final class DefaultAdminRoute extends AllDirectives implements AdminRoute {

  private static final Duration ASK_TIMEOUT = Duration.ofSeconds(2);

  private final ActorRef<HealthCheckCommand> healthCheckActor;
  private final ActorSystem<Void> system;

  @Inject
  DefaultAdminRoute(ActorRef<HealthCheckCommand> healthCheckActor, ActorSystem<Void> system) {
    this.healthCheckActor =
        Objects.requireNonNull(healthCheckActor, "healthCheckActor must not be null");
    this.system = Objects.requireNonNull(system, "system must not be null");
  }

  @Override
  public Route route() {
    return pathPrefix(
        "api",
        () ->
            pathPrefix(
                "admin",
                () ->
                    pathPrefix(
                        "v1",
                        () ->
                            pathPrefix(
                                "health",
                                () -> pathEndOrSingleSlash(() -> get(this::handleHealth))))));
  }

  private Route handleHealth() {
    return onComplete(this::askHealthCheck, this::mapResponseToRoute);
  }

  private CompletionStage<HealthCheckResponse> askHealthCheck() {
    return AskPattern.ask(
        healthCheckActor, HealthCheckCommand.CheckHealth::new, ASK_TIMEOUT, system.scheduler());
  }

  private Route mapResponseToRoute(Try<HealthCheckResponse> responseTry) {
    if (responseTry.isSuccess() && responseTry.get() instanceof HealthCheckResponse.Healthy) {
      return complete(StatusCodes.OK, "");
    }
    return complete(StatusCodes.INTERNAL_SERVER_ERROR, "");
  }
}
