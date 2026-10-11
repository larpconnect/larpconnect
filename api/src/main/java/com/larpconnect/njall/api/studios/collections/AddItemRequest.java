package com.larpconnect.njall.api.studios.collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Request payload for inserting an entity reference into a collection (AIP-136). */
@Immutable
public record AddItemRequest(
    @JsonProperty("refersToId") UUID refersToId,
    @JsonProperty("before") Optional<UUID> before,
    @JsonProperty("index") Optional<Integer> index) {

  public AddItemRequest(UUID refersToId) {
    this(refersToId, Optional.empty(), Optional.empty());
  }

  @JsonCreator
  public AddItemRequest(
      @JsonProperty("refersToId") UUID refersToId,
      @JsonProperty("before") @Nullable UUID before,
      @JsonProperty("index") @Nullable Integer index) {
    this(refersToId, Optional.ofNullable(before), Optional.ofNullable(index));
  }
}
