package com.larpconnect.njall.api.studios.actors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new tenanted studio actor. */
@Immutable
public record CreateActorRequest(
    @JsonProperty("generator") Optional<String> generator,
    @JsonProperty("preferredUsername") String preferredUsername,
    @JsonProperty("summary") Optional<String> summary) {

  @JsonCreator
  public CreateActorRequest(
      @JsonProperty("generator") @Nullable String generator,
      @JsonProperty("preferredUsername") String preferredUsername,
      @JsonProperty("summary") @Nullable String summary) {
    this(Optional.ofNullable(generator), preferredUsername, Optional.ofNullable(summary));
  }
}
