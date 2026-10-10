package com.larpconnect.njall.api.studios.tags;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio hashtag actors and routes. */
public final class TagsModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(TagsRoute.class).in(Scopes.SINGLETON);
    bind(TagActorFactory.class).to(DefaultTagActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<TagCommand> provideTagActor(
      ActorSystem<Void> system, TagActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "tagActor", dispatcher);
  }
}
