package com.larpconnect.njall.api.studios.individuals;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new studio individual. */
@Immutable
public record CreateIndividualRequest(
    @JsonProperty("name") String name, @JsonProperty("summary") Optional<String> summary) {

  public CreateIndividualRequest(String name) {
    this(name, Optional.empty());
  }

  @JsonCreator
  public CreateIndividualRequest(
      @JsonProperty("name") String name, @JsonProperty("summary") @Nullable String summary) {
    this(name, Optional.ofNullable(summary));
  }
}
