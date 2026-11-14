package com.larpconnect.njall.api.studios.systems;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Response payload representing a game system resource. */
@Immutable
public record GameSystemResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("generator") String generator,
    @JsonProperty("name") String name,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("createdOn") Instant createdOn,
    @JsonProperty("updatedOn") Instant updatedOn) {}
