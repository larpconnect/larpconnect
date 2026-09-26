package com.larpconnect.njall.api.studios;

import com.google.errorprone.annotations.Immutable;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link StudioActor}. */
public sealed interface StudioCommand {

  /**
   * Retrieves studio information for the given studio identifier (UUID or alias).
   *
   * @param studioIdParam The public studio identifier or alias.
   * @param replyTo The recipient for the actor response.
   */
  @Immutable
  record GetStudio(String studioIdParam, ActorRef<StudioActorResponse> replyTo)
      implements StudioCommand {}
}
