package com.larpconnect.njall.api.studios.events;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.ImmutableTypeParameter;
import com.larpconnect.njall.api.studios.common.StudioResourceUriSupport;
import com.larpconnect.njall.api.studios.common.UpdateMaskSupport;
import com.larpconnect.njall.data.dao.studios.events.EventDAO;
import com.larpconnect.njall.data.dao.studios.events.PatchEventParams;
import com.larpconnect.njall.data.domain.studios.Event;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted event operations. */
final class EventActor {

  private final Logger logger = LoggerFactory.getLogger(EventActor.class);
  private final EventDAO eventDao;

  private EventActor(EventDAO eventDao) {
    this.eventDao = eventDao;
  }

  static Behavior<EventCommand> create(EventDAO eventDao) {
    return createActor(eventDao).behavior();
  }

  private static EventActor createActor(EventDAO eventDao) {
    return new EventActor(eventDao);
  }

  private Behavior<EventCommand> behavior() {
    return Behaviors.receiveMessage(this::onCommand);
  }

  private Behavior<EventCommand> onCommand(EventCommand command) {
    return switch (command) {
      case EventCommand.CreateEvent cmd -> onCreateEvent(cmd);
      case EventCommand.GetEvent cmd -> onGetEvent(cmd);
      case EventCommand.ListEvents cmd -> onListEvents(cmd);
      case EventCommand.PatchEvent cmd -> onPatchEvent(cmd);
      case EventCommand.DeleteEvent cmd -> onDeleteEvent(cmd);
    };
  }

