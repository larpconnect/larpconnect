package com.larpconnect.njall.api.admin.health;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.common.telemetry.ApiCall;
import io.dropwizard.metrics5.health.HealthCheck;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring health check routes, actors, and Dropwizard health checks. */
public final class HealthAdminModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), HealthCheck.class).addBinding().to(PekkoHealthCheck.class);
    bind(HealthAdminRoute.class);
    bind(HealthCheckActorFactory.class).to(DefaultHealthCheckActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<ApiCall<HealthCheckCommand>> provideHealthCheckActor(
      ActorSystem<Void> system, HealthCheckActorFactory factory) {
    return system.systemActorOf(factory.create(), "healthCheckActor", Props.empty());
  }
}
