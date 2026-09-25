package com.larpconnect.njall.data.domain;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable domain representation of an administrative user.
 *
 * @param id The user UUID.
 * @param username The unique username.
 * @param status The user status.
 * @param createdAt Creation timestamp.
 * @param updatedAt Last update timestamp.
 * @param roles Immutable list of assigned roles.
 */
public record AdminUser(
    UUID id,
    String username,
    AdminUserStatus status,
    Instant createdAt,
    Instant updatedAt,
    ImmutableList<AdminRole> roles)
    implements DatabaseObject {

  public AdminUser {
    roles = ImmutableList.copyOf(roles);
  }

  public AdminUser(
      UUID id,
      String username,
      AdminUserStatus status,
      Instant createdAt,
      Instant updatedAt,
      List<AdminRole> roles) {
    this(id, username, status, createdAt, updatedAt, ImmutableList.copyOf(roles));
  }

  public static AdminUser of(
      UUID id,
      String username,
      AdminUserStatus status,
      Instant createdAt,
      Instant updatedAt,
      List<AdminRole> roles) {
    return new AdminUser(id, username, status, createdAt, updatedAt, roles);
  }
}
