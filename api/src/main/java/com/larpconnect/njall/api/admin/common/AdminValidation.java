package com.larpconnect.njall.api.admin.common;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/** Validation utilities for admin endpoints and canonical identifiers. */
@Immutable
public final class AdminValidation {

  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");

  private AdminValidation() {}

  /**
   * Validates if the given string adheres to the canonical identifier pattern: alphanumeric
   * lowercase, underscores, starting with a letter.
   *
   * @param value The value to validate.
   * @return true if valid, false otherwise.
   */
  public static boolean isValidIdentifier(@Nullable String value) {
    if (value == null) {
      return false;
    }
    return IDENTIFIER_PATTERN.matcher(value).matches();
  }

  /**
   * Attempts to parse a string as a {@link UUID}.
   *
   * @param value The string to parse.
   * @return Optional containing the UUID if successfully parsed, empty otherwise.
   */
  public static Optional<UUID> tryParseUuid(@Nullable String value) {
    if (value == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
