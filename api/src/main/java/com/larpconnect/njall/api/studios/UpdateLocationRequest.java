package com.larpconnect.njall.api.studios;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for partially updating a studio location (AIP-134). */
@Immutable
public record UpdateLocationRequest(
    @JsonProperty("name") Optional<String> name,
    @JsonProperty("summary") Optional<String> summary) {

  public UpdateLocationRequest() {
    this(Optional.empty(), Optional.empty());
  }

  @JsonCreator
  public UpdateLocationRequest(
      @JsonProperty("name") @Nullable String name,
      @JsonProperty("summary") @Nullable String summary) {
    this(Optional.ofNullable(name), Optional.ofNullable(summary));
  }
}
