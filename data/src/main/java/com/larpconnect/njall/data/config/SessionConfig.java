package com.larpconnect.njall.data.config;

import static java.util.Objects.requireNonNull;

import com.typesafe.config.Config;
import org.jspecify.annotations.Nullable;

/**
 * Configuration profile for role-scoped database session factories.
 *
 * @param jdbcUrl The JDBC database connection URL.
 * @param username The database user name.
 * @param password The database user password, or null/blank if omitted.
 * @param minPoolSize Minimum connection pool size.
 * @param maxPoolSize Maximum connection pool size.
 * @param timeoutSeconds Connection timeout in seconds.
 */
public record SessionConfig(
    String jdbcUrl,
    String username,
    @Nullable String password,
    int minPoolSize,
    int maxPoolSize,
    int timeoutSeconds) {

  public SessionConfig {
    requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
    requireNonNull(username, "username cannot be null");
    if (minPoolSize < 1) {
      throw new IllegalArgumentException("minPoolSize must be at least 1");
    }
    if (maxPoolSize < minPoolSize) {
      throw new IllegalArgumentException(
          "maxPoolSize must be greater than or equal to minPoolSize");
    }
    if (timeoutSeconds < 1) {
      throw new IllegalArgumentException("timeoutSeconds must be at least 1");
    }
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
   * Pure factory method for creating a {@link SessionConfig}.
   *
   * @param jdbcUrl The JDBC connection URL.
   * @param username The database user name.
   * @param password The database user password, or null if omitted.
   * @param minPoolSize Minimum connection pool size.
   * @param maxPoolSize Maximum connection pool size.
   * @param timeoutSeconds Connection timeout in seconds.
   * @return A new {@link SessionConfig} instance.
   */
  public static SessionConfig of(
      String jdbcUrl,
      String username,
      @Nullable String password,
      int minPoolSize,
      int maxPoolSize,
      int timeoutSeconds) {
    return new SessionConfig(jdbcUrl, username, password, minPoolSize, maxPoolSize, timeoutSeconds);
  }

  /**
   * Extracts {@link SessionConfig} from Typesafe {@link Config} at the given path.
   *
   * @param config The Typesafe configuration tree.
   * @param path The path to the session configuration section.
   * @return A new {@link SessionConfig} instance populated from configuration.
   */
  public static SessionConfig fromConfig(Config config, String path) {
    requireNonNull(config, "config cannot be null");
    requireNonNull(path, "path cannot be null");

    var sessionSection = config.getConfig(path);
    var jdbcUrl = sessionSection.getString("jdbc-url");
    var username = sessionSection.getString("username");
    var password = sessionSection.hasPath("password") ? sessionSection.getString("password") : null;

    var poolSection = sessionSection.getConfig("pool");
    var minPoolSize = poolSection.getInt("min-size");
    var maxPoolSize = poolSection.getInt("max-size");
    var timeoutSeconds = poolSection.getInt("timeout-seconds");

    return of(jdbcUrl, username, password, minPoolSize, maxPoolSize, timeoutSeconds);
  }
}
