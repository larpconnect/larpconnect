package com.larpconnect.njall.api.studios.games;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Request payload for partially updating an existing studio game conforming to AIP-134. */
@Immutable
public record UpdateGameRequest(
    @JsonProperty("title") Optional<String> title,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("system") Optional<String> system,
    @JsonProperty("campaign") Optional<String> campaign,
    @JsonProperty("locationId") Optional<UUID> locationId,
    @JsonProperty("startTime") Optional<Instant> startTime,
    @JsonProperty("endTime") Optional<Instant> endTime) {

  /** Creates an empty game update request. */
  public UpdateGameRequest() {
    this(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  /** Creates a game update request normalizing nullable JSON properties to empty optionals. */
  @JsonCreator
  public UpdateGameRequest(
      @JsonProperty("title") @Nullable String title,
      @JsonProperty("summary") @Nullable String summary,
      @JsonProperty("system") @Nullable String system,
      @JsonProperty("campaign") @Nullable String campaign,
      @JsonProperty("locationId") @Nullable UUID locationId,
      @JsonProperty("startTime") @Nullable Instant startTime,
      @JsonProperty("endTime") @Nullable Instant endTime) {
    this(
        Optional.ofNullable(title),
        Optional.ofNullable(summary),
        Optional.ofNullable(system),
        Optional.ofNullable(campaign),
        Optional.ofNullable(locationId),
        Optional.ofNullable(startTime),
        Optional.ofNullable(endTime));
  }
}
