package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.util.UUID;

/**
 * Immutable domain representation of an administrative role.
 *
 * @param id The role UUID.
 * @param roleName The unique canonical role name.
 */
@Immutable
public record AdminRole(UUID id, String roleName) implements DatabaseObject {}
