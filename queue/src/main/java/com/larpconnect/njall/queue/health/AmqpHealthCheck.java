package com.larpconnect.njall.queue.health;

import io.dropwizard.metrics5.health.HealthCheck;

/** Health check probe validating AMQP 1.0 broker connectivity with a 10-second Caffeine cache. */
public interface AmqpHealthCheck extends HealthCheck {

  @Override
  Result check();

  /** Invalidates all cached health probe results so the next check probes the broker directly. */
  void invalidateCache();
}
