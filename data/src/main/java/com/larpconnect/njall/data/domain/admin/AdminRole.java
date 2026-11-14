package com.larpconnect.njall.data.domain.admin;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.common.DatabaseObject;
import java.util.UUID;

/**
 * Immutable domain representation of an administrative role.
 *
 * @param id The role UUID.
 * @param roleName The unique canonical role name.
 */
@Immutable
public record AdminRole(UUID id, String roleName) implements DatabaseObject {}
