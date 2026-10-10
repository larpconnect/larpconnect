package com.larpconnect.njall.api.studios.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Public HTTP representation of a tenanted studio event. */
@Immutable
public record EventResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("title") String title,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("locationId") Optional<UUID> locationId,
    @JsonProperty("startTime") Optional<Instant> startTime,
    @JsonProperty("endTime") Optional<Instant> endTime,
    @JsonProperty("createdOn") Instant createdOn,
    @JsonProperty("updatedOn") Instant updatedOn) {}
