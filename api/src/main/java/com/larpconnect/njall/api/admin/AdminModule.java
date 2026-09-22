package com.larpconnect.njall.api.admin;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.api.RouteProvider;
import com.larpconnect.njall.common.annotation.Blocking;
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
    bind(ServerAdminActorFactory.class).to(DefaultServerAdminActorFactory.class);
    bind(StudioAdminActorFactory.class).to(DefaultStudioAdminActorFactory.class);
    bind(RoleAdminActorFactory.class).to(DefaultRoleAdminActorFactory.class);
    bind(UserAdminActorFactory.class).to(DefaultUserAdminActorFactory.class);
  }

  @Provides
  @Singleton
  ObjectMapper provideObjectMapper() {
    return JsonMapper.builder()
        .findAndAddModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();
  }

  @Provides
  @Singleton
  ActorRef<HealthCheckCommand> provideHealthCheckActor(
      ActorSystem<Void> system, HealthCheckActorFactory factory) {
    return system.systemActorOf(factory.create(), "healthCheckActor", Props.empty());
  }

  @Provides
  @Singleton
  ActorRef<ServerAdminCommand> provideServerAdminActor(
      ActorSystem<Void> system, ServerAdminActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "serverAdminActor", dispatcher);
  }

  @Provides
  @Singleton
  ActorRef<StudioAdminCommand> provideStudioAdminActor(
      ActorSystem<Void> system, StudioAdminActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "studioAdminActor", dispatcher);
  }

  @Provides
  @Singleton
  ActorRef<RoleAdminCommand> provideRoleAdminActor(
      ActorSystem<Void> system, RoleAdminActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "roleAdminActor", dispatcher);
  }

  @Provides
  @Singleton
  ActorRef<UserAdminCommand> provideUserAdminActor(
      ActorSystem<Void> system, UserAdminActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "userAdminActor", dispatcher);
  }
}
