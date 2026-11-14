package com.larpconnect.njall.api.studios.systems;

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

/** Guice module configuring user-space studio game system actors and routes. */
public final class SystemsModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SystemsRoute.class).in(Scopes.SINGLETON);
    bind(GameSystemActorFactory.class).to(DefaultGameSystemActorFactory.class);
    Multibinder.newSetBinder(binder(), RouteProvider.class, StudioSubRoute.class)
        .addBinding()
        .to(SystemsRoute.class);
  }

  @Provides
  @Singleton
  ActorRef<GameSystemCommand> provideGameSystemActor(
      ActorSystem<Void> system, GameSystemActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "gameSystemActor", dispatcher);
  }
}
