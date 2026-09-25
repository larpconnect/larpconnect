package com.larpconnect.njall.api.admin;

import static java.util.Objects.requireNonNull;

import com.larpconnect.njall.common.telemetry.TraceContext;
import java.util.Optional;
import org.apache.pekko.actor.typed.ActorRef;

/** Command protocol for querying the health check actor. */
public sealed interface HealthCheckCommand {

  /**
   * Command instructing the health check actor to evaluate all registered checks.
   *
   * @param replyTo the actor to reply to with health check status
   * @param traceContext optional telemetry trace context for MDC correlation
   */
  record CheckHealth(ActorRef<HealthCheckResponse> replyTo, Optional<TraceContext> traceContext)
      implements HealthCheckCommand {

    public CheckHealth {
      requireNonNull(replyTo, "replyTo cannot be null");
      requireNonNull(traceContext, "traceContext cannot be null");
    }

    public CheckHealth(ActorRef<HealthCheckResponse> replyTo) {
      this(replyTo, Optional.empty());
    }

    public CheckHealth(ActorRef<HealthCheckResponse> replyTo, TraceContext traceContext) {
      this(replyTo, Optional.ofNullable(traceContext));
    }
  }
}
