package com.larpconnect.njall.api.studios.locations;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link LocationActor} behaviors. */
public interface LocationActorFactory {

  /**
   * Creates the {@link Behavior} for {@link LocationActor}.
   *
   * @return A typed behavior handling {@link LocationCommand}.
   */
  Behavior<LocationCommand> create();
}
