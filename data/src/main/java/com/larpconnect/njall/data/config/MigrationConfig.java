package com.larpconnect.njall.data.config;

import com.larpconnect.njall.common.config.ServerConfig;
import com.typesafe.config.Config;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for database migrations via Flyway.
 *
 * @param jdbcUrl The JDBC database connection URL.
 * @param username The administrative database username.
 * @param password The administrative database password, or null/blank if trust auth is enabled.
 * @param trustAuth Whether passwordless trust authentication is explicitly permitted.
 * @param schemas The list of database schemas managed by migration.
 * @param defaultSchema The default schema for Flyway metadata history.
 * @param placeholders Map of placeholder variables for script substitution.
 */
public record MigrationConfig(
    String jdbcUrl,
    String username,
    @Nullable String password,
    boolean trustAuth,
    List<String> schemas,
    String defaultSchema,
    Map<String, String> placeholders) {

  public MigrationConfig {
    if (!trustAuth && (password == null || password.isBlank())) {
      throw new IllegalStateException(
          "Database password is required for migration profile with username '"
              + username
              + "' unless trust-auth is enabled");
    }
    schemas = List.copyOf(schemas);
    placeholders = Map.copyOf(placeholders);
  }

  /**
   * Returns true if a non-blank password is provided.
   *
   * @return true if password is present and non-blank.
   */
  public boolean hasPassword() {
    return password != null && !password.isBlank();
  }

  /**
   * Pure factory method for creating a {@link MigrationConfig} with explicit trust authentication.
   *
   * @param jdbcUrl The JDBC database connection URL.
   * @param username The administrative database username.
   * @param password The administrative database password.
   * @param trustAuth Whether passwordless trust authentication is enabled.
   * @param schemas The list of database schemas managed by migration.
   * @param defaultSchema The default schema for Flyway metadata history.
   * @param placeholders Map of placeholder variables for script substitution.
   * @return A new {@link MigrationConfig} instance.
   */
  public static MigrationConfig of(
      String jdbcUrl,
      String username,
      @Nullable String password,
      boolean trustAuth,
      List<String> schemas,
      String defaultSchema,
      Map<String, String> placeholders) {
    return new MigrationConfig(
        jdbcUrl, username, password, trustAuth, schemas, defaultSchema, placeholders);
  }

  /**
   * Convenience factory method defaulting trust authentication to false.
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
      @Nullable String password,
      List<String> schemas,
      String defaultSchema,
      Map<String, String> placeholders) {
    return of(jdbcUrl, username, password, false, schemas, defaultSchema, placeholders);
  }

  /**
   * Extracts {@link MigrationConfig} from Typesafe {@link Config} and {@link ServerConfig}.
   *
   * @param config The Typesafe configuration tree.
   * @param serverConfig The server configuration containing seed values.
   * @return A new {@link MigrationConfig} instance populated from configuration.
   */
  public static MigrationConfig fromConfig(Config config, ServerConfig serverConfig) {
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

    return of(jdbcUrl, username, password, trustAuth, schemas, defaultSchema, placeholders);
  }
}
