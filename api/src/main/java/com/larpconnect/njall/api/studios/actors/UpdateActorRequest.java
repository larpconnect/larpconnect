package com.larpconnect.njall.api.studios.actors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for partially updating a tenanted studio actor. */
@Immutable
public record UpdateActorRequest(
    @JsonProperty("preferredUsername") Optional<String> preferredUsername,
    @JsonProperty("summary") Optional<String> summary) {

  public UpdateActorRequest() {
    this(Optional.empty(), Optional.empty());
  }

  @JsonCreator
  public UpdateActorRequest(
      @JsonProperty("preferredUsername") @Nullable String preferredUsername,
      @JsonProperty("summary") @Nullable String summary) {
    this(Optional.ofNullable(preferredUsername), Optional.ofNullable(summary));
  }
}
