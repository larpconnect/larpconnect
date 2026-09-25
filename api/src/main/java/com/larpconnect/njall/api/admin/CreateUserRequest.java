package com.larpconnect.njall.api.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new administrative user. */
public record CreateUserRequest(
    @JsonProperty("username") String username,
    @JsonProperty("status") @Nullable AdminUserStatus status,
    @JsonProperty("roles") @Nullable ImmutableList<String> roles) {

  public CreateUserRequest {
    roles = roles != null ? ImmutableList.copyOf(roles) : null;
  }

  /**
   * Pure factory method creating a CreateUserRequest.
   *
   * @param username The username.
   * @param status The initial user status.
   * @param roles Optional initial role names or IDs.
   * @return A new instance of CreateUserRequest.
   */
  public static CreateUserRequest of(
      String username, @Nullable AdminUserStatus status, @Nullable List<String> roles) {
    return new CreateUserRequest(
        username, status, roles != null ? ImmutableList.copyOf(roles) : null);
  }
}
