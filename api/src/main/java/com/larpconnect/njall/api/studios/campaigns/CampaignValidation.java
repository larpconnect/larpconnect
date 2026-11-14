package com.larpconnect.njall.api.studios.campaigns;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.api.studios.common.UpdateMaskSupport;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Immutable
final class CampaignValidation {

  private CampaignValidation() {}

  static Optional<String> validateCreate(CreateCampaignRequest request) {
    if (request.generator().isBlank()) {
      return Optional.of("Generator URI or actor identifier must not be blank");
    }
    if (request.title().isBlank()) {
      return Optional.of("Campaign title must not be blank");
    }
    return Optional.empty();
  }

  static Optional<String> validatePatch(
      UpdateCampaignRequest request, Optional<ImmutableSet<String>> maskFields) {
    if (UpdateMaskSupport.isFieldCleared(maskFields, request.title(), "title")) {
      return Optional.of("Campaign title cannot be cleared");
    }
    if (UpdateMaskSupport.isFieldTargeted(maskFields, request.title(), "title")
        && request.title().filter(String::isBlank).isPresent()) {
      return Optional.of("Campaign title must not be blank");
    }
    return Optional.empty();
  }

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
