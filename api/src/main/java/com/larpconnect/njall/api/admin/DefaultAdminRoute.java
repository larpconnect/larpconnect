package com.larpconnect.njall.api.admin;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.common.telemetry.TraceContext;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson;
import org.apache.pekko.http.javadsl.model.HttpHeader;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

final class DefaultAdminRoute extends AllDirectives implements AdminRoute {

  private final Logger logger = LoggerFactory.getLogger(DefaultAdminRoute.class);
  private static final Duration HEALTH_ASK_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration SERVER_ASK_TIMEOUT = Duration.ofSeconds(10);

  private final ActorRef<HealthCheckCommand> healthCheckActor;
  private final ActorRef<ServerAdminCommand> serverAdminActor;
  private final StudioAdminRoute studioAdminRoute;
  private final UserAdminRoute userAdminRoute;
  private final RoleAdminRoute roleAdminRoute;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;

  @Inject
  DefaultAdminRoute(
      ActorRef<HealthCheckCommand> healthCheckActor,
      ActorRef<ServerAdminCommand> serverAdminActor,
      StudioAdminRoute studioAdminRoute,
      UserAdminRoute userAdminRoute,
      RoleAdminRoute roleAdminRoute,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this.healthCheckActor = healthCheckActor;
    this.serverAdminActor = serverAdminActor;
    this.studioAdminRoute = studioAdminRoute;
    this.userAdminRoute = userAdminRoute;
    this.roleAdminRoute = roleAdminRoute;
    this.system = system;
    this.objectMapper = objectMapper;
  }

  @Override
  public Route route() {
    return concat(
        pathPrefix(
            PathMatchers.separateOnSlashes("api/admin/v1/health"),
            () -> pathEndOrSingleSlash(() -> get(this::handleHealth))),
        pathPrefix(
            PathMatchers.separateOnSlashes("api/admin/v1/servers"),
            () -> pathEndOrSingleSlash(() -> get(this::handleServers))),
        studioAdminRoute.route(),
        userAdminRoute.route(),
        roleAdminRoute.route());
  }

  private Route handleHealth() {
    return extractRequest(
        request -> onComplete(() -> askHealthCheck(request), this::mapResponseToRoute));
  }

  private CompletionStage<HealthCheckResponse> askHealthCheck(HttpRequest request) {
    var traceContext =
        request
            .getHeader("traceparent")
            .map(HttpHeader::value)
            .flatMap(TraceContext::parseTraceparent);
    return ask(
        healthCheckActor,
        replyTo -> new HealthCheckCommand.CheckHealth(replyTo, traceContext),
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

  private Route handleServers() {
    return onComplete(this::askListServers, this::mapServerResponseToRoute);
  }

  private CompletionStage<ServerAdminResponse> askListServers() {
    return ask(
        serverAdminActor,
        ServerAdminCommand.ListServers::new,
        SERVER_ASK_TIMEOUT,
        system.scheduler());
  }

  private Route mapServerResponseToRoute(Try<ServerAdminResponse> responseTry) {
    if (responseTry.isFailure()) {
      logger.error("Failed to query servers", responseTry.failed().get());
      return complete(StatusCodes.INTERNAL_SERVER_ERROR, "");
    }
    return switch (responseTry.get()) {
      case ServerAdminResponse.ServerList list ->
          completeOK(list.servers(), Jackson.marshaller(objectMapper));
      case ServerAdminResponse.Failure f -> {
        logger.error("Server query returned failure: {}", f.reason());
        yield complete(StatusCodes.INTERNAL_SERVER_ERROR, "");
      }
    };
  }
}
