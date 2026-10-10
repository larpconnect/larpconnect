package com.larpconnect.njall.api.studios.events;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.data.dao.studios.EventDAO;
import com.larpconnect.njall.data.domain.Event;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apache Pekko Typed actor executing user-space tenanted event operations. */
final class EventActor extends AbstractBehavior<EventCommand> {

  private final Logger logger = LoggerFactory.getLogger(EventActor.class);
  private final EventDAO eventDao;

  EventActor(ActorContext<EventCommand> context, EventDAO eventDao) {
    super(context);
    this.eventDao = eventDao;
  }

  @Override
  public Receive<EventCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(EventCommand.CreateEvent.class, this::onCreateEvent)
        .onMessage(EventCommand.GetEvent.class, this::onGetEvent)
        .onMessage(EventCommand.ListEvents.class, this::onListEvents)
        .onMessage(EventCommand.PatchEvent.class, this::onPatchEvent)
        .onMessage(EventCommand.DeleteEvent.class, this::onDeleteEvent)
        .build();
  }

  private Behavior<EventCommand> onCreateEvent(EventCommand.CreateEvent cmd) {
    try {
      var validationError = EventValidation.validateCreate(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(EventActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      executeCreate(cmd.tenantId(), cmd.request(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "create event", e);
    }
    return this;
  }

  private void executeCreate(
      UUID tenantId, CreateEventRequest request, ActorRef<EventActorResponse> replyTo) {
    var domain =
        eventDao.create(
            tenantId,
            request.title(),
            request.summary(),
            request.locationId(),
            request.startTime(),
            request.endTime());
    replyTo.tell(EventActorResponse.success(toEventResponse(domain)));
  }

  private Behavior<EventCommand> onGetEvent(EventCommand.GetEvent cmd) {
    try {
      executeGet(cmd.tenantId(), cmd.eventId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "get event", e);
    }
    return this;
  }

  private void executeGet(UUID tenantId, UUID eventId, ActorRef<EventActorResponse> replyTo) {
    eventDao
        .findById(tenantId, eventId)
        .ifPresentOrElse(
            event -> replyTo.tell(EventActorResponse.success(toEventResponse(event))),
            () -> replyTo.tell(EventActorResponse.notFound("Event not found: " + eventId)));
  }

  private Behavior<EventCommand> onListEvents(EventCommand.ListEvents cmd) {
    try {
      executeList(cmd.tenantId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "list events", e);
    }
    return this;
  }

  private void executeList(UUID tenantId, ActorRef<EventActorResponse> replyTo) {
    var events =
        eventDao.listAll(tenantId).stream().map(this::toEventResponse).collect(toImmutableList());
    replyTo.tell(EventActorResponse.items(events));
  }

  private Behavior<EventCommand> onPatchEvent(EventCommand.PatchEvent cmd) {
    try {
      var validationError = EventValidation.validatePatch(cmd.request());
      if (validationError.isPresent()) {
        cmd.replyTo().tell(EventActorResponse.badRequest(validationError.orElseThrow()));
        return this;
      }
      executePatch(cmd.tenantId(), cmd.eventId(), cmd.request(), cmd.updateMask(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "patch event", e);
    }
    return this;
  }

  private void executePatch(
      UUID tenantId,
      UUID eventId,
      UpdateEventRequest request,
      Optional<String> updateMask,
      ActorRef<EventActorResponse> replyTo) {
    var maskFields = parseUpdateMask(updateMask);
    var title = selectField(maskFields, "title", "title", request.title());
    var summary = selectField(maskFields, "summary", "summary", request.summary());
    var locationId = selectField(maskFields, "locationId", "location_id", request.locationId());
    var startTime = selectField(maskFields, "startTime", "start_time", request.startTime());
    var endTime = selectField(maskFields, "endTime", "end_time", request.endTime());

    eventDao
        .patch(tenantId, eventId, title, summary, locationId, startTime, endTime)
        .ifPresentOrElse(
            updated -> replyTo.tell(EventActorResponse.success(toEventResponse(updated))),
            () -> replyTo.tell(EventActorResponse.notFound("Event not found: " + eventId)));
  }

  private Behavior<EventCommand> onDeleteEvent(EventCommand.DeleteEvent cmd) {
    try {
      executeDelete(cmd.tenantId(), cmd.eventId(), cmd.replyTo());
    } catch (Exception e) {
      handleError(cmd.replyTo(), "delete event", e);
    }
    return this;
  }

  private void executeDelete(UUID tenantId, UUID eventId, ActorRef<EventActorResponse> replyTo) {
    var deleted = eventDao.softDelete(tenantId, eventId);
    if (deleted) {
      replyTo.tell(EventActorResponse.deleted());
    } else {
      replyTo.tell(EventActorResponse.notFound("Event not found: " + eventId));
    }
  }

  private EventResponse toEventResponse(Event event) {
    return new EventResponse(
        event.id(),
        event.title(),
        event.summary(),
        event.locationId(),
        event.startTime(),
        event.endTime(),
        event.createdOn(),
        event.updatedOn());
  }

  private Optional<Set<String>> parseUpdateMask(Optional<String> updateMask) {
    return updateMask.map(
        mask -> ImmutableSet.copyOf(Splitter.on(',').trimResults().omitEmptyStrings().split(mask)));
  }

  private <T> Optional<T> selectField(
      Optional<Set<String>> maskFields,
      String camelCaseField,
      String snakeCaseField,
      Optional<T> requestValue) {
    if (maskFields.isPresent()) {
      var set = maskFields.orElseThrow();
      var matches = set.contains(camelCaseField) || set.contains(snakeCaseField);
      return matches ? requestValue : Optional.empty();
    }
    return requestValue;
  }

  private void handleError(
      ActorRef<EventActorResponse> replyTo, String operation, Exception error) {
    logger.error("Failed to {} in EventActor", operation, error);
    if (isConstraintViolation(error)) {
      replyTo.tell(EventActorResponse.badRequest("Request data violates database constraints"));
      return;
    }
    var message = error.getMessage();
    var reason = !Strings.isNullOrEmpty(message) ? message : "Error executing " + operation;
    replyTo.tell(EventActorResponse.failure(reason));
  }

  private static boolean isConstraintViolation(Throwable throwable) {
    var current = throwable;
    while (current != null) {
      if (current instanceof ConstraintViolationException) {
        return true;
      }
      var msg = current.getMessage();
      if (msg != null
          && (msg.contains("violates foreign key constraint")
              || msg.contains("violates check constraint")
              || msg.contains("fk_events_locations")
              || msg.contains("chk_events_time_order"))) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
