package com.larpconnect.njall.api.studios.games;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link GameActor} behaviors. */
public interface GameActorFactory {

  /**
   * Creates the typed {@link Behavior} handling {@link GameCommand} messages.
   *
   * @return A typed behavior for studio game operations.
   */
  Behavior<GameCommand> create();
}
