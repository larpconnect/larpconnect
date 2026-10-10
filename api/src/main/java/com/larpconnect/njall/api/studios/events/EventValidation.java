package com.larpconnect.njall.api.studios.events;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.UUID;

final class EventValidation {

  private EventValidation() {}

  static Optional<String> validateCreate(CreateEventRequest request) {
    if (Strings.isNullOrEmpty(request.title()) || request.title().isBlank()) {
      return Optional.of("Event title must not be blank");
    }
    if (request.startTime().isPresent()
        && request.endTime().isPresent()
        && request.endTime().orElseThrow().isBefore(request.startTime().orElseThrow())) {
      return Optional.of("Event end time must not be earlier than start time");
    }
    return Optional.empty();
  }

  static Optional<String> validatePatch(UpdateEventRequest request) {
    if (request.title().isPresent()) {
      var title = request.title().orElseThrow();
      if (title.isBlank()) {
        return Optional.of("Event title must not be blank");
      }
    }
    if (request.startTime().isPresent()
        && request.endTime().isPresent()
        && request.endTime().orElseThrow().isBefore(request.startTime().orElseThrow())) {
      return Optional.of("Event end time must not be earlier than start time");
    }
    return Optional.empty();
  }

  static Optional<UUID> tryParseUuid(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
