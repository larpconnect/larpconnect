package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.util.UUID;

/**
 * Immutable domain representation of a default studio role.
 *
 * @param id The role UUID identifier.
 * @param name The unique role name.
 */
@Immutable
public record DefaultStudioRole(UUID id, String name) implements DatabaseObject {}
