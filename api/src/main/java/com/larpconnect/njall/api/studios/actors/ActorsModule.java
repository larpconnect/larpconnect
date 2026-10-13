package com.larpconnect.njall.api.studios.actors;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio actor (agent) actors and routes. */
public final class ActorsModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AgentRoute.class).in(Scopes.SINGLETON);
    bind(AgentActorFactory.class).to(DefaultAgentActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<AgentCommand> provideAgentActor(
      ActorSystem<Void> system, AgentActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "agentActor", dispatcher);
  }
}
