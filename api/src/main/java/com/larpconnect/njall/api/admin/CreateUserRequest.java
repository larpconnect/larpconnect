package com.larpconnect.njall.api.admin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Request payload for creating a new administrative user. */
@Immutable
public record CreateUserRequest(
    @JsonProperty("username") String username,
    @JsonProperty("status") AdminUserStatus status,
    @JsonProperty("roles") ImmutableList<String> roles) {

  @SuppressWarnings("RedundantNullCheck")
  public CreateUserRequest {
    status = status != null ? status : AdminUserStatus.UNKNOWN;
    roles = roles != null ? ImmutableList.copyOf(roles) : ImmutableList.of();
  }

  /**
   * Overloaded constructor accepting a generic List of roles and nullable status.
   *
   * @param username The username.
   * @param status The initial user status.
   * @param roles Optional initial role names or IDs.
   */
  @JsonCreator
  public CreateUserRequest(
      @JsonProperty("username") String username,
      @JsonProperty("status") @Nullable AdminUserStatus status,
      @JsonProperty("roles") @Nullable List<String> roles) {
    this(
        username,
        status != null ? status : AdminUserStatus.UNKNOWN,
        roles != null ? ImmutableList.copyOf(roles) : ImmutableList.of());
  }
}
