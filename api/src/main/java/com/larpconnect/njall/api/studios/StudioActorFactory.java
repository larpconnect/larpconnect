package com.larpconnect.njall.api.studios;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link StudioActor} behaviors. */
public interface StudioActorFactory {

  /**
   * Creates the {@link Behavior} for {@link StudioActor}.
   *
   * @return A typed behavior handling {@link StudioCommand}.
   */
  Behavior<StudioCommand> create();
}
