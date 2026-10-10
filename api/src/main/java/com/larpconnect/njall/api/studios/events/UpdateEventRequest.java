package com.larpconnect.njall.api.studios.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Request payload for partially updating a studio event (AIP-134). */
@Immutable
public record UpdateEventRequest(
    @JsonProperty("title") Optional<String> title,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("locationId") Optional<UUID> locationId,
    @JsonProperty("startTime") Optional<Instant> startTime,
    @JsonProperty("endTime") Optional<Instant> endTime) {

  public UpdateEventRequest() {
    this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  @JsonCreator
  public UpdateEventRequest(
      @JsonProperty("title") @Nullable String title,
      @JsonProperty("summary") @Nullable String summary,
      @JsonProperty("locationId") @Nullable UUID locationId,
      @JsonProperty("startTime") @Nullable Instant startTime,
      @JsonProperty("endTime") @Nullable Instant endTime) {
    this(
        Optional.ofNullable(title),
        Optional.ofNullable(summary),
        Optional.ofNullable(locationId),
        Optional.ofNullable(startTime),
        Optional.ofNullable(endTime));
  }
}
