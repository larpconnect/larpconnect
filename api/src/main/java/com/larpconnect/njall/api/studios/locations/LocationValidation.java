package com.larpconnect.njall.api.studios.locations;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.UUID;

final class LocationValidation {

  private LocationValidation() {}

  static Optional<String> validateCreate(CreateLocationRequest request) {
    if (Strings.isNullOrEmpty(request.name()) || request.name().isBlank()) {
      return Optional.of("Location name must not be blank");
    }
    if (request.name().length() > 255) {
      return Optional.of("Location name must not exceed 255 characters");
    }
    return Optional.empty();
  }

  static Optional<String> validatePatch(UpdateLocationRequest request) {
    if (request.name().isPresent()) {
      var name = request.name().orElseThrow();
      if (name.isBlank()) {
        return Optional.of("Location name must not be blank");
      }
      if (name.length() > 255) {
        return Optional.of("Location name must not exceed 255 characters");
      }
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
