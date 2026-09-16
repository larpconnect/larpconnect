package com.larpconnect.njall.data.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.config.ServerConfig;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.typesafe.config.Config;

/** Guice module providing database, migration, and session configurations. */
public final class DatabaseConfigModule extends AbstractModule {

  @Override
  protected void configure() {
    // Configuration providers declared via @Provides methods
  }

  @Provides
  @Singleton
  DatabaseConfig provideDatabaseConfig(Config config, ServerConfig serverConfig) {
    return DatabaseConfig.fromConfig(config, serverConfig);
  }

  @Provides
  @Singleton
  MigrationConfig provideMigrationConfig(DatabaseConfig databaseConfig) {
    return databaseConfig.migration();
  }

  @Provides
  @Singleton
  @NjallAdmin
  SessionConfig provideAdminSessionConfig(DatabaseConfig databaseConfig) {
    return databaseConfig.admin();
  }

  @Provides
  @Singleton
  @NjallUsers
  SessionConfig provideUsersSessionConfig(DatabaseConfig databaseConfig) {
    return databaseConfig.users();
  }
}
