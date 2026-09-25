package com.larpconnect.njall.data.config;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Configuration profile for role-scoped database session factories.
 *
 * @param jdbcUrl The JDBC database connection URL.
 * @param username The database user name.
 * @param password The database user password, or empty if omitted/blank.
 * @param trustAuth Whether passwordless trust authentication is explicitly permitted.
 * @param minPoolSize Minimum connection pool size.
 * @param maxPoolSize Maximum connection pool size.
 * @param timeoutSeconds Connection timeout in seconds.
 */
@Immutable
public record SessionConfig(
    String jdbcUrl,
    String username,
    Optional<String> password,
    boolean trustAuth,
    int minPoolSize,
    int maxPoolSize,
    int timeoutSeconds) {

  public SessionConfig {
    if (!trustAuth && password.filter(p -> !p.isBlank()).isEmpty()) {
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

  /** Overloaded constructor accepting an optional/nullable String password. */
  public SessionConfig(
      String jdbcUrl,
      String username,
      @Nullable String password,
      boolean trustAuth,
      int minPoolSize,
      int maxPoolSize,
      int timeoutSeconds) {
    this(
        jdbcUrl,
        username,
        Optional.ofNullable(password),
        trustAuth,
        minPoolSize,
        maxPoolSize,
        timeoutSeconds);
  }

  /**
   * Convenience constructor defaulting trust authentication to false.
   *
   * @param jdbcUrl The JDBC connection URL.
   * @param username The database user name.
   * @param password The database user password, or null if omitted.
   * @param minPoolSize Minimum connection pool size.
   * @param maxPoolSize Maximum connection pool size.
   * @param timeoutSeconds Connection timeout in seconds.
   */
  public SessionConfig(
      String jdbcUrl,
      String username,
      @Nullable String password,
      int minPoolSize,
      int maxPoolSize,
      int timeoutSeconds) {
    this(jdbcUrl, username, password, false, minPoolSize, maxPoolSize, timeoutSeconds);
  }

  /**
   * Returns true if a non-blank password is provided.
   *
   * @return true if password is present and non-blank.
   */
  public boolean hasPassword() {
    return password.filter(p -> !p.isBlank()).isPresent();
  }
}
