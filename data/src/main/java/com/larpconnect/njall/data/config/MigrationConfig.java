package com.larpconnect.njall.data.config;

import static java.util.Objects.requireNonNull;

import com.larpconnect.njall.common.config.ServerConfig;
import com.typesafe.config.Config;
import java.util.List;
import java.util.Map;

/**
 * Configuration for database migrations via Flyway.
 *
 * @param jdbcUrl The JDBC database connection URL.
 * @param username The administrative database username.
 * @param password The administrative database password.
 * @param schemas The list of database schemas managed by migration.
 * @param defaultSchema The default schema for Flyway metadata history.
 * @param placeholders Map of placeholder variables for script substitution.
 */
public record MigrationConfig(
    String jdbcUrl,
    String username,
    String password,
    List<String> schemas,
    String defaultSchema,
    Map<String, String> placeholders) {

  public MigrationConfig {
    requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
    requireNonNull(username, "username cannot be null");
    requireNonNull(password, "password cannot be null");
    requireNonNull(schemas, "schemas cannot be null");
    requireNonNull(defaultSchema, "defaultSchema cannot be null");
    requireNonNull(placeholders, "placeholders cannot be null");
    schemas = List.copyOf(schemas);
    placeholders = Map.copyOf(placeholders);
  }

  /**
   * Pure factory method for creating a {@link MigrationConfig}.
   *
   * @param jdbcUrl The JDBC database connection URL.
   * @param username The administrative database username.
   * @param password The administrative database password.
   * @param schemas The list of database schemas managed by migration.
   * @param defaultSchema The default schema for Flyway metadata history.
   * @param placeholders Map of placeholder variables for script substitution.
   * @return A new {@link MigrationConfig} instance.
   */
  public static MigrationConfig of(
      String jdbcUrl,
      String username,
      String password,
      List<String> schemas,
      String defaultSchema,
      Map<String, String> placeholders) {
    return new MigrationConfig(jdbcUrl, username, password, schemas, defaultSchema, placeholders);
  }

  /**
   * Extracts {@link MigrationConfig} from Typesafe {@link Config} and {@link ServerConfig}.
   *
   * @param config The Typesafe configuration tree.
   * @param serverConfig The server configuration containing seed values.
   * @return A new {@link MigrationConfig} instance populated from configuration.
   */
  public static MigrationConfig fromConfig(Config config, ServerConfig serverConfig) {
    requireNonNull(config, "config cannot be null");
    requireNonNull(serverConfig, "serverConfig cannot be null");

    var dbPath = "larpconnect.data.database.migration";
    var migrationConfig = config.getConfig(dbPath);

    var jdbcUrl = migrationConfig.getString("jdbc-url");
    var username = migrationConfig.getString("username");
    var password = migrationConfig.getString("password");
    var schemas = migrationConfig.getStringList("schemas");
    var defaultSchema = migrationConfig.getString("default-schema");

    var placeholders =
        Map.of(
            "server_name", serverConfig.name(),
            "primary_domain", serverConfig.primaryDomain(),
            "admin_contact", serverConfig.adminContact());

    return of(jdbcUrl, username, password, schemas, defaultSchema, placeholders);
  }
}
