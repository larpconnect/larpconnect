package com.larpconnect.njall.api.studios.actors;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link AgentActor} behaviors. */
public interface AgentActorFactory {

  /**
   * Creates the {@link Behavior} for {@link AgentActor}.
   *
   * @return A typed behavior handling {@link AgentCommand}.
   */
  Behavior<AgentCommand> create();
}
