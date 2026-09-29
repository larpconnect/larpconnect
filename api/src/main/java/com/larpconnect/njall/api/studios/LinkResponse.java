package com.larpconnect.njall.api.studios;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Public metadata response for an external studio link. */
@Immutable
public record LinkResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("linkType") String linkType,
    @JsonProperty("url") String url,
    @JsonProperty("mediaType") String mediaType,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("createdOn") Instant createdOn,
    @JsonProperty("updatedOn") Instant updatedOn) {}
