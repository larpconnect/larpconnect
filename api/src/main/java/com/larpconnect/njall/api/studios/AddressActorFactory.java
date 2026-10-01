package com.larpconnect.njall.api.studios;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link AddressActor} behaviors. */
public interface AddressActorFactory {

  /**
   * Creates the {@link Behavior} for {@link AddressActor}.
   *
   * @return A typed behavior handling {@link AddressCommand}.
   */
  Behavior<AddressCommand> create();
}
