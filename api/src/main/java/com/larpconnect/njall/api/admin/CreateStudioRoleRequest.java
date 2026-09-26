package com.larpconnect.njall.api.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

/** Request payload for creating a new default studio role. */
@Immutable
public record CreateStudioRoleRequest(@JsonProperty("name") String name) {}
