package com.larpconnect.njall.api.studios.individuals;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.UUID;

final class IndividualValidation {

  private IndividualValidation() {}

  static Optional<String> validateCreate(CreateIndividualRequest request) {
    if (Strings.isNullOrEmpty(request.name()) || request.name().isBlank()) {
      return Optional.of("Individual name must not be blank");
    }
    return Optional.empty();
  }

  static Optional<String> validatePatch(UpdateIndividualRequest request) {
    return request.name().filter(String::isBlank).map(name -> "Individual name must not be blank");
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
