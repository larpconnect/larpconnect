package com.larpconnect.njall.data.config;

import com.typesafe.config.Config;
import org.jspecify.annotations.Nullable;

/**
 * Configuration profile for role-scoped database session factories.
 *
 * @param jdbcUrl The JDBC database connection URL.
 * @param username The database user name.
 * @param password The database user password, or null/blank if omitted.
 * @param trustAuth Whether passwordless trust authentication is explicitly permitted.
 * @param minPoolSize Minimum connection pool size.
 * @param maxPoolSize Maximum connection pool size.
 * @param timeoutSeconds Connection timeout in seconds.
 */
public record SessionConfig(
    String jdbcUrl,
    String username,
    @Nullable String password,
    boolean trustAuth,
    int minPoolSize,
    int maxPoolSize,
    int timeoutSeconds) {

  public SessionConfig {
    if (!trustAuth && (password == null || password.isBlank())) {
      throw new IllegalStateException(
          "Database password is required for profile with username '"
              + username
              + "' unless trust-auth is enabled");
    }
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
   * Pure factory method for creating a {@link SessionConfig} with explicit trust authentication.
   *
   * @param jdbcUrl The JDBC connection URL.
   * @param username The database user name.
   * @param password The database user password, or null if omitted.
   * @param trustAuth Whether passwordless trust authentication is enabled.
   * @param minPoolSize Minimum connection pool size.
   * @param maxPoolSize Maximum connection pool size.
   * @param timeoutSeconds Connection timeout in seconds.
   * @return A new {@link SessionConfig} instance.
   */
  public static SessionConfig of(
      String jdbcUrl,
      String username,
      @Nullable String password,
      boolean trustAuth,
      int minPoolSize,
      int maxPoolSize,
      int timeoutSeconds) {
    return new SessionConfig(
        jdbcUrl, username, password, trustAuth, minPoolSize, maxPoolSize, timeoutSeconds);
  }

  /**
   * Convenience factory method defaulting trust authentication to false.
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
    return of(jdbcUrl, username, password, false, minPoolSize, maxPoolSize, timeoutSeconds);
  }

  /**
   * Extracts {@link SessionConfig} from Typesafe {@link Config} at the given path.
   *
   * @param config The Typesafe configuration tree.
   * @param path The path to the session configuration section.
   * @return A new {@link SessionConfig} instance populated from configuration.
   */
  public static SessionConfig fromConfig(Config config, String path) {
    var sessionSection = config.getConfig(path);
    var jdbcUrl = sessionSection.getString("jdbc-url");
    var username = sessionSection.getString("username");
    var password = sessionSection.hasPath("password") ? sessionSection.getString("password") : null;

    var globalTrustKey = "larpconnect.data.database.trust-auth";
    var globalTrustAuth = config.hasPath(globalTrustKey) && config.getBoolean(globalTrustKey);
    var trustAuth =
        sessionSection.hasPath("trust-auth")
            ? sessionSection.getBoolean("trust-auth")
            : globalTrustAuth;

    var poolSection = sessionSection.getConfig("pool");
    var minPoolSize = poolSection.getInt("min-size");
    var maxPoolSize = poolSection.getInt("max-size");
    var timeoutSeconds = poolSection.getInt("timeout-seconds");

    return of(jdbcUrl, username, password, trustAuth, minPoolSize, maxPoolSize, timeoutSeconds);
  }
}
