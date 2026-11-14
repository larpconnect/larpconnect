package com.larpconnect.njall.api.studios.games;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Public response representation for a studio game. */
@Immutable
public record GameResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("generator") String generator,
    @JsonProperty("system") String system,
    @JsonProperty("campaign") Optional<String> campaign,
    @JsonProperty("eventType") EventType eventType,
    @JsonProperty("title") String title,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("locationId") Optional<UUID> locationId,
    @JsonProperty("startTime") Optional<Instant> startTime,
    @JsonProperty("endTime") Optional<Instant> endTime,
    @JsonProperty("createdOn") Instant createdOn,
    @JsonProperty("updatedOn") Instant updatedOn) {}
