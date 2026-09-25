package com.larpconnect.njall.common.config;

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
    this.config = config;
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
    var host = config.getString("larpconnect.server.host");
    var port = config.getInt("larpconnect.server.port");
    var name =
        config.hasPath("larpconnect.server.name")
            ? config.getString("larpconnect.server.name")
            : ServerConfig.DEFAULT_NAME;
    var primaryDomain =
        config.hasPath("larpconnect.server.primary-domain")
            ? config.getString("larpconnect.server.primary-domain")
            : ServerConfig.DEFAULT_PRIMARY_DOMAIN;
    var adminContact =
        config.hasPath("larpconnect.server.admin-contact")
            ? config.getString("larpconnect.server.admin-contact")
            : ServerConfig.DEFAULT_ADMIN_CONTACT;
    return new ServerConfig(host, port, name, primaryDomain, adminContact);
  }
}
