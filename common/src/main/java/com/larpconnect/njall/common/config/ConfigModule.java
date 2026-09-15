package com.larpconnect.njall.common.config;

import static java.util.Objects.requireNonNull;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/** Guice module providing Typesafe {@link Config} and {@link ServerConfig}. */
public final class ConfigModule extends AbstractModule {

  private final Config config;

  public ConfigModule() {
    this(ConfigFactory.load());
  }

  public ConfigModule(Config config) {
    this.config = requireNonNull(config, "config cannot be null");
  }

  @Override
  protected void configure() {
    // Config providers are declared via @Provides methods
  }

  @Provides
  @Singleton
  Config provideConfig() {
    return config;
  }

  @Provides
  @Singleton
  ServerConfig provideServerConfig(Config config) {
    return ServerConfig.fromConfig(config);
  }
}
