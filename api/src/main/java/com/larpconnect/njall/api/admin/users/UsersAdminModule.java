package com.larpconnect.njall.api.admin.users;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user administration routes, actors, and factories. */
public final class UsersAdminModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(UserAdminRoute.class);
    bind(UserAdminActorFactory.class).to(DefaultUserAdminActorFactory.class);
  }

  @Provides
  @Singleton
  ActorRef<UserAdminCommand> provideUserAdminActor(
      ActorSystem<Void> system, UserAdminActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "userAdminActor", dispatcher);
  }
}
