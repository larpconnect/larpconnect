package com.larpconnect.njall.api.studios.systems;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Request payload for partially updating a game system. */
@Immutable
public record UpdateGameSystemRequest(
    @JsonProperty("name") Optional<String> name,
    @JsonProperty("summary") Optional<String> summary) {

  /** Creates an empty update request. */
  public UpdateGameSystemRequest() {
    this(Optional.empty(), Optional.empty());
  }

  /** Jackson creator converting nullable JSON fields into {@link Optional} values. */
  @JsonCreator
  public UpdateGameSystemRequest(
      @JsonProperty("name") @Nullable String name,
      @JsonProperty("summary") @Nullable String summary) {
    this(Optional.ofNullable(name), Optional.ofNullable(summary));
  }
}
