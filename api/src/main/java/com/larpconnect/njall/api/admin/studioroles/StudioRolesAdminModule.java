package com.larpconnect.njall.api.admin.studioroles;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring default studio role administration routes, actors, and factories. */
public final class StudioRolesAdminModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(StudioRoleAdminRoute.class);
    bind(StudioRoleAdminActorFactory.class).to(DefaultStudioRoleAdminActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<StudioRoleAdminCommand> provideStudioRoleAdminActor(
      ActorSystem<Void> system, StudioRoleAdminActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "studioRoleAdminActor", dispatcher);
  }
}
