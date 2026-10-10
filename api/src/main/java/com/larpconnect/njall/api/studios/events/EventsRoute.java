package com.larpconnect.njall.api.studios.events;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
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

/** HTTP route handling tenanted physical and scheduled studio events. */
public final class EventsRoute extends AllDirectives implements RouteProvider {

  private final Logger logger = LoggerFactory.getLogger(EventsRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<EventCommand> eventActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  EventsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<EventCommand> eventActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioLookupCache, eventActor, system, objectMapper, Duration.ofSeconds(20));
  }

  EventsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<EventCommand> eventActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.eventActor = eventActor;
    this.system = system;
    this.objectMapper = objectMapper;
    this.askTimeout = askTimeout;
  }

  @Override
  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/studios"),
        () ->
            pathPrefix(
                PathMatchers.segment(),
                studioIdParam ->
                    pathPrefix(
                        PathMatchers.separateOnSlashes("v1/events"),
                        () -> handleEvents(studioIdParam))));
  }

  private Route handleEvents(String studioIdParam) {
    return studioLookupCache
        .findByIdOrAlias(studioIdParam)
        .filter(this::isActive)
        .map(this::buildTenantedRoutes)
        .orElseGet(() -> completeNotFound("Studio not found: " + studioIdParam));
  }

  private boolean isActive(StudioLookup lookup) {
    return !lookup.isDeleted();
  }

  private Route buildTenantedRoutes(StudioLookup lookup) {
    return concat(
        pathEndOrSingleSlash(
            () ->
                concat(
                    get(() -> handleListEvents(lookup.tenantId())),
                    post(() -> handlePostEvent(lookup.tenantId())))),
        pathPrefix(
            PathMatchers.segment(),
            eventIdParam -> handleEventBranch(lookup.tenantId(), eventIdParam)));
  }

  private Route handlePostEvent(UUID tenantId) {
    return entity(
        Jackson.unmarshaller(objectMapper, CreateEventRequest.class),
        request ->
            onComplete(
                () -> askCreateEvent(tenantId, request),
                res -> mapEventResponse(res, StatusCodes.CREATED)));
  }

  private Route handleListEvents(UUID tenantId) {
    return onComplete(() -> askListEvents(tenantId), res -> mapEventResponse(res, StatusCodes.OK));
  }

  private Route handleEventBranch(UUID tenantId, String eventIdParam) {
    return EventValidation.tryParseUuid(eventIdParam)
        .map(eventId -> buildEventSubroutes(tenantId, eventId))
        .orElseGet(() -> completeNotFound("Event not found: " + eventIdParam));
  }

  private Route buildEventSubroutes(UUID tenantId, UUID eventId) {
    return pathEndOrSingleSlash(
        () ->
            concat(
                get(() -> handleGetEvent(tenantId, eventId)),
                patch(() -> handlePatchEvent(tenantId, eventId)),
                delete(() -> handleDeleteEvent(tenantId, eventId))));
  }

  private Route handleGetEvent(UUID tenantId, UUID eventId) {
    return onComplete(
        () -> askGetEvent(tenantId, eventId), res -> mapEventResponse(res, StatusCodes.OK));
  }

  private Route handlePatchEvent(UUID tenantId, UUID eventId) {
    return parameterOptional(
        "update_mask",
        mask ->
            entity(
                Jackson.unmarshaller(objectMapper, UpdateEventRequest.class),
                req ->
                    onComplete(
                        () -> askPatchEvent(tenantId, eventId, req, mask),
                        res -> mapEventResponse(res, StatusCodes.OK))));
  }

  private Route handleDeleteEvent(UUID tenantId, UUID eventId) {
    return onComplete(
        () -> askDeleteEvent(tenantId, eventId),
        res -> mapEventResponse(res, StatusCodes.NO_CONTENT));
  }

  private CompletionStage<EventActorResponse> askCreateEvent(
      UUID tenantId, CreateEventRequest request) {
    return ask(
        eventActor,
        replyTo -> new EventCommand.CreateEvent(tenantId, request, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<EventActorResponse> askListEvents(UUID tenantId) {
    return ask(
        eventActor,
        replyTo -> new EventCommand.ListEvents(tenantId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<EventActorResponse> askGetEvent(UUID tenantId, UUID eventId) {
    return ask(
        eventActor,
        replyTo -> new EventCommand.GetEvent(tenantId, eventId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<EventActorResponse> askPatchEvent(
      UUID tenantId, UUID eventId, UpdateEventRequest request, Optional<String> updateMask) {
    return ask(
        eventActor,
        replyTo -> new EventCommand.PatchEvent(tenantId, eventId, request, updateMask, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<EventActorResponse> askDeleteEvent(UUID tenantId, UUID eventId) {
    return ask(
        eventActor,
        replyTo -> new EventCommand.DeleteEvent(tenantId, eventId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapEventResponse(Try<EventActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      return handleActorFailure("Event", responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case EventActorResponse.Success s ->
          complete(successStatus, s.event(), Jackson.marshaller(objectMapper));
      case EventActorResponse.Items i ->
          complete(StatusCodes.OK, i.events(), Jackson.marshaller(objectMapper));
      case EventActorResponse.Deleted _ -> complete(StatusCodes.NO_CONTENT);
      case EventActorResponse.Failure f -> {
        var status = StatusCodes.get(f.status());
        yield complete(
            status, createErrorResponse(status, f.message()), Jackson.marshaller(objectMapper));
      }
    };
  }

  private Route completeNotFound(String message) {
    return complete(
        StatusCodes.NOT_FOUND,
        createErrorResponse(StatusCodes.NOT_FOUND, message),
        Jackson.marshaller(objectMapper));
  }

  private Route handleActorFailure(String entityType, Throwable error) {
    logger.error("{} actor request failed", entityType, error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        createErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private StudioErrorResponse createErrorResponse(StatusCode status, String message) {
    return new StudioErrorResponse(status.intValue(), message);
  }
}
