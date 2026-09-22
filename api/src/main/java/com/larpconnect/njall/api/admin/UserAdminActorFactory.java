package com.larpconnect.njall.api.admin;

import org.apache.pekko.actor.typed.Behavior;

/** Factory contract creating {@link UserAdminActor} behaviors. */
public interface UserAdminActorFactory {

  /**
   * Creates an Apache Pekko {@link Behavior} for the user admin actor.
   *
   * @return The configured behavior.
   */
  Behavior<UserAdminCommand> create();
}
