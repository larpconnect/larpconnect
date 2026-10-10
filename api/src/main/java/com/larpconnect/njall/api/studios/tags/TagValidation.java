package com.larpconnect.njall.api.studios.tags;

import java.text.Normalizer;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/** Validation and normalization utilities for tenanted studio hashtag requests. */
final class TagValidation {

  private static final int MAX_TAG_LENGTH = 32;
  private static final int MAX_BATCH_SIZE = 50;
  private static final Pattern VALID_TAG_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_-]+$");

  private TagValidation() {}

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
   * Sanitizes a tag string by stripping leading '#' display symbols, trimming, and normalizing.
   *
   * @param raw The raw tag input.
   * @return The sanitized tag string.
   */
  static String sanitizeTag(@Nullable String raw) {
    if (raw == null) {
      return "";
    }
    var trimmed = raw.trim();
    if (trimmed.startsWith("#")) {
      trimmed = trimmed.substring(1).trim();
    }
    return Normalizer.normalize(trimmed, Normalizer.Form.NFC);
  }

  /**
   * Validates a tag name string.
   *
   * @param raw The raw tag string.
   * @return Error message if invalid, or empty if valid.
   */
  static Optional<String> validateTagName(@Nullable String raw) {
    var tag = sanitizeTag(raw);
    if (tag.isEmpty()) {
      return Optional.of("tag must not be blank");
    }
    if (tag.length() > MAX_TAG_LENGTH) {
      return Optional.of("tag must not exceed 32 characters in length");
    }
    if (!VALID_TAG_PATTERN.matcher(tag).matches()) {
      return Optional.of(
          "tag must contain only web-safe Unicode letters, digits, underscores, or hyphens");
    }
    return Optional.empty();
  }

  /**
   * Validates a hashtag creation request payload.
   *
   * @param request The creation request.
   * @return Error message if invalid, or empty if valid.
   */
  static Optional<String> validateCreate(CreateTagRequest request) {
    return validateTagName(request.tag());
  }

  /**
   * Validates a batch hashtag creation request payload.
   *
   * @param request The batch creation request.
   * @return Error message if invalid, or empty if valid.
   */
  static Optional<String> validateBatchCreate(BatchCreateTagsRequest request) {
    if (request.requests().isEmpty()) {
      return Optional.of("requests must not be empty");
    }
    if (request.requests().size() > MAX_BATCH_SIZE) {
      return Optional.of("requests array must not exceed 50 items");
    }
    for (var item : request.requests()) {
      var error = validateCreate(item);
      if (error.isPresent()) {
        return error;
      }
    }
    return Optional.empty();
  }

  /**
   * Validates a hashtag update request payload.
   *
   * @param request The update request.
   * @return Error message if invalid, or empty if valid.
   */
  static Optional<String> validateUpdate(UpdateTagRequest request) {
    if (request.tag().isEmpty() && request.summary().isEmpty()) {
      return Optional.of("At least one field (tag or summary) must be specified for update");
    }
    if (request.tag().isPresent()) {
      return validateTagName(request.tag().get());
    }
    return Optional.empty();
  }
}
