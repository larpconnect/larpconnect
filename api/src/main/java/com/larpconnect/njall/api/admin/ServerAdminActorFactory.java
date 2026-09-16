package com.larpconnect.njall.api.admin;

import org.apache.pekko.actor.typed.Behavior;

/** Factory contract creating {@link ServerAdminActor} behaviors. */
public interface ServerAdminActorFactory {

  /**
   * Creates an Apache Pekko {@link Behavior} for the server admin actor.
   *
   * @return The configured behavior.
   */
  Behavior<ServerAdminCommand> create();
}
