package com.larpconnect.njall.api.admin;

import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.util.List;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;
import org.jspecify.annotations.Nullable;

/** Command protocol for administrative user operations. */
public sealed interface UserAdminCommand {

  /** Command instructing the actor to register a new user. */
  record CreateUser(
      String username,
      @Nullable AdminUserStatus status,
      @Nullable List<String> initialRoles,
      ActorRef<UserAdminResponse> replyTo)
      implements UserAdminCommand {}

  /** Command instructing the actor to list all users. */
  record ListUsers(ActorRef<UserAdminResponse> replyTo) implements UserAdminCommand {}

  /** Command instructing the actor to find a user by ID. */
  record GetUserById(UUID userId, ActorRef<UserAdminResponse> replyTo)
      implements UserAdminCommand {}

  /** Command instructing the actor to find a user by username. */
  record GetUserByUsername(String username, ActorRef<UserAdminResponse> replyTo)
      implements UserAdminCommand {}

  /** Command instructing the actor to assign a role to a user. */
  record AddRole(
      String userIdentifier,
      @Nullable UUID roleId,
      @Nullable String roleName,
      ActorRef<UserAdminResponse> replyTo)
      implements UserAdminCommand {}

  /** Command instructing the actor to remove a role from a user. */
  record RemoveRole(
      String userIdentifier,
      @Nullable UUID roleId,
      @Nullable String roleName,
      ActorRef<UserAdminResponse> replyTo)
      implements UserAdminCommand {}
}
