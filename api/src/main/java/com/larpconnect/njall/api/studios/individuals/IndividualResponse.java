package com.larpconnect.njall.api.studios.individuals;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Public HTTP representation of a tenanted studio individual. */
@Immutable
public record IndividualResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("name") String name,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("createdOn") Instant createdOn,
    @JsonProperty("updatedOn") Instant updatedOn) {}
