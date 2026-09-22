package com.larpconnect.njall.api.admin;

import org.apache.pekko.actor.typed.Behavior;

/** Factory contract creating {@link RoleAdminActor} behaviors. */
public interface RoleAdminActorFactory {

  /**
   * Creates an Apache Pekko {@link Behavior} for the role admin actor.
   *
   * @return The configured behavior.
   */
  Behavior<RoleAdminCommand> create();
}
