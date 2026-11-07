package com.larpconnect.njall.api.studios.individuals;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new studio individual. */
@Immutable
public record CreateIndividualRequest(
    @JsonProperty("generator") String generator,
    @JsonProperty("name") String name,
    @JsonProperty("summary") Optional<String> summary) {

  public CreateIndividualRequest(String generator, String name) {
    this(generator, name, Optional.empty());
  }

  @JsonCreator
  public CreateIndividualRequest(
      @JsonProperty("generator") String generator,
      @JsonProperty("name") String name,
      @JsonProperty("summary") @Nullable String summary) {
    this(generator, name, Optional.ofNullable(summary));
  }
}
