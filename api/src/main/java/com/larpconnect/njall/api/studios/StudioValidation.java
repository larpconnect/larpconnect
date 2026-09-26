package com.larpconnect.njall.api.studios;

import java.util.Optional;
import java.util.UUID;

final class StudioValidation {

  private StudioValidation() {}

  static Optional<UUID> tryParseUuid(String value) {
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
