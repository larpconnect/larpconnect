package com.larpconnect.njall.api.studios;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.UUID;

/** Public metadata response for a tenant studio. */
@Immutable
public record StudioResponse(
    @JsonProperty("studioId") UUID studioId,
    @JsonProperty("alias") String alias,
    @JsonProperty("name") String name) {}
