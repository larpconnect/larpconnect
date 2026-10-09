package com.larpconnect.njall.api.studios.addresses;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio address actors and routes. */
public final class AddressesModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AddressesRoute.class).in(Scopes.SINGLETON);
    bind(AddressActorFactory.class).to(DefaultAddressActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<AddressCommand> provideAddressActor(
      ActorSystem<Void> system, AddressActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "addressActor", dispatcher);
  }
}
