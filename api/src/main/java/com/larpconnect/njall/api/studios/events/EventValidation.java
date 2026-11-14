package com.larpconnect.njall.api.studios.events;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.api.studios.common.UpdateMaskSupport;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Immutable
final class EventValidation {

  private EventValidation() {}

  static Optional<String> validateCreate(CreateEventRequest request) {
    if (request.generator().isBlank()) {
      return Optional.of("Generator URI or actor identifier must not be blank");
    }
    if (request.title().isBlank()) {
      return Optional.of("Event title must not be blank");
    }
    if (isGameEventType(request.eventType())) {
      return Optional.of("Events with eventType GAME must be created via /v1/games");
    }
    if (hasInvalidTimeRange(request.startTime(), request.endTime())) {
      return Optional.of("Event end time must not be earlier than start time");
    }
    return Optional.empty();
  }

  static Optional<String> validatePatch(
      UpdateEventRequest request, Optional<ImmutableSet<String>> maskFields) {
    var targetedTitle =
        UpdateMaskSupport.selectOptionalField(
            request.title(), Optional.empty(), maskFields, "title");
    if (targetedTitle.filter(String::isBlank).isPresent()) {
      return Optional.of("Event title must not be blank");
    }
    var targetedType =
        UpdateMaskSupport.selectOptionalField(
            request.eventType(), Optional.empty(), maskFields, "eventType", "event_type");
    if (isGameEventType(targetedType)) {
      return Optional.of("Events cannot be updated to eventType GAME");
    }
    var targetedStart =
        UpdateMaskSupport.selectOptionalField(
            request.startTime(), Optional.empty(), maskFields, "startTime", "start_time");
    var targetedEnd =
        UpdateMaskSupport.selectOptionalField(
            request.endTime(), Optional.empty(), maskFields, "endTime", "end_time");
    if (hasInvalidTimeRange(targetedStart, targetedEnd)) {
      return Optional.of("Event end time must not be earlier than start time");
    }
    return Optional.empty();
  }

  static Optional<UUID> tryParseUuid(@Nullable String value) {
    if (value == null || value.isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static boolean isGameEventType(Optional<EventType> eventType) {
    return eventType.filter(type -> type == EventType.GAME).isPresent();
  }

  private static boolean hasInvalidTimeRange(
      Optional<Instant> startTime, Optional<Instant> endTime) {
    return startTime.isPresent()
        && endTime.isPresent()
        && endTime.orElseThrow().isBefore(startTime.orElseThrow());
  }
}
