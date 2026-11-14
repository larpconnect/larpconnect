package com.larpconnect.njall.api.studios.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new studio event. */
@Immutable
public record CreateEventRequest(
    @JsonProperty("generator") String generator,
    @JsonProperty("title") String title,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("eventType") Optional<EventType> eventType,
    @JsonProperty("locationId") Optional<UUID> locationId,
    @JsonProperty("startTime") Optional<Instant> startTime,
    @JsonProperty("endTime") Optional<Instant> endTime) {

  public CreateEventRequest(String generator, String title) {
    this(
        generator,
        title,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  public CreateEventRequest(
      String generator,
      String title,
      @Nullable String summary,
      @Nullable UUID locationId,
      @Nullable Instant startTime,
      @Nullable Instant endTime) {
    this(
        generator,
        title,
        Optional.ofNullable(summary),
        Optional.empty(),
        Optional.ofNullable(locationId),
        Optional.ofNullable(startTime),
        Optional.ofNullable(endTime));
  }

  @JsonCreator
  public CreateEventRequest(
      @JsonProperty("generator") @Nullable String generator,
      @JsonProperty("title") @Nullable String title,
      @JsonProperty("summary") @Nullable String summary,
      @JsonProperty("eventType") @Nullable EventType eventType,
      @JsonProperty("locationId") @Nullable UUID locationId,
      @JsonProperty("startTime") @Nullable Instant startTime,
      @JsonProperty("endTime") @Nullable Instant endTime) {
    this(
        Strings.nullToEmpty(generator),
        Strings.nullToEmpty(title),
        Optional.ofNullable(summary),
        Optional.ofNullable(eventType),
        Optional.ofNullable(locationId),
        Optional.ofNullable(startTime),
        Optional.ofNullable(endTime));
  }
}
