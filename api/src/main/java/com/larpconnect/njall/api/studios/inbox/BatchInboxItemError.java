package com.larpconnect.njall.api.studios.inbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

/** DTO representing an error for an individual activity within a batch submission. */
@Immutable
public record BatchInboxItemError(
    @JsonProperty("code") int code, @JsonProperty("message") String message) {}
