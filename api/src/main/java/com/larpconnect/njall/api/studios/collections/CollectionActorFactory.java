package com.larpconnect.njall.api.studios.collections;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link CollectionActor} behaviors. */
public interface CollectionActorFactory {

  /**
   * Creates the {@link Behavior} for {@link CollectionActor}.
   *
   * @return A typed behavior handling {@link CollectionCommand}.
   */
  Behavior<CollectionCommand> create();
}
