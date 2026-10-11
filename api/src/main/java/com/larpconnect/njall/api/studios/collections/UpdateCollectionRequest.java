package com.larpconnect.njall.api.studios.collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for partially updating an existing studio collection (AIP-134). */
@Immutable
public record UpdateCollectionRequest(
    @JsonProperty("name") Optional<String> name,
    @JsonProperty("summary") Optional<String> summary) {

  @JsonCreator
  public UpdateCollectionRequest(
      @JsonProperty("name") @Nullable String name,
      @JsonProperty("summary") @Nullable String summary) {
    this(Optional.ofNullable(name), Optional.ofNullable(summary));
  }
}
