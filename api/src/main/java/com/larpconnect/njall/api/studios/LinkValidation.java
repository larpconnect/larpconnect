package com.larpconnect.njall.api.studios;

import com.google.common.base.Strings;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Validation utilities for tenanted studio link requests. */
final class LinkValidation {

  private LinkValidation() {}

  /**
   * Attempts to parse a string as a {@link UUID}.
   *
   * @param value The string to parse.
   * @return Optional containing the UUID if successfully parsed, empty otherwise.
   */
  static Optional<UUID> tryParseUuid(@Nullable String value) {
    if (value == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  /**
   * Checks whether the provided string is a valid URI with an absolute scheme.
   *
   * @param value The string to validate.
   * @return true if valid URI, false otherwise.
   */
  static boolean isValidUri(@Nullable String value) {
    if (Strings.isNullOrEmpty(value)) {
      return false;
    }
    try {
      var uri = URI.create(value);
      return uri.getScheme() != null && !uri.getScheme().isBlank();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Validates a link creation request payload.
   *
   * @param request The creation request.
   * @return Error message if invalid, or empty if valid.
   */
  static Optional<String> validateCreate(CreateLinkRequest request) {
    if (request.linkType().isBlank()) {
      return Optional.of("linkType must not be blank");
    }
    if (!isValidUri(request.url())) {
      return Optional.of("url must be a valid URI");
    }
    if (request.mediaType().isPresent() && request.mediaType().get().isBlank()) {
      return Optional.of("mediaType must not be blank if provided");
    }
    return Optional.empty();
  }

  /**
   * Validates a link update request payload.
   *
   * @param request The update request.
   * @return Error message if invalid, or empty if valid.
   */
  static Optional<String> validateUpdate(UpdateLinkRequest request) {
    if (request.linkType().isPresent() && request.linkType().get().isBlank()) {
      return Optional.of("linkType must not be blank");
    }
    if (request.url().isPresent() && !isValidUri(request.url().get())) {
      return Optional.of("url must be a valid URI");
    }
    if (request.mediaType().isPresent() && request.mediaType().get().isBlank()) {
      return Optional.of("mediaType must not be blank");
    }
    return Optional.empty();
  }
}
