package com.larpconnect.njall.api.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Request payload for role assignment custom methods (:addRole, :removeRole). */
public record RoleAssignmentRequest(
    @JsonProperty("roleId") @Nullable UUID roleId,
    @JsonProperty("roleName") @Nullable String roleName) {

  /** Pure factory creating a request using roleId. */
  public static RoleAssignmentRequest ofRoleId(UUID roleId) {
    return new RoleAssignmentRequest(roleId, null);
  }

  /** Pure factory creating a request using roleName. */
  public static RoleAssignmentRequest ofRoleName(String roleName) {
    return new RoleAssignmentRequest(null, roleName);
  }
}
