package com.larpconnect.njall.api.admin;

import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol for querying the health check actor. */
public sealed interface HealthCheckCommand {

  /** Command instructing the health check actor to evaluate all registered checks. */
  record CheckHealth(ActorRef<HealthCheckResponse> replyTo) implements HealthCheckCommand {}
}
