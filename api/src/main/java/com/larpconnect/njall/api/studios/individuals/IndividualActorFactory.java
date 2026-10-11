package com.larpconnect.njall.api.studios.individuals;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link IndividualActor} behaviors. */
public interface IndividualActorFactory {

  /**
   * Creates the {@link Behavior} for {@link IndividualActor}.
   *
   * @return A typed behavior handling {@link IndividualCommand}.
   */
  Behavior<IndividualCommand> create();
}
