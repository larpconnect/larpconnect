package com.larpconnect.njall.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.api.ApiModule;
import com.larpconnect.njall.common.CommonModule;
import com.larpconnect.njall.data.DataModule;
import com.larpconnect.njall.server.http.HttpServerModule;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/** Root application Guice module composing common, data, api, and server layers. */
public final class ServerModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new CommonModule());
    install(new DataModule());
    install(new ApiModule());
    install(new HttpServerModule());
  }

  @Provides
  @Singleton
  ActorSystem<Void> provideActorSystem() {
    return ActorSystem.create(Behaviors.empty(), "njall-server");
  }
}
