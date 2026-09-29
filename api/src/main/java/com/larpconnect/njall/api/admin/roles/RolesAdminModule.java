package com.larpconnect.njall.api.admin.roles;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring role administration routes, actors, and factories. */
public final class RolesAdminModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RoleAdminRoute.class);
    bind(RoleAdminActorFactory.class).to(DefaultRoleAdminActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<RoleAdminCommand> provideRoleAdminActor(
      ActorSystem<Void> system, RoleAdminActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "roleAdminActor", dispatcher);
  }
}
