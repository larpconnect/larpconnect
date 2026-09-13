package com.larpconnect.njall.api.admin;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring administrative routes, health checks, and actor bindings. */
public final class AdminModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), HealthCheck.class).addBinding().to(PekkoHealthCheck.class);
    bind(AdminRoute.class).to(DefaultAdminRoute.class);
  }

  @Provides
  @Singleton
  ActorRef<HealthCheckCommand> provideHealthCheckActor(
      ActorSystem<Void> system, HealthCheckRegistry registry) {
    return system.systemActorOf(
        HealthCheckActor.create(registry), "healthCheckActor", Props.empty());
  }
}
