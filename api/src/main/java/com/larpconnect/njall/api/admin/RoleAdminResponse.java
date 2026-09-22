package com.larpconnect.njall.api.admin;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.AdminRole;

/** Response protocol emitted by the role admin actor. */
public sealed interface RoleAdminResponse {

  /** Represents a single retrieved or created role. */
  record RoleSingle(AdminRole role) implements RoleAdminResponse {}

  /** Represents a list of retrieved roles. */
  record RoleList(ImmutableList<AdminRole> roles) implements RoleAdminResponse {}

  /** Indicates that the requested role was not found. */
  record NotFound(String message) implements RoleAdminResponse {}

  /** Indicates that the role name already exists (conflict). */
  record Conflict(String message) implements RoleAdminResponse {}

  /** Indicates a malformed request or invalid role name format. */
  record BadRequest(String message) implements RoleAdminResponse {}

  /** Indicates an unexpected internal failure during the operation. */
  record Failure(String message) implements RoleAdminResponse {}

  static RoleAdminResponse single(AdminRole role) {
    return new RoleSingle(role);
  }

  static RoleAdminResponse list(ImmutableList<AdminRole> roles) {
    return new RoleList(roles);
  }

  static RoleAdminResponse notFound(String message) {
    return new NotFound(message);
  }

  static RoleAdminResponse conflict(String message) {
    return new Conflict(message);
  }

  static RoleAdminResponse badRequest(String message) {
    return new BadRequest(message);
  }

  static RoleAdminResponse failure(String message) {
    return new Failure(message);
  }
}
