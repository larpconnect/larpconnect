package com.larpconnect.njall.api.studios.games;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.api.studios.common.UpdateMaskSupport;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Validation and identifier-parsing utilities for tenanted studio game requests. */
@Immutable
final class GameValidation {

  private GameValidation() {}

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

  static Optional<String> validateCreate(CreateGameRequest request) {
    if (request.generator().isBlank()) {
      return Optional.of("Generator URI or actor identifier must not be blank");
    }
    if (request.system().isBlank()) {
      return Optional.of("System URI or identifier must not be blank");
    }
    if (request.title().isBlank()) {
      return Optional.of("Game title must not be blank");
    }
    if (request.campaign().filter(String::isBlank).isPresent()) {
      return Optional.of("Campaign URI or identifier must not be blank");
    }
    return validateTimeOrder(request.startTime(), request.endTime());
  }

  static Optional<String> validatePatch(
      UpdateGameRequest request, Optional<ImmutableSet<String>> maskFields) {
    return validateRequiredPatchFields(request, maskFields)
        .or(() -> validateOptionalPatchFields(request, maskFields));
  }

  private static Optional<String> validateRequiredPatchFields(
      UpdateGameRequest request, Optional<ImmutableSet<String>> maskFields) {
    if (UpdateMaskSupport.isFieldCleared(maskFields, request.title(), "title")) {
      return Optional.of("Game title must not be cleared");
    }
    if (UpdateMaskSupport.isFieldTargeted(maskFields, request.title(), "title")
        && request.title().filter(String::isBlank).isPresent()) {
      return Optional.of("Game title must not be blank");
    }
    if (UpdateMaskSupport.isFieldCleared(maskFields, request.system(), "system")) {
      return Optional.of("Game system must not be cleared");
    }
    if (UpdateMaskSupport.isFieldTargeted(maskFields, request.system(), "system")
        && request.system().filter(String::isBlank).isPresent()) {
      return Optional.of("System URI or identifier must not be blank");
    }
    return Optional.empty();
  }

  private static Optional<String> validateOptionalPatchFields(
      UpdateGameRequest request, Optional<ImmutableSet<String>> maskFields) {
    var campaign =
        UpdateMaskSupport.selectOptionalField(
            request.campaign(), Optional.empty(), maskFields, "campaign");
    if (campaign.filter(String::isBlank).isPresent()) {
      return Optional.of("Campaign URI or identifier must not be blank");
    }
    var startTime =
        UpdateMaskSupport.selectOptionalField(
            request.startTime(), Optional.empty(), maskFields, "startTime", "start_time");
    var endTime =
        UpdateMaskSupport.selectOptionalField(
            request.endTime(), Optional.empty(), maskFields, "endTime", "end_time");
    return validateTimeOrder(startTime, endTime);
  }

  private static Optional<String> validateTimeOrder(
      Optional<Instant> start, Optional<Instant> end) {
    if (start.isPresent() && end.isPresent() && end.orElseThrow().isBefore(start.orElseThrow())) {
      return Optional.of("endTime must not be before startTime");
    }
    return Optional.empty();
  }
}
