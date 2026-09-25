package com.larpconnect.njall.api.admin;

import com.google.errorprone.annotations.Immutable;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol for querying the health check actor. */
@Immutable
public sealed interface HealthCheckCommand {

  /**
   * Command instructing the health check actor to evaluate all registered checks.
   *
   * @param replyTo the actor to reply to with health check status
   */
  @Immutable
  record CheckHealth(ActorRef<HealthCheckResponse> replyTo) implements HealthCheckCommand {}
}
