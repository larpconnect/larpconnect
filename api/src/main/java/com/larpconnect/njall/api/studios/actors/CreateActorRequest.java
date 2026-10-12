package com.larpconnect.njall.api.studios.actors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new tenanted studio actor. */
@Immutable
public record CreateActorRequest(
    @JsonProperty("ownerId") UUID ownerId,
    @JsonProperty("preferredUsername") String preferredUsername,
    @JsonProperty("summary") Optional<String> summary) {

  public CreateActorRequest(UUID ownerId, String preferredUsername) {
    this(ownerId, preferredUsername, Optional.empty());
  }

  @JsonCreator
  public CreateActorRequest(
      @JsonProperty("ownerId") UUID ownerId,
      @JsonProperty("preferredUsername") String preferredUsername,
      @JsonProperty("summary") @Nullable String summary) {
    this(ownerId, preferredUsername, Optional.ofNullable(summary));
  }
}
