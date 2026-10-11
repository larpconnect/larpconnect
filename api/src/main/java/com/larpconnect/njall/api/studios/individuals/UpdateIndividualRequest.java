package com.larpconnect.njall.api.studios.individuals;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for partially updating a studio individual (AIP-134). */
@Immutable
public record UpdateIndividualRequest(
    @JsonProperty("name") Optional<String> name,
    @JsonProperty("summary") Optional<String> summary) {

  public UpdateIndividualRequest() {
    this(Optional.empty(), Optional.empty());
  }

  @JsonCreator
  public UpdateIndividualRequest(
      @JsonProperty("name") @Nullable String name,
      @JsonProperty("summary") @Nullable String summary) {
    this(Optional.ofNullable(name), Optional.ofNullable(summary));
  }
}
