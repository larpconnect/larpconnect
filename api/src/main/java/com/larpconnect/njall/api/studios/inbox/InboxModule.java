package com.larpconnect.njall.api.studios.inbox;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Routers;

/** Public Guice module configuring FeatherPub inbox components, actors, and routes. */
public final class InboxModule extends AbstractModule {

  private static final int POOL_SIZE = 16;

  @Override
  protected void configure() {
    bind(InboxRoute.class).to(DefaultInboxRoute.class).in(Scopes.SINGLETON);
    bind(InboxDeduplicationCache.class)
        .to(DefaultInboxDeduplicationCache.class)
        .in(Scopes.SINGLETON);
    bind(ContentDigestVerifier.class).to(DefaultContentDigestVerifier.class).in(Scopes.SINGLETON);
    bind(FeatherPubActivityValidator.class)
        .to(DefaultFeatherPubActivityValidator.class)
        .in(Scopes.SINGLETON);
    bind(LocalActorRecipientExtractor.class)
        .to(DefaultLocalActorRecipientExtractor.class)
        .in(Scopes.SINGLETON);
    bind(InboxActorFactory.class).to(DefaultInboxActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<InboxCommand> provideInboxActor(
      ActorSystem<Void> system, InboxActorFactory factory, @Blocking Props dispatcher) {
    var supervised = Behaviors.supervise(factory.create()).onFailure(SupervisorStrategy.restart());
    var pool =
        Routers.pool(POOL_SIZE, supervised).withRoundRobinRouting().withRouteeProps(dispatcher);
    return system.systemActorOf(pool, "inboxActor", dispatcher);
  }
}
