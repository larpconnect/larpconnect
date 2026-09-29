package com.larpconnect.njall.api.studios;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link LinkActor} behaviors. */
public interface LinkActorFactory {

  /**
   * Creates the {@link Behavior} for {@link LinkActor}.
   *
   * @return A typed behavior handling {@link LinkCommand}.
   */
  Behavior<LinkCommand> create();
}
