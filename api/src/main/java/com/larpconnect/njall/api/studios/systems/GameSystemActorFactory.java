package com.larpconnect.njall.api.studios.systems;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link GameSystemActor} behaviors. */
public interface GameSystemActorFactory {

  /**
   * Creates the {@link Behavior} for {@link GameSystemActor}.
   *
   * @return A typed behavior handling {@link GameSystemCommand}.
   */
  Behavior<GameSystemCommand> create();
}
