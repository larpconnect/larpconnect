package com.larpconnect.njall.api.admin;

import org.apache.pekko.actor.typed.Behavior;

/** Factory contract creating {@link StudioAdminActor} behaviors. */
public interface StudioAdminActorFactory {

  /**
   * Creates an Apache Pekko {@link Behavior} for the studio admin actor.
   *
   * @return The configured behavior.
   */
  Behavior<StudioAdminCommand> create();
}
