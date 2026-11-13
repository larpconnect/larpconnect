package com.larpconnect.njall.api.studios.collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new studio collection. */
@Immutable
public record CreateCollectionRequest(
    @JsonProperty("ownerId") Optional<UUID> ownerId,
    @JsonProperty("name") String name,
    @JsonProperty("summary") Optional<String> summary) {

  @JsonCreator
  public CreateCollectionRequest(
      @JsonProperty("ownerId") @Nullable UUID ownerId,
      @JsonProperty("name") String name,
      @JsonProperty("summary") @Nullable String summary) {
    this(Optional.ofNullable(ownerId), name, Optional.ofNullable(summary));
  }
}
