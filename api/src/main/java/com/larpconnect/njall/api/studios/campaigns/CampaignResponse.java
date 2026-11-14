package com.larpconnect.njall.api.studios.campaigns;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Public HTTP representation of a tenanted studio campaign. */
@Immutable
public record CampaignResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("generator") String generator,
    @JsonProperty("title") String title,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("system") Optional<String> system,
    @JsonProperty("createdOn") Instant createdOn,
    @JsonProperty("updatedOn") Instant updatedOn) {}
