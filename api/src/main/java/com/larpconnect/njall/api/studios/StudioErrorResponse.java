package com.larpconnect.njall.api.studios;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

/** Structured AIP-193 error response representation for user-space studio endpoints. */
@Immutable
public record StudioErrorResponse(
    @JsonProperty("code") int code, @JsonProperty("message") String message) {}
