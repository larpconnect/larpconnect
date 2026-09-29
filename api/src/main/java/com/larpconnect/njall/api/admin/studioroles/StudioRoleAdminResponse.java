package com.larpconnect.njall.api.admin.studioroles;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.DefaultStudioRole;

/** Response protocol emitted by the default studio role admin actor. */
public sealed interface StudioRoleAdminResponse {

  @Immutable
  record RoleSingle(DefaultStudioRole role) implements StudioRoleAdminResponse {}

  @Immutable
  record RoleList(ImmutableList<DefaultStudioRole> roles) implements StudioRoleAdminResponse {}

  @Immutable
  record NotFound(String message) implements StudioRoleAdminResponse {}

  @Immutable
  record Conflict(String message) implements StudioRoleAdminResponse {}

  @Immutable
  record BadRequest(String message) implements StudioRoleAdminResponse {}

  @Immutable
  record Failure(String message) implements StudioRoleAdminResponse {}

  static StudioRoleAdminResponse single(DefaultStudioRole role) {
    return new RoleSingle(role);
  }

  static StudioRoleAdminResponse list(ImmutableList<DefaultStudioRole> roles) {
    return new RoleList(roles);
  }

  static StudioRoleAdminResponse notFound(String message) {
    return new NotFound(message);
  }

  static StudioRoleAdminResponse conflict(String message) {
    return new Conflict(message);
  }

  static StudioRoleAdminResponse badRequest(String message) {
    return new BadRequest(message);
  }

  static StudioRoleAdminResponse failure(String message) {
    return new Failure(message);
  }
}
