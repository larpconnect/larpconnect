package com.larpconnect.njall.data.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.config.ServerConfig;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.typesafe.config.Config;
import java.util.Map;

/** Guice module providing database, migration, and session configurations. */
public final class DatabaseConfigModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SessionConfigFactory.class).to(DefaultSessionConfigFactory.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  DatabaseConfig provideDatabaseConfig(
      MigrationConfig migrationConfig,
      @NjallAdmin SessionConfig adminConfig,
      @NjallUsers SessionConfig usersConfig) {
    return new DatabaseConfig(migrationConfig, adminConfig, usersConfig);
  }

  @Provides
  @Singleton
  MigrationConfig provideMigrationConfig(Config config, ServerConfig serverConfig) {
    var dbPath = "larpconnect.data.database.migration";
    var migrationConfig = config.getConfig(dbPath);

    var jdbcUrl = migrationConfig.getString("jdbc-url");
    var username = migrationConfig.getString("username");
    var password =
        migrationConfig.hasPath("password") ? migrationConfig.getString("password") : null;

    var globalTrustKey = "larpconnect.data.database.trust-auth";
    var globalTrustAuth = config.hasPath(globalTrustKey) && config.getBoolean(globalTrustKey);
    var trustAuth =
        migrationConfig.hasPath("trust-auth")
            ? migrationConfig.getBoolean("trust-auth")
            : globalTrustAuth;

    var schemas = migrationConfig.getStringList("schemas");
    var defaultSchema = migrationConfig.getString("default-schema");

    var placeholders =
        Map.of(
            "server_name", serverConfig.name(),
            "primary_domain", serverConfig.primaryDomain(),
            "admin_contact", serverConfig.adminContact());

    return new MigrationConfig(
        jdbcUrl, username, password, trustAuth, schemas, defaultSchema, placeholders);
  }

  @Provides
  @Singleton
  @NjallAdmin
  SessionConfig provideAdminSessionConfig(SessionConfigFactory sessionConfigFactory) {
    return sessionConfigFactory.create("larpconnect.data.database.admin");
  }

  @Provides
  @Singleton
  @NjallUsers
  SessionConfig provideUsersSessionConfig(SessionConfigFactory sessionConfigFactory) {
    return sessionConfigFactory.create("larpconnect.data.database.users");
  }
}
