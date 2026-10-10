package com.larpconnect.njall.api.studios.events;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio event actors and routes. */
public final class EventsModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EventsRoute.class).in(Scopes.SINGLETON);
    bind(EventActorFactory.class).to(DefaultEventActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<EventCommand> provideEventActor(
      ActorSystem<Void> system, EventActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "eventActor", dispatcher);
  }
}
