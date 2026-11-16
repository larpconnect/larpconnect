package com.larpconnect.njall.api.studios;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CharMatcher;
import com.google.inject.Inject;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.time.Duration;
import java.util.Optional;
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

/** Default Pekko HTTP implementation of {@link StudiosRoute}. */
final class DefaultStudiosRoute extends AllDirectives implements StudiosRoute {

  private static final Duration DEFAULT_ASK_TIMEOUT = Duration.ofSeconds(20);
  private static final int MAX_STUDIO_ID_LENGTH = 128;
  private static final CharMatcher ISO_CONTROL = CharMatcher.javaIsoControl();
  private static final String STUDIO_NOT_FOUND_MESSAGE = "Studio not found";
  private static final String INTERNAL_ERROR_MESSAGE = "Internal server error";

  private final Logger logger = LoggerFactory.getLogger(DefaultStudiosRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<StudioCommand> studioActor;
  private final StudioSubRoutes subRoutes;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  DefaultStudiosRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<StudioCommand> studioActor,
      StudioSubRoutes subRoutes,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioLookupCache, studioActor, subRoutes, system, objectMapper, DEFAULT_ASK_TIMEOUT);
  }

  DefaultStudiosRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<StudioCommand> studioActor,
      StudioSubRoutes subRoutes,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.studioActor = studioActor;
    this.subRoutes = subRoutes;
    this.system = system;
    this.objectMapper = objectMapper;
    this.askTimeout = askTimeout;
  }

  @Override
  public Route route() {
    // Concatenate non-overlapping child routes; specific path prefixes avoid route shadowing
    return concat(studioRoute(), subRoutes.route());
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
    if (!isValidStudioId(studioIdParam)) {
      return completeNotFound();
    }
    return routeActiveStudio(studioIdParam);
  }

  private boolean isValidStudioId(String studioIdParam) {
    return !studioIdParam.isBlank()
        && studioIdParam.length() <= MAX_STUDIO_ID_LENGTH
        && !ISO_CONTROL.matchesAnyOf(studioIdParam);
  }

  private Route routeActiveStudio(String studioIdParam) {
    return findActiveStudio(studioIdParam)
        .map(this::dispatchGetStudio)
        .orElseGet(this::completeNotFound);
  }

  private Optional<StudioLookup> findActiveStudio(String studioIdParam) {
    return studioLookupCache.findByIdOrAlias(studioIdParam).filter(lookup -> !lookup.isDeleted());
  }

  private Route dispatchGetStudio(StudioLookup lookup) {
    return onComplete(() -> askGetStudio(lookup), responseTry -> mapResponse(lookup, responseTry));
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

  private Route mapResponse(StudioLookup lookup, Try<StudioActorResponse> responseTry) {
    return failureOf(responseTry)
        .map(error -> handleActorFailure(lookup, error))
        .orElseGet(() -> mapActorResponse(responseOf(responseTry)));
  }

  private Optional<Throwable> failureOf(Try<StudioActorResponse> responseTry) {
    if (responseTry.isFailure()) {
      return Optional.of(responseTry.failed().get());
    }
    return Optional.empty();
  }

  private StudioActorResponse responseOf(Try<StudioActorResponse> responseTry) {
    return responseTry.get();
  }

  private Route mapActorResponse(StudioActorResponse response) {
    return switch (response) {
      case StudioActorResponse.Success success -> completeJson(StatusCodes.OK, success.studio());
      case StudioActorResponse.NotFound _ -> completeNotFound();
      case StudioActorResponse.Failure _ -> completeInternalError();
    };
  }

  private Route handleActorFailure(StudioLookup lookup, Throwable error) {
    logActorFailure(lookup, error);
    return completeInternalError();
  }

  private void logActorFailure(StudioLookup lookup, Throwable error) {
    logger.error("Studio actor request failed for studio {}", lookup.studioId(), error);
  }

  private Route completeNotFound() {
    return completeError(StatusCodes.NOT_FOUND, STUDIO_NOT_FOUND_MESSAGE);
  }

  private Route completeInternalError() {
    return completeError(StatusCodes.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE);
  }

  private Route completeError(StatusCode status, String message) {
    return completeJson(status, createErrorResponse(status.intValue(), message));
  }

  private <T> Route completeJson(StatusCode status, T body) {
    return complete(status, body, Jackson.marshaller(objectMapper));
  }

  private StudioErrorResponse createErrorResponse(int code, String message) {
    return new StudioErrorResponse(code, message);
  }
}
