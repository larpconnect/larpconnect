package com.larpconnect.njall.api.studios.tags;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link TagActor} behaviors. */
public interface TagActorFactory {

  /**
   * Creates the {@link Behavior} for {@link TagActor}.
   *
   * @return A typed behavior handling {@link TagCommand}.
   */
  Behavior<TagCommand> create();
}
