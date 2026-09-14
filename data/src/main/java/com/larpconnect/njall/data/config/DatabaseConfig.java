package com.larpconnect.njall.data.config;

import static java.util.Objects.requireNonNull;

import com.larpconnect.njall.common.config.ServerConfig;
import com.typesafe.config.Config;

/**
 * Root database configuration composite for Project Njall persistence.
 *
 * @param migration Migration-specific configuration profile.
 */
public record DatabaseConfig(MigrationConfig migration) {

  public DatabaseConfig {
    requireNonNull(migration, "migration cannot be null");
  }

  /**
   * Pure factory method for creating a {@link DatabaseConfig}.
   *
   * @param migration Migration configuration profile.
   * @return A new {@link DatabaseConfig} instance.
   */
  public static DatabaseConfig of(MigrationConfig migration) {
    return new DatabaseConfig(migration);
  }

  /**
   * Extracts {@link DatabaseConfig} from Typesafe {@link Config} and {@link ServerConfig}.
   *
   * @param config The Typesafe configuration tree.
   * @param serverConfig The server configuration containing seed values.
   * @return A new {@link DatabaseConfig} instance.
   */
  public static DatabaseConfig fromConfig(Config config, ServerConfig serverConfig) {
    requireNonNull(config, "config cannot be null");
    requireNonNull(serverConfig, "serverConfig cannot be null");
    var migration = MigrationConfig.fromConfig(config, serverConfig);
    return of(migration);
  }
}
