package com.larpconnect.njall.api.studios.actors;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

final class AgentValidation {

  private static final Pattern ENDPOINT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,31}$");

  private AgentValidation() {}

  static Optional<String> validateCreate(CreateActorRequest request) {
    if (Strings.isNullOrEmpty(request.preferredUsername())
        || request.preferredUsername().isBlank()) {
      return Optional.of("Preferred username must not be blank");
    }
    return Optional.empty();
  }

  static Optional<String> validatePatch(UpdateActorRequest request) {
    return request
        .preferredUsername()
        .filter(String::isBlank)
        .map(name -> "Preferred username must not be blank");
  }

  static Optional<String> validateAddEndpoint(AddEndpointRequest request) {
    if (Strings.isNullOrEmpty(request.name())
        || !ENDPOINT_NAME_PATTERN.matcher(request.name()).matches()) {
      return Optional.of("Endpoint name must be alphanumeric and less than 32 characters");
    }
    if (Strings.isNullOrEmpty(request.endpoint()) || request.endpoint().isBlank()) {
      return Optional.of("Endpoint URI must not be blank");
    }
    return Optional.empty();
  }

  static Optional<String> validateRemoveEndpoint(RemoveEndpointRequest request) {
    if (Strings.isNullOrEmpty(request.name()) || request.name().isBlank()) {
      return Optional.of("Endpoint name must not be blank");
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
