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
  DatabaseConfig provideDatabaseConfig(
      MigrationConfig migrationConfig,
      @NjallAdmin SessionConfig adminConfig,
      @NjallUsers SessionConfig usersConfig) {
    return DatabaseConfig.of(migrationConfig, adminConfig, usersConfig);
  }

  @Provides
  @Singleton
  MigrationConfig provideMigrationConfig(Config config, ServerConfig serverConfig) {
    return MigrationConfig.fromConfig(config, serverConfig);
  }

  @Provides
  @Singleton
  @NjallAdmin
  SessionConfig provideAdminSessionConfig(Config config) {
    return SessionConfig.fromConfig(config, "larpconnect.data.database.admin");
  }

  @Provides
  @Singleton
  @NjallUsers
  SessionConfig provideUsersSessionConfig(Config config) {
    return SessionConfig.fromConfig(config, "larpconnect.data.database.users");
  }
}
