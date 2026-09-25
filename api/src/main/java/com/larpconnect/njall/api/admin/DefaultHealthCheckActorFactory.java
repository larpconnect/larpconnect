package com.larpconnect.njall.api.admin;

import com.google.inject.Inject;
import com.larpconnect.njall.common.telemetry.ApiCall;
import io.dropwizard.metrics5.health.HealthCheckRegistry;
import org.apache.pekko.actor.typed.Behavior;

/** Default implementation of {@link HealthCheckActorFactory} creating {@link HealthCheckActor}. */
final class DefaultHealthCheckActorFactory implements HealthCheckActorFactory {

  private final HealthCheckRegistry registry;

  @Inject
  DefaultHealthCheckActorFactory(HealthCheckRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Behavior<ApiCall<HealthCheckCommand>> create() {
    return HealthCheckActor.create(registry);
  }
}
