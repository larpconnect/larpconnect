package com.larpconnect.njall.data.domain;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

/**
 * Immutable domain representation of an administrative role.
 *
 * @param id The role UUID.
 * @param roleName The unique canonical role name.
 */
public record AdminRole(UUID id, String roleName) implements DatabaseObject {

  public AdminRole {
    requireNonNull(id, "id cannot be null");
    requireNonNull(roleName, "roleName cannot be null");
  }

  public static AdminRole of(UUID id, String roleName) {
    return new AdminRole(id, roleName);
  }
}
