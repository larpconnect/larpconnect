package com.larpconnect.njall.api.admin;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.jspecify.annotations.Nullable;

/** Command protocol for administrative user operations. */
public sealed interface UserAdminCommand {

  /** Command instructing the actor to register a new user. */
  @Immutable
  record CreateUser(
      String username,
      AdminUserStatus status,
      ImmutableList<String> initialRoles,
      ActorRef<UserAdminResponse> replyTo)
      implements UserAdminCommand {

    @SuppressWarnings("RedundantNullCheck")
    public CreateUser {
      status = status != null ? status : AdminUserStatus.UNKNOWN;
      initialRoles = initialRoles != null ? ImmutableList.copyOf(initialRoles) : ImmutableList.of();
    }

    public CreateUser(
        String username,
        @Nullable AdminUserStatus status,
        @Nullable List<String> initialRoles,
        ActorRef<UserAdminResponse> replyTo) {
      this(
          username,
          status != null ? status : AdminUserStatus.UNKNOWN,
          initialRoles != null ? ImmutableList.copyOf(initialRoles) : ImmutableList.of(),
          replyTo);
    }
  }

  /** Command instructing the actor to list all users. */
  @Immutable
  record ListUsers(ActorRef<UserAdminResponse> replyTo) implements UserAdminCommand {}

  /** Command instructing the actor to find a user by ID. */
  @Immutable
  record GetUserById(UUID userId, ActorRef<UserAdminResponse> replyTo)
      implements UserAdminCommand {}

  /** Command instructing the actor to find a user by username. */
  @Immutable
  record GetUserByUsername(String username, ActorRef<UserAdminResponse> replyTo)
      implements UserAdminCommand {}

  /** Command instructing the actor to assign a role to a user. */
  @Immutable
  record AddRole(
      String userIdentifier,
      Optional<UUID> roleId,
      Optional<String> roleName,
      ActorRef<UserAdminResponse> replyTo)
      implements UserAdminCommand {

    public AddRole(
        String userIdentifier,
        @Nullable UUID roleId,
        @Nullable String roleName,
        ActorRef<UserAdminResponse> replyTo) {
      this(userIdentifier, Optional.ofNullable(roleId), Optional.ofNullable(roleName), replyTo);
    }
  }

  /** Command instructing the actor to remove a role from a user. */
  @Immutable
  record RemoveRole(
      String userIdentifier,
      Optional<UUID> roleId,
      Optional<String> roleName,
      ActorRef<UserAdminResponse> replyTo)
      implements UserAdminCommand {

    public RemoveRole(
        String userIdentifier,
        @Nullable UUID roleId,
        @Nullable String roleName,
        ActorRef<UserAdminResponse> replyTo) {
      this(userIdentifier, Optional.ofNullable(roleId), Optional.ofNullable(roleName), replyTo);
    }
  }
}
