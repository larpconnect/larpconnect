package com.larpconnect.njall.api.admin;

import com.larpconnect.njall.data.domain.DeletionFilter;
import java.util.UUID;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol for studio administration operations. */
public sealed interface StudioAdminCommand {

  /** Command instructing the actor to register a new studio with the given alias. */
  record CreateStudio(String alias, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}

  /** Command instructing the actor to list registered studios. */
  record ListStudios(DeletionFilter filter, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}

  /** Command instructing the actor to find a studio by its unique UUID. */
  record GetStudioById(UUID studioId, DeletionFilter filter, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}

  /** Command instructing the actor to find a studio by its natural alias. */
  record GetStudioByAlias(
      String alias, DeletionFilter filter, ActorRef<StudioAdminResponse> replyTo)
      implements StudioAdminCommand {}
}
