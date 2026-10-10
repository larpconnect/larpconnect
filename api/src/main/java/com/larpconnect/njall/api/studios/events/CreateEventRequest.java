package com.larpconnect.njall.api.studios.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new studio event. */
@Immutable
public record CreateEventRequest(
    @JsonProperty("title") String title,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("locationId") Optional<UUID> locationId,
    @JsonProperty("startTime") Optional<Instant> startTime,
    @JsonProperty("endTime") Optional<Instant> endTime) {

  public CreateEventRequest(String title) {
    this(title, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  @JsonCreator
  public CreateEventRequest(
      @JsonProperty("title") String title,
      @JsonProperty("summary") @Nullable String summary,
      @JsonProperty("locationId") @Nullable UUID locationId,
      @JsonProperty("startTime") @Nullable Instant startTime,
      @JsonProperty("endTime") @Nullable Instant endTime) {
    this(
        title,
        Optional.ofNullable(summary),
        Optional.ofNullable(locationId),
        Optional.ofNullable(startTime),
        Optional.ofNullable(endTime));
  }
}
