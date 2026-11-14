package com.larpconnect.njall.api.studios.games;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a studio game. */
@Immutable
public record CreateGameRequest(
    @JsonProperty("generator") String generator,
    @JsonProperty("system") String system,
    @JsonProperty("campaign") Optional<String> campaign,
    @JsonProperty("title") String title,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("locationId") Optional<UUID> locationId,
    @JsonProperty("startTime") Optional<Instant> startTime,
    @JsonProperty("endTime") Optional<Instant> endTime) {

  /** Creates a game request with required fields only. */
  public CreateGameRequest(String generator, String system, String title) {
    this(
        generator,
        system,
        Optional.empty(),
        title,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  /** Creates a game creation request normalizing nullable JSON properties. */
  @JsonCreator
  public CreateGameRequest(
      @JsonProperty("generator") @Nullable String generator,
      @JsonProperty("system") @Nullable String system,
      @JsonProperty("campaign") @Nullable String campaign,
      @JsonProperty("title") @Nullable String title,
      @JsonProperty("summary") @Nullable String summary,
      @JsonProperty("locationId") @Nullable UUID locationId,
      @JsonProperty("startTime") @Nullable Instant startTime,
      @JsonProperty("endTime") @Nullable Instant endTime) {
    this(
        Strings.nullToEmpty(generator),
        Strings.nullToEmpty(system),
        Optional.ofNullable(campaign),
        Strings.nullToEmpty(title),
        Optional.ofNullable(summary),
        Optional.ofNullable(locationId),
        Optional.ofNullable(startTime),
        Optional.ofNullable(endTime));
  }
}
