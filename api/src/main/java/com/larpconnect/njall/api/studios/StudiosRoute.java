package com.larpconnect.njall.api.studios;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
import com.larpconnect.njall.api.studios.links.LinksRoute;
import com.larpconnect.njall.api.studios.locations.LocationsRoute;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

/** HTTP route handling user-space tenanted studio retrieval and aggregating subordinate routes. */
public final class StudiosRoute extends AllDirectives implements RouteProvider {

  private final Logger logger = LoggerFactory.getLogger(StudiosRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<StudioCommand> studioActor;
  private final LinksRoute linksRoute;
  private final LocationsRoute locationsRoute;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  StudiosRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<StudioCommand> studioActor,
      LinksRoute linksRoute,
      LocationsRoute locationsRoute,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(
        studioLookupCache,
        studioActor,
        linksRoute,
        locationsRoute,
        system,
        objectMapper,
        Duration.ofSeconds(20));
  }

  StudiosRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<StudioCommand> studioActor,
      LinksRoute linksRoute,
      LocationsRoute locationsRoute,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.studioActor = studioActor;
    this.linksRoute = linksRoute;
    this.locationsRoute = locationsRoute;
    this.system = system;
    this.objectMapper = objectMapper;
    this.askTimeout = askTimeout;
  }

  @Override
  public Route route() {
    // Concatenate non-overlapping child routes; specific path prefixes avoid route shadowing
    return concat(studioRoute(), locationsRoute.route(), linksRoute.route());
  }

  private Route studioRoute() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/studios"),
        () ->
            pathPrefix(
                PathMatchers.segment(),
                studioIdParam ->
                    path(
                        PathMatchers.separateOnSlashes("v1/studio"),
                        () -> get(() -> handleGetStudio(studioIdParam)))));
  }

  private Route handleGetStudio(String studioIdParam) {
    return studioLookupCache
        .findByIdOrAlias(studioIdParam)
        .filter(this::isActive)
        .map(this::dispatchGetStudio)
        .orElseGet(() -> completeNotFound(studioIdParam));
  }

  private boolean isActive(StudioLookup lookup) {
    return !lookup.isDeleted();
  }

  private Route dispatchGetStudio(StudioLookup lookup) {
    return onComplete(() -> askGetStudio(lookup), this::mapResponse);
  }

  private CompletionStage<StudioActorResponse> askGetStudio(StudioLookup lookup) {
    return ask(
        studioActor,
        replyTo -> createGetStudioCommand(lookup, replyTo),
        askTimeout,
        system.scheduler());
  }

  private StudioCommand.GetStudio createGetStudioCommand(
      StudioLookup lookup, ActorRef<StudioActorResponse> replyTo) {
    return new StudioCommand.GetStudio(lookup, replyTo);
  }

  private Route completeNotFound(String studioIdParam) {
    return complete(
        StatusCodes.NOT_FOUND,
        createNotFoundErrorResponse(studioIdParam),
        Jackson.marshaller(objectMapper));
  }

  private StudioErrorResponse createNotFoundErrorResponse(String studioIdParam) {
    return new StudioErrorResponse(404, "Studio not found: " + studioIdParam);
  }

  private Route mapResponse(Try<StudioActorResponse> responseTry) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case StudioActorResponse.Success s ->
          completeOK(s.studio(), Jackson.marshaller(objectMapper));
      case StudioActorResponse.NotFound nf ->
          complete(
              StatusCodes.NOT_FOUND,
              createErrorResponse(StatusCodes.NOT_FOUND, nf.message()),
              Jackson.marshaller(objectMapper));
      case StudioActorResponse.Failure f ->
          complete(
              StatusCodes.INTERNAL_SERVER_ERROR,
              createErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, f.message()),
              Jackson.marshaller(objectMapper));
    };
  }

  private Route handleActorFailure(Throwable error) {
    logger.error("Studio actor request failed", error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        createErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private StudioErrorResponse createErrorResponse(StatusCode status, String message) {
    return new StudioErrorResponse(status.intValue(), message);
  }
}
