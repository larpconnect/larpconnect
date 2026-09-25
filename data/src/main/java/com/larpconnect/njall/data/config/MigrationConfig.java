package com.larpconnect.njall.data.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for database migrations via Flyway.
 *
 * @param jdbcUrl The JDBC database connection URL.
 * @param username The administrative database username.
 * @param password The administrative database password, or empty if omitted/blank.
 * @param trustAuth Whether passwordless trust authentication is explicitly permitted.
 * @param schemas The list of database schemas managed by migration.
 * @param defaultSchema The default schema for Flyway metadata history.
 * @param placeholders Map of placeholder variables for script substitution.
 */
@Immutable
public record MigrationConfig(
    String jdbcUrl,
    String username,
    Optional<String> password,
    boolean trustAuth,
    ImmutableList<String> schemas,
    String defaultSchema,
    ImmutableMap<String, String> placeholders) {

  public MigrationConfig {
    if (!trustAuth && password.filter(p -> !p.isBlank()).isEmpty()) {
      throw new IllegalStateException(
          "Database password is required for migration profile with username '"
              + username
              + "' unless trust-auth is enabled");
    }
    schemas = ImmutableList.copyOf(schemas);
    placeholders = ImmutableMap.copyOf(placeholders);
  }

  /** Overloaded constructor accepting an optional/nullable String password. */
  public MigrationConfig(
      String jdbcUrl,
      String username,
      @Nullable String password,
      boolean trustAuth,
      ImmutableList<String> schemas,
      String defaultSchema,
      ImmutableMap<String, String> placeholders) {
    this(
        jdbcUrl,
        username,
        Optional.ofNullable(password),
        trustAuth,
        schemas,
        defaultSchema,
        placeholders);
  }

  /**
   * Convenience constructor accepting standard List and Map collections.
   *
   * @param jdbcUrl The JDBC database connection URL.
   * @param username The administrative database username.
   * @param password The administrative database password.
   * @param trustAuth Whether passwordless trust authentication is explicitly permitted.
   * @param schemas The list of database schemas managed by migration.
   * @param defaultSchema The default schema for Flyway metadata history.
   * @param placeholders Map of placeholder variables for script substitution.
   */
  public MigrationConfig(
      String jdbcUrl,
      String username,
      @Nullable String password,
      boolean trustAuth,
      List<String> schemas,
      String defaultSchema,
      Map<String, String> placeholders) {
    this(
        jdbcUrl,
        username,
        Optional.ofNullable(password),
        trustAuth,
        ImmutableList.copyOf(schemas),
        defaultSchema,
        ImmutableMap.copyOf(placeholders));
  }

  /**
   * Convenience constructor defaulting trust authentication to false.
   *
   * @param jdbcUrl The JDBC database connection URL.
   * @param username The administrative database username.
   * @param password The administrative database password.
   * @param schemas The list of database schemas managed by migration.
   * @param defaultSchema The default schema for Flyway metadata history.
   * @param placeholders Map of placeholder variables for script substitution.
   */
  public MigrationConfig(
      String jdbcUrl,
      String username,
      @Nullable String password,
      List<String> schemas,
      String defaultSchema,
      Map<String, String> placeholders) {
    this(jdbcUrl, username, password, false, schemas, defaultSchema, placeholders);
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
