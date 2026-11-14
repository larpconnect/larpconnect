package com.larpconnect.njall.api.studios.inbox;

import org.apache.pekko.actor.typed.Behavior;

/** Factory interface for creating typed {@link InboxActor} behaviors. */
public interface InboxActorFactory {

  /** Creates a new {@link Behavior} for processing {@link InboxCommand} messages. */
  Behavior<InboxCommand> create();
}
