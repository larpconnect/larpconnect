package com.larpconnect.njall.api.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

/** Structured AIP-193 error response representation. */
@Immutable
public record AdminErrorResponse(
    @JsonProperty("code") int code, @JsonProperty("message") String message) {}
