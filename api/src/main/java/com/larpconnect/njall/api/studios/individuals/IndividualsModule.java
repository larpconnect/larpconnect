package com.larpconnect.njall.api.studios.individuals;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioSubRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio individual actors and routes. */
public final class IndividualsModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(IndividualsRoute.class).in(Scopes.SINGLETON);
    bind(IndividualActorFactory.class).to(DefaultIndividualActorFactory.class);
    Multibinder.newSetBinder(binder(), RouteProvider.class, StudioSubRoute.class)
        .addBinding()
        .to(IndividualsRoute.class);
  }

  @Provides
  @Singleton
  ActorRef<IndividualCommand> provideIndividualActor(
      ActorSystem<Void> system, IndividualActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "individualActor", dispatcher);
  }
}
