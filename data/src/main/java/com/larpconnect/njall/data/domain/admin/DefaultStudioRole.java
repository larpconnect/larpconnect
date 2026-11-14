package com.larpconnect.njall.data.domain.admin;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.common.DatabaseObject;
import java.util.UUID;

/**
 * Immutable domain representation of a default studio role.
 *
 * @param id The role UUID identifier.
 * @param name The unique role name.
 */
@Immutable
public record DefaultStudioRole(UUID id, String name) implements DatabaseObject {}
