package com.larpconnect.njall.api.studios.events;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link EventActor} behaviors. */
public interface EventActorFactory {

  /**
   * Creates the {@link Behavior} for {@link EventActor}.
   *
   * @return A typed behavior handling {@link EventCommand}.
   */
  Behavior<EventCommand> create();
}
