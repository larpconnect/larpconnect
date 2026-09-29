package com.larpconnect.njall.api.admin.roles;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;

/** Request payload for registering a new administrative role. */
@Immutable
public record CreateRoleRequest(@JsonProperty("roleName") String roleName) {}
