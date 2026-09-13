package com.larpconnect.njall.api.admin;

import static java.util.Objects.requireNonNull;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Inject;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/** Default implementation of {@link HealthCheckActorFactory} creating {@link HealthCheckActor}. */
final class DefaultHealthCheckActorFactory implements HealthCheckActorFactory {

  private final HealthCheckRegistry registry;

  @Inject
  DefaultHealthCheckActorFactory(HealthCheckRegistry registry) {
    this.registry = requireNonNull(registry, "registry must not be null");
  }

  @Override
  public Behavior<HealthCheckCommand> create() {
    return Behaviors.setup(this::createActor);
  }

  private HealthCheckActor createActor(ActorContext<HealthCheckCommand> context) {
    return new HealthCheckActor(context, registry);
  }
}
