package com.larpconnect.njall.api.admin.studios;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring studio administration routes, actors, and factories. */
public final class StudiosAdminModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(StudioAdminRoute.class);
    bind(StudioAdminActorFactory.class).to(DefaultStudioAdminActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<StudioAdminCommand> provideStudioAdminActor(
      ActorSystem<Void> system, StudioAdminActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "studioAdminActor", dispatcher);
  }
}
