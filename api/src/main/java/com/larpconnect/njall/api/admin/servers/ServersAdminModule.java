package com.larpconnect.njall.api.admin.servers;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring server telemetry routes, actors, and factories. */
public final class ServersAdminModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ServersAdminRoute.class);
    bind(ServerAdminActorFactory.class).to(DefaultServerAdminActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<ServerAdminCommand> provideServerAdminActor(
      ActorSystem<Void> system, ServerAdminActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "serverAdminActor", dispatcher);
  }
}
