package com.larpconnect.njall.api.admin.studioroles;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol for default studio role administrative operations. */
public sealed interface StudioRoleAdminCommand {

  @Immutable
  record CreateRole(String name, ActorRef<StudioRoleAdminResponse> replyTo)
      implements StudioRoleAdminCommand {}

  @Immutable
  record ListRoles(ActorRef<StudioRoleAdminResponse> replyTo) implements StudioRoleAdminCommand {}

  @Immutable
  record GetRoleById(UUID roleId, ActorRef<StudioRoleAdminResponse> replyTo)
      implements StudioRoleAdminCommand {}

  @Immutable
  record UpdateRole(
      UUID roleId,
      String name,
      Optional<String> updateMask,
      ActorRef<StudioRoleAdminResponse> replyTo)
      implements StudioRoleAdminCommand {}
}
