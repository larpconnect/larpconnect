package com.larpconnect.njall.api.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

/** Request payload for updating an existing default studio role. */
@Immutable
public record UpdateStudioRoleRequest(@JsonProperty("name") String name) {}
