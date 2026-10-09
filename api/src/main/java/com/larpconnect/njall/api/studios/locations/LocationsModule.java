package com.larpconnect.njall.api.studios.locations;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio location actors and routes. */
public final class LocationsModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(LocationsRoute.class).in(Scopes.SINGLETON);
    bind(LocationActorFactory.class).to(DefaultLocationActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<LocationCommand> provideLocationActor(
      ActorSystem<Void> system, LocationActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "locationActor", dispatcher);
  }
}
