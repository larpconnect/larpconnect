package com.larpconnect.njall.server;

import static java.util.Objects.requireNonNull;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.api.ApiModule;
import com.larpconnect.njall.common.CommonModule;
import com.larpconnect.njall.data.DataModule;
import com.larpconnect.njall.server.cli.CliModule;
import com.larpconnect.njall.server.http.HttpServerModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/** Root application Guice module composing common, data, api, and server layers. */
public final class ServerModule extends AbstractModule {

  private final Config config;

  public ServerModule() {
    this(ConfigFactory.load());
  }

  public ServerModule(Config config) {
    this.config = requireNonNull(config, "config cannot be null");
  }

  @Override
  protected void configure() {
    install(new CommonModule(config));
    install(new DataModule());
    install(new ApiModule());
    install(new HttpServerModule());
    install(new CliModule());
  }

  @Provides
  @Singleton
  ActorSystem<Void> provideActorSystem() {
    return ActorSystem.create(Behaviors.empty(), "njall-server");
  }
}
