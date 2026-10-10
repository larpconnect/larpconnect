package com.larpconnect.njall.api.studios.tags;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Public metadata response for a studio hashtag link object. */
@Immutable
public record TagResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("tag") String tag,
    @JsonProperty("linkType") String linkType,
    @JsonProperty("url") String url,
    @JsonProperty("mediaType") String mediaType,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("createdOn") Instant createdOn,
    @JsonProperty("updatedOn") Instant updatedOn) {}
