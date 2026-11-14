package com.larpconnect.njall.api.studios.events;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
import com.larpconnect.njall.api.studios.common.UpdateMaskSupport;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.MalformedRequestContentRejection;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.RejectionHandler;
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
        .map(lookup -> buildTenantedRoutes(lookup, studioIdParam))
        .orElseGet(
            () -> completeError(StatusCodes.NOT_FOUND, "Studio not found: " + studioIdParam));
  }

  private boolean isActive(StudioLookup lookup) {
    return !lookup.isDeleted();
  }

  private Route buildTenantedRoutes(StudioLookup lookup, String studioIdParam) {
    return handleRejections(
        malformedRequestRejectionHandler(),
        () ->
            concat(
                pathEndOrSingleSlash(
                    () ->
                        concat(
                            get(() -> handleListEvents(lookup.tenantId(), studioIdParam)),
                            post(() -> handlePostEvent(lookup, studioIdParam)))),
                pathPrefix(
                    PathMatchers.segment(),
                    eventIdParam ->
                        handleEventBranch(lookup.tenantId(), studioIdParam, eventIdParam))));
  }

  private RejectionHandler malformedRequestRejectionHandler() {
    return RejectionHandler.newBuilder()
        .handle(
            MalformedRequestContentRejection.class,
            rejection -> completeError(StatusCodes.BAD_REQUEST, rejection.message()))
        .build();
  }

  private Route handlePostEvent(StudioLookup lookup, String studioIdParam) {
    return handleJsonBody(
        CreateEventRequest.class,
        false,
        request ->
            onComplete(
                () -> askCreateEvent(lookup, studioIdParam, request),
                res -> mapEventResponse(res, StatusCodes.CREATED)));
  }

  private Route handleListEvents(UUID tenantId, String studioIdParam) {
    return onComplete(
        () -> askListEvents(tenantId, studioIdParam), res -> mapEventResponse(res, StatusCodes.OK));
  }

  private Route handleEventBranch(UUID tenantId, String studioIdParam, String eventIdParam) {
    return EventValidation.tryParseUuid(eventIdParam)
        .map(eventId -> buildEventSubroutes(tenantId, studioIdParam, eventId))
        .orElseGet(() -> completeError(StatusCodes.NOT_FOUND, "Event not found: " + eventIdParam));
  }

  private Route buildEventSubroutes(UUID tenantId, String studioIdParam, UUID eventId) {
    return pathEndOrSingleSlash(
        () ->
            concat(
                get(() -> handleGetEvent(tenantId, studioIdParam, eventId)),
                patch(() -> handlePatchEvent(tenantId, studioIdParam, eventId)),
                delete(() -> handleDeleteEvent(tenantId, eventId))));
  }

  private Route handleGetEvent(UUID tenantId, String studioIdParam, UUID eventId) {
    return onComplete(
        () -> askGetEvent(tenantId, studioIdParam, eventId),
        res -> mapEventResponse(res, StatusCodes.OK));
  }

  private Route handlePatchEvent(UUID tenantId, String studioIdParam, UUID eventId) {
    return parameterOptional(
        "update_mask",
        mask ->
            handleJsonBody(
                UpdateEventRequest.class,
                UpdateMaskSupport.parseUpdateMask(mask).isPresent(),
                req ->
                    onComplete(
                        () -> askPatchEvent(tenantId, studioIdParam, eventId, req, mask),
                        res -> mapEventResponse(res, StatusCodes.OK))));
  }

  private Route handleDeleteEvent(UUID tenantId, UUID eventId) {
    return onComplete(
        () -> askDeleteEvent(tenantId, eventId),
        res -> mapEventResponse(res, StatusCodes.NO_CONTENT));
  }

  private CompletionStage<EventActorResponse> askCreateEvent(
      StudioLookup lookup, String studioIdentifier, CreateEventRequest request) {
    return ask(
        eventActor,
        replyTo ->
            new EventCommand.CreateEvent(
                lookup.tenantId(), studioIdentifier, request, replyTo, Optional.of(lookup)),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<EventActorResponse> askListEvents(
      UUID tenantId, String studioIdentifier) {
    return ask(
        eventActor,
        replyTo -> new EventCommand.ListEvents(tenantId, studioIdentifier, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<EventActorResponse> askGetEvent(
      UUID tenantId, String studioIdentifier, UUID eventId) {
    return ask(
        eventActor,
        replyTo -> new EventCommand.GetEvent(tenantId, studioIdentifier, eventId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<EventActorResponse> askPatchEvent(
      UUID tenantId,
      String studioIdentifier,
      UUID eventId,
      UpdateEventRequest request,
      Optional<String> updateMask) {
    return ask(
        eventActor,
        replyTo ->
            new EventCommand.PatchEvent(
                tenantId, studioIdentifier, eventId, request, updateMask, replyTo),
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

  private <T> Route handleJsonBody(
      Class<T> targetType, boolean allowNullRequiredFields, Function<T, Route> onParsed) {
    return entity(
        Jackson.unmarshaller(objectMapper, JsonNode.class),
        body -> decodeAndRun(body, targetType, allowNullRequiredFields, onParsed));
  }

  private <T> Route decodeAndRun(
      JsonNode body,
      Class<T> targetType,
      boolean allowNullRequiredFields,
      Function<T, Route> onParsed) {
    var jsonError = validateJsonBody(body, allowNullRequiredFields);
    if (jsonError.isPresent()) {
      return completeError(StatusCodes.BAD_REQUEST, jsonError.orElseThrow());
    }
    try {
      return onParsed.apply(objectMapper.treeToValue(body, targetType));
    } catch (JsonProcessingException e) {
      var detail = Optional.ofNullable(e.getOriginalMessage()).orElse("Malformed request payload");
      return completeError(StatusCodes.BAD_REQUEST, "Invalid request payload: " + detail);
    }
  }

  private static Optional<String> validateJsonBody(JsonNode body, boolean allowNullRequiredFields) {
    if (!body.isObject()) {
      return Optional.of("Request body must be a JSON object");
    }
    if (hasInvalidRequiredField(body, "eventType", allowNullRequiredFields)) {
      return Optional.of("Event eventType must be a valid EventType string");
    }
    if (hasInvalidRequiredField(body, "title", allowNullRequiredFields)) {
      return Optional.of("Event title must be a string");
    }
    return Optional.empty();
  }

  private static boolean hasInvalidRequiredField(
      JsonNode body, String fieldName, boolean allowNullRequiredFields) {
    if (!body.has(fieldName)) {
      return false;
    }
    var node = body.get(fieldName);
    return allowNullRequiredFields ? (!node.isNull() && !node.isTextual()) : !node.isTextual();
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
      case EventActorResponse.Failure f -> completeError(StatusCodes.get(f.status()), f.message());
    };
  }

  private Route completeError(StatusCode status, String message) {
    return complete(status, createErrorResponse(status, message), Jackson.marshaller(objectMapper));
  }

  private Route handleActorFailure(String entityType, Throwable error) {
    logger.error("{} actor request failed", entityType, error);
    return completeError(StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  private StudioErrorResponse createErrorResponse(StatusCode status, String message) {
    return new StudioErrorResponse(status.intValue(), message);
  }
}
