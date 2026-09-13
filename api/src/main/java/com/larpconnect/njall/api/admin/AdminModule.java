package com.larpconnect.njall.api.admin;

import static java.util.Objects.requireNonNull;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.api.RouteProvider;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring administrative routes, health checks, and actor bindings. */
public final class AdminModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), HealthCheck.class).addBinding().to(PekkoHealthCheck.class);
    bind(AdminRoute.class).to(DefaultAdminRoute.class);
    Multibinder.newSetBinder(binder(), RouteProvider.class)
        .addBinding()
        .to(DefaultAdminRoute.class);
    bind(HealthCheckActorFactory.class).to(DefaultHealthCheckActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<HealthCheckCommand> provideHealthCheckActor(
      ActorSystem<Void> system, HealthCheckActorFactory factory) {
    requireNonNull(system, "system must not be null");
    requireNonNull(factory, "factory must not be null");
    return system.systemActorOf(factory.create(), "healthCheckActor", Props.empty());
  }
}
