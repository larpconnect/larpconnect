package org.larpconnect.data;

/** Abstract capability for running Flyway migrations. */
public interface FlywayMigrator {
  /**
   * Runs Flyway migration using the provided database configuration.
   *
   * @param config The database configuration to connect with.
   */
  void migrate(DatabaseConfiguration config);
}
