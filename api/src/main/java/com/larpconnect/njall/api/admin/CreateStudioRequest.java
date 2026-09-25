package com.larpconnect.njall.api.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

/** Request payload for creating a new studio. */
@Immutable
public record CreateStudioRequest(@JsonProperty("alias") String alias) {}
