package com.larpconnect.njall.api.studios.games;

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

/** Guice module configuring tenanted studio game actors and routes. */
public final class GamesModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(GamesRoute.class).in(Scopes.SINGLETON);
    bind(GameActorFactory.class).to(DefaultGameActorFactory.class);
    Multibinder.newSetBinder(binder(), RouteProvider.class, StudioSubRoute.class)
        .addBinding()
        .to(GamesRoute.class);
  }

  @Provides
  @Singleton
  ActorRef<GameCommand> provideGameActor(
      ActorSystem<Void> system, GameActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "gameActor", dispatcher);
  }
}
