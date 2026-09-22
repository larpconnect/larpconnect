package com.larpconnect.njall.api.admin;

import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol for administrative role operations. */
public sealed interface RoleAdminCommand {

  /** Command instructing the actor to register a new role. */
  record CreateRole(String roleName, ActorRef<RoleAdminResponse> replyTo)
      implements RoleAdminCommand {}

  /** Command instructing the actor to list all roles. */
  record ListRoles(ActorRef<RoleAdminResponse> replyTo) implements RoleAdminCommand {}

  /** Command instructing the actor to find a role by its unique UUID. */
  record GetRoleById(UUID roleId, ActorRef<RoleAdminResponse> replyTo)
      implements RoleAdminCommand {}

  /** Command instructing the actor to find a role by its natural name. */
  record GetRoleByName(String roleName, ActorRef<RoleAdminResponse> replyTo)
      implements RoleAdminCommand {}
}
