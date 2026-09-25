package com.larpconnect.njall.api.admin;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.AdminUser;

/** Response protocol emitted by the user admin actor. */
public sealed interface UserAdminResponse {

  /** Represents a single retrieved, created, or updated user. */
  @Immutable
  record UserSingle(AdminUser user) implements UserAdminResponse {}

  /** Represents a list of retrieved users. */
  @Immutable
  record UserList(ImmutableList<AdminUser> users) implements UserAdminResponse {}

  /** Indicates that the requested user was not found. */
  @Immutable
  record NotFound(String message) implements UserAdminResponse {}

  /** Indicates that the username already exists (conflict). */
  @Immutable
  record Conflict(String message) implements UserAdminResponse {}

  /** Indicates a malformed request, missing parameters, or invalid role. */
  @Immutable
  record BadRequest(String message) implements UserAdminResponse {}

  /** Indicates an unexpected internal failure during the operation. */
  @Immutable
  record Failure(String message) implements UserAdminResponse {}

  static UserAdminResponse single(AdminUser user) {
    return new UserSingle(user);
  }

  static UserAdminResponse list(ImmutableList<AdminUser> users) {
    return new UserList(users);
  }

  static UserAdminResponse notFound(String message) {
    return new NotFound(message);
  }

  static UserAdminResponse conflict(String message) {
    return new Conflict(message);
  }

  static UserAdminResponse badRequest(String message) {
    return new BadRequest(message);
  }

  static UserAdminResponse failure(String message) {
    return new Failure(message);
  }
}
