package com.larpconnect.njall.api.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request payload for registering a new administrative role. */
public record CreateRoleRequest(@JsonProperty("roleName") String roleName) {

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
