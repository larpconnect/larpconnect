package com.larpconnect.njall.api.admin;

import com.google.errorprone.annotations.Immutable;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol for administrative role operations. */
public sealed interface RoleAdminCommand {

  /** Command instructing the actor to register a new role. */
  @Immutable
  record CreateRole(String roleName, ActorRef<RoleAdminResponse> replyTo)
      implements RoleAdminCommand {}

  /** Command instructing the actor to list all roles. */
  @Immutable
  record ListRoles(ActorRef<RoleAdminResponse> replyTo) implements RoleAdminCommand {}

  /** Command instructing the actor to find a role by its unique UUID. */
  @Immutable
  record GetRoleById(UUID roleId, ActorRef<RoleAdminResponse> replyTo)
      implements RoleAdminCommand {}

  /** Command instructing the actor to find a role by its natural name. */
  @Immutable
  record GetRoleByName(String roleName, ActorRef<RoleAdminResponse> replyTo)
      implements RoleAdminCommand {}
}
