package com.larpconnect.njall.api.studios.collections;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio collection actors and routes. */
public final class CollectionsModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(CollectionsRoute.class).in(Scopes.SINGLETON);
    bind(CollectionActorFactory.class).to(DefaultCollectionActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<CollectionCommand> provideCollectionActor(
      ActorSystem<Void> system, CollectionActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "collectionActor", dispatcher);
  }
}
