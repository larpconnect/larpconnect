package com.larpconnect.njall.common.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/** Guice module providing Typesafe {@link Config} and {@link ServerConfig}. */
public final class ConfigModule extends AbstractModule {

  @Override
  protected void configure() {
    // Config providers are declared via @Provides methods
  }

  @Provides
  @Singleton
  Config provideConfig() {
    return ConfigFactory.load();
  }

  @Provides
  @Singleton
  ServerConfig provideServerConfig(Config config) {
    return ServerConfig.fromConfig(config);
  }
}
