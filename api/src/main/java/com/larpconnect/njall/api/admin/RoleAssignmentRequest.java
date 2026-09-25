package com.larpconnect.njall.api.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Request payload for role assignment custom methods (:addRole, :removeRole). */
@Immutable
public record RoleAssignmentRequest(
    @JsonProperty("roleId") Optional<UUID> roleId,
    @JsonProperty("roleName") Optional<String> roleName) {

  @SuppressWarnings("RedundantNullCheck")
  public RoleAssignmentRequest {
    roleId = roleId != null ? roleId : Optional.empty();
    roleName = roleName != null ? roleName : Optional.empty();
  }

  /**
   * Overloaded constructor accepting nullable raw values.
   *
   * @param roleId Optional role UUID.
   * @param roleName Optional role name.
   */
  @JsonCreator
  public RoleAssignmentRequest(
      @JsonProperty("roleId") @Nullable UUID roleId,
      @JsonProperty("roleName") @Nullable String roleName) {
    this(Optional.ofNullable(roleId), Optional.ofNullable(roleName));
  }

  /** Convenience constructor creating a request using roleId. */
  public RoleAssignmentRequest(UUID roleId) {
    this(Optional.ofNullable(roleId), Optional.empty());
  }

  /** Convenience constructor creating a request using roleName. */
  public RoleAssignmentRequest(String roleName) {
    this(Optional.empty(), Optional.ofNullable(roleName));
  }
}