  private Behavior<EventCommand> onCreateEvent(EventCommand.CreateEvent cmd) {
    try {
      var validationError = EventValidation.validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(EventActorResponse.badRequest(validationError.orElseThrow()));
        return Behaviors.same();
      }
      var owner =
          StudioResourceUriSupport.parseResourceUri(
              cmd.request().generator(),
              cmd.studioIdentifier(),
              StudioResourceUriSupport.ACTORS_COLLECTION,
              cmd.lookup());
      executeCreate(cmd.tenantId(), cmd.studioIdentifier(), owner, cmd.request(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create event", e);
    }
    return Behaviors.same();
  }

  private void executeCreate(
      UUID tenantId,
      String studioIdentifier,
      UUID owner,
      CreateEventRequest request,
      ActorRef<EventActorResponse> replyTo) {
    var domain =
        eventDao.create(
            tenantId,
            owner,
            request.eventType().orElse(EventType.EVENT_TYPE_UNSPECIFIED),
            request.title(),
            request.summary(),
            request.locationId(),
            request.startTime(),
            request.endTime());
    replyTo.tell(EventActorResponse.success(toEventResponse(studioIdentifier, domain)));
  }

  private Behavior<EventCommand> onGetEvent(EventCommand.GetEvent cmd) {
    try {
      executeGet(cmd.tenantId(), cmd.studioIdentifier(), cmd.eventId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get event", e);
    }
    return Behaviors.same();
  }

  private void executeGet(
      UUID tenantId, String studioIdentifier, UUID eventId, ActorRef<EventActorResponse> replyTo) {
    eventDao
        .findById(tenantId, eventId)
        .ifPresentOrElse(
            event ->
                replyTo.tell(EventActorResponse.success(toEventResponse(studioIdentifier, event))),
            () -> replyTo.tell(EventActorResponse.notFound("Event not found: " + eventId)));
  }

  private Behavior<EventCommand> onListEvents(EventCommand.ListEvents cmd) {
    try {
      executeList(cmd.tenantId(), cmd.studioIdentifier(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list events", e);
    }
    return Behaviors.same();
  }

  private void executeList(
      UUID tenantId, String studioIdentifier, ActorRef<EventActorResponse> replyTo) {
    var events =
        eventDao.listAll(tenantId).stream()
            .map(e -> toEventResponse(studioIdentifier, e))
            .collect(toImmutableList());
    replyTo.tell(EventActorResponse.items(events));
  }

  private Behavior<EventCommand> onPatchEvent(EventCommand.PatchEvent cmd) {
    try {
      var maskFields = UpdateMaskSupport.parseUpdateMask(cmd.updateMask());
      var validationError = validatePatch(maskFields, cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(EventActorResponse.badRequest(validationError.orElseThrow()));
        return Behaviors.same();
      }
      executePatch(
          cmd.tenantId(),
          cmd.studioIdentifier(),
          cmd.eventId(),
          cmd.request(),
          maskFields,
          cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch event", e);
    }
    return Behaviors.same();
  }

  private void executePatch(
      UUID tenantId,
      String studioIdentifier,
      UUID eventId,
      UpdateEventRequest request,
      Optional<ImmutableSet<String>> maskFields,
      ActorRef<EventActorResponse> replyTo) {
    eventDao
        .patch(tenantId, eventId, toPatchParams(maskFields, request))
        .ifPresentOrElse(
            updated ->
                replyTo.tell(
                    EventActorResponse.success(toEventResponse(studioIdentifier, updated))),
            () -> replyTo.tell(EventActorResponse.notFound("Event not found: " + eventId)));
  }

  private Behavior<EventCommand> onDeleteEvent(EventCommand.DeleteEvent cmd) {
    try {
      executeDelete(cmd.tenantId(), cmd.eventId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete event", e);
    }
    return Behaviors.same();
  }

  private void executeDelete(UUID tenantId, UUID eventId, ActorRef<EventActorResponse> replyTo) {
    var deleted = eventDao.softDelete(tenantId, eventId);
    if (deleted) {
      replyTo.tell(EventActorResponse.deleted());
    } else {
      replyTo.tell(EventActorResponse.notFound("Event not found: " + eventId));
    }
  }

  private static EventResponse toEventResponse(String studioIdentifier, Event event) {
    var generatorUri =
        StudioResourceUriSupport.formatResourceUri(
            studioIdentifier, StudioResourceUriSupport.ACTORS_COLLECTION, event.generator());
    return createEventResponse(event, generatorUri);
  }

  private static EventResponse createEventResponse(Event event, String generatorUri) {
    return new EventResponse(
        event.id(),
        generatorUri,
        event.title(),
        event.summary(),
        event.eventType(),
        event.locationId(),
        event.startTime(),
        event.endTime(),
        event.createdOn(),
        event.updatedOn());
  }

  private static Optional<String> validatePatch(
      Optional<ImmutableSet<String>> maskFields, UpdateEventRequest request) {
    return validateRequiredMaskFields(maskFields, request)
        .or(() -> EventValidation.validatePatch(request, maskFields));
  }

  private static Optional<String> validateRequiredMaskFields(
      Optional<ImmutableSet<String>> maskFields, UpdateEventRequest request) {
    if (UpdateMaskSupport.isFieldCleared(maskFields, request.title(), "title")) {
      return Optional.of("Event title must not be blank");
    }
    if (UpdateMaskSupport.isFieldCleared(
        maskFields, request.eventType(), "eventType", "event_type")) {
      return Optional.of("Event eventType must not be cleared");
    }
    return Optional.empty();
  }

  @Immutable
  private record MaskedField<@ImmutableTypeParameter T>(Optional<T> value, boolean cleared) {}

  private static PatchEventParams toPatchParams(
      Optional<ImmutableSet<String>> maskFields, UpdateEventRequest request) {
    var eventType =
        UpdateMaskSupport.selectOptionalField(
            request.eventType(), Optional.empty(), maskFields, "eventType", "event_type");
    var title =
        UpdateMaskSupport.selectOptionalField(
            request.title(), Optional.empty(), maskFields, "title");
    var summary = resolveMaskedField(maskFields, request.summary(), "summary");
    var locationId =
        resolveMaskedField(maskFields, request.locationId(), "locationId", "location_id");
    var startTime = resolveMaskedField(maskFields, request.startTime(), "startTime", "start_time");
    var endTime = resolveMaskedField(maskFields, request.endTime(), "endTime", "end_time");
    return createPatchParams(eventType, title, summary, locationId, startTime, endTime);
  }

  private static <T> MaskedField<T> resolveMaskedField(
      Optional<ImmutableSet<String>> maskFields, Optional<T> requestValue, String... fieldNames) {
    var value =
        UpdateMaskSupport.selectOptionalField(
            requestValue, Optional.empty(), maskFields, fieldNames);
    var cleared = UpdateMaskSupport.isFieldCleared(maskFields, requestValue, fieldNames);
    return createMaskedField(value, cleared);
  }

  private static <T> MaskedField<T> createMaskedField(Optional<T> value, boolean cleared) {
    return new MaskedField<>(value, cleared);
  }

  private static PatchEventParams createPatchParams(
      Optional<EventType> eventType,
      Optional<String> title,
      MaskedField<String> summary,
      MaskedField<UUID> locationId,
      MaskedField<Instant> startTime,
      MaskedField<Instant> endTime) {
    return new PatchEventParams(
        eventType,
        title,
        summary.value(),
        summary.cleared(),
        locationId.value(),
        locationId.cleared(),
        startTime.value(),
        startTime.cleared(),
        endTime.value(),
        endTime.cleared());
  }

  private void handleError(
      ActorRef<EventActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in EventActor", operation, error);
    if (error instanceof IllegalArgumentException) {
      var message = error.getMessage();
      var reason = !Strings.isNullOrEmpty(message) ? message : "Invalid request";
      replyTo.tell(EventActorResponse.badRequest(reason));
      return;
    }
    if (isConstraintViolation(error)) {
      replyTo.tell(EventActorResponse.badRequest("Request data violates database constraints"));
      return;
    }
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(EventActorResponse.failure(reason));
  }

  private static boolean isConstraintViolation(@Nullable Throwable throwable) {
    var current = throwable;
    while (current != null) {
      if (current instanceof ConstraintViolationException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
