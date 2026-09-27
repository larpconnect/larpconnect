package com.larpconnect.njall.api.studios;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.StudioLookup;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol accepted by {@link StudioActor}. */
public sealed interface StudioCommand {

  /**
   * Retrieves studio information for the pre-resolved studio lookup.
   *
   * @param lookup The pre-resolved studio lookup.
   * @param replyTo The recipient for the actor response.
   */
  @Immutable
  record GetStudio(StudioLookup lookup, ActorRef<StudioActorResponse> replyTo)
      implements StudioCommand {}
}
