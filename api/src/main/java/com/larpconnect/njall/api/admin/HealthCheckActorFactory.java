package com.larpconnect.njall.api.admin;

import org.apache.pekko.actor.typed.Behavior;

/** Factory contract creating {@link HealthCheckActor} behaviors. */
public interface HealthCheckActorFactory {

  /**
   * Creates an Apache Pekko {@link Behavior} for the health check actor.
   *
   * @return The configured behavior.
   */
  Behavior<HealthCheckCommand> create();
}
