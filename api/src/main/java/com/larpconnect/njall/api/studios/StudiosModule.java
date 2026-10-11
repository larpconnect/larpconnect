package com.larpconnect.njall.api.studios;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.addresses.AddressesModule;
import com.larpconnect.njall.api.studios.events.EventsModule;
import com.larpconnect.njall.api.studios.individuals.IndividualsModule;
import com.larpconnect.njall.api.studios.links.LinksModule;
import com.larpconnect.njall.api.studios.locations.LocationsModule;
import com.larpconnect.njall.api.studios.tags.TagsModule;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio routes and actor bindings. */
public final class StudiosModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new LinksModule());
    install(new AddressesModule());
    install(new LocationsModule());
    install(new TagsModule());
    install(new EventsModule());
    install(new IndividualsModule());

    bind(StudioSubRoutes.class).in(Scopes.SINGLETON);
    bind(StudiosRoute.class).in(Scopes.SINGLETON);
    Multibinder.newSetBinder(binder(), RouteProvider.class).addBinding().to(StudiosRoute.class);
    bind(StudioActorFactory.class).to(DefaultStudioActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<StudioCommand> provideStudioActor(
      ActorSystem<Void> system, StudioActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "studioActor", dispatcher);
  }
}
