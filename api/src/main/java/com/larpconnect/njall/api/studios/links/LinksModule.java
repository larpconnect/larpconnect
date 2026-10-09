package com.larpconnect.njall.api.studios.links;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio link actors and routes. */
public final class LinksModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(LinksRoute.class).in(Scopes.SINGLETON);
    bind(LinkActorFactory.class).to(DefaultLinkActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<LinkCommand> provideLinkActor(
      ActorSystem<Void> system, LinkActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "linkActor", dispatcher);
  }
}
