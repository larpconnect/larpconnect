package com.larpconnect.njall.api.admin;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request payload for registering a new administrative role. */
public record CreateRoleRequest(@JsonProperty("roleName") String roleName) {

  public CreateRoleRequest {
    requireNonNull(roleName, "roleName cannot be null");
  }

  /**
   * Pure factory method creating a CreateRoleRequest.
   *
   * @param roleName The role name.
   * @return A new instance of CreateRoleRequest.
   */
  public static CreateRoleRequest of(String roleName) {
    return new CreateRoleRequest(roleName);
  }
}
