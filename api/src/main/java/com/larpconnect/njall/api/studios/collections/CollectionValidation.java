package com.larpconnect.njall.api.studios.collections;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.UUID;

final class CollectionValidation {

  private CollectionValidation() {}

  static Optional<String> validateCreate(CreateCollectionRequest request) {
    if (Strings.isNullOrEmpty(request.name()) || request.name().isBlank()) {
      return Optional.of("Collection name must not be blank");
    }
    return Optional.empty();
  }

  static Optional<String> validatePatch(UpdateCollectionRequest request) {
    return request.name().filter(String::isBlank).map(name -> "Collection name must not be blank");
  }

  static Optional<String> validateAddItem(AddItemRequest request) {
    if (request.before().isPresent() && request.index().isPresent()) {
      return Optional.of("Cannot specify both 'before' and 'index' placement specifiers");
    }
    if (request.index().isPresent() && request.index().get() < 0) {
      return Optional.of("Index must be non-negative");
    }
    return Optional.empty();
  }

  /**
   * Validates a remove item request.
   *
   * <p>Currently an intentional no-op placeholder for future AIP removal constraints.
   *
   * @param request the remove request payload
   * @return empty optional if valid, or a validation failure message
   */
  static Optional<String> validateRemoveItem(RemoveItemRequest request) {
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
