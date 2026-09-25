package com.larpconnect.njall.data.config;

import com.larpconnect.njall.common.config.ServerConfig;
import com.typesafe.config.Config;

/**
 * Root database configuration composite for Project Njall persistence.
 *
 * @param migration Migration-specific configuration profile.
 * @param admin Administrator database session configuration profile.
 * @param users Tenanted users database session configuration profile.
 */
public record DatabaseConfig(MigrationConfig migration, SessionConfig admin, SessionConfig users) {

  /**
   * Pure factory method for creating a {@link DatabaseConfig}.
   *
   * @param migration Migration configuration profile.
   * @param admin Administrator session configuration profile.
   * @param users Tenanted users session configuration profile.
   * @return A new {@link DatabaseConfig} instance.
   */
  public static DatabaseConfig of(
      MigrationConfig migration, SessionConfig admin, SessionConfig users) {
    return new DatabaseConfig(migration, admin, users);
  }

  /**
   * Extracts {@link DatabaseConfig} from Typesafe {@link Config} and {@link ServerConfig}.
   *
   * @param config The Typesafe configuration tree.
   * @param serverConfig The server configuration containing seed values.
   * @return A new {@link DatabaseConfig} instance.
   */
  public static DatabaseConfig fromConfig(Config config, ServerConfig serverConfig) {
    var migration = MigrationConfig.fromConfig(config, serverConfig);
    var admin = SessionConfig.fromConfig(config, "larpconnect.data.database.admin");
    var users = SessionConfig.fromConfig(config, "larpconnect.data.database.users");
    return of(migration, admin, users);
  }
}
