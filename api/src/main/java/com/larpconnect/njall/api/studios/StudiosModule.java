package com.larpconnect.njall.api.studios;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio routes and actor bindings. */
public final class StudiosModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(StudiosRoute.class).in(Scopes.SINGLETON);
    Multibinder.newSetBinder(binder(), RouteProvider.class).addBinding().to(StudiosRoute.class);
    bind(StudioActorFactory.class).to(DefaultStudioActorFactory.class);

    bind(LinksRoute.class).in(Scopes.SINGLETON);
    Multibinder.newSetBinder(binder(), RouteProvider.class).addBinding().to(LinksRoute.class);
    bind(LinkActorFactory.class).to(DefaultLinkActorFactory.class);

    bind(LocationsRoute.class).in(Scopes.SINGLETON);
    Multibinder.newSetBinder(binder(), RouteProvider.class).addBinding().to(LocationsRoute.class);
    bind(LocationActorFactory.class).to(DefaultLocationActorFactory.class);
    bind(AddressActorFactory.class).to(DefaultAddressActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<StudioCommand> provideStudioActor(
      ActorSystem<Void> system, StudioActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "studioActor", dispatcher);
  }

  @Provides
  @Singleton
  ActorRef<LinkCommand> provideLinkActor(
      ActorSystem<Void> system, LinkActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "linkActor", dispatcher);
  }

  @Provides
  @Singleton
  ActorRef<LocationCommand> provideLocationActor(
      ActorSystem<Void> system, LocationActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "locationActor", dispatcher);
  }

  @Provides
  @Singleton
  ActorRef<AddressCommand> provideAddressActor(
      ActorSystem<Void> system, AddressActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "addressActor", dispatcher);
  }
}
