package com.larpconnect.njall.api.admin;

import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol for studio administration operations. */
public sealed interface StudioAdminCommand {

  /** Command instructing the actor to register a new studio with the given alias. */
  record CreateStudio(String alias, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}

  /** Command instructing the actor to list registered studios. */
  record ListStudios(boolean includeDeleted, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}

  /** Command instructing the actor to find a studio by its unique UUID. */
  record GetStudioById(UUID studioId, boolean includeDeleted, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}

  /** Command instructing the actor to find a studio by its natural alias. */
  record GetStudioByAlias(
      String alias, boolean includeDeleted, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}
}
