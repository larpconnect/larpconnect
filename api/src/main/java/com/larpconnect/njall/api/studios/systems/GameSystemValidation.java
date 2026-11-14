package com.larpconnect.njall.api.studios.systems;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.api.studios.common.UpdateMaskSupport;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Pure validation helpers for game system requests and identifiers. */
@Immutable
final class GameSystemValidation {

  private GameSystemValidation() {}

  /** Validates a create request, returning a field error message if invalid. */
  static Optional<String> validateCreate(CreateGameSystemRequest request) {
    if (request.generator().isBlank()) {
      return Optional.of("generator is required");
    }
    if (request.name().isBlank()) {
      return Optional.of("name is required");
    }
    return Optional.empty();
  }

  /** Validates a patch request against an optional update mask. */
  static Optional<String> validatePatch(
      UpdateGameSystemRequest request, Optional<ImmutableSet<String>> maskFields) {
    if (UpdateMaskSupport.isFieldCleared(maskFields, request.name(), "name")) {
      return Optional.of("name cannot be cleared");
    }
    if (UpdateMaskSupport.isFieldTargeted(maskFields, request.name(), "name")
        && request.name().filter(String::isBlank).isPresent()) {
      return Optional.of("name must not be blank");
    }
    return Optional.empty();
  }

  /** Attempts to parse a UUID string, returning empty when null, blank, or malformed. */
  static Optional<UUID> tryParseUuid(@Nullable String value) {
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
