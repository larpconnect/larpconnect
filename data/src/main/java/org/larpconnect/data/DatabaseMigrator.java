package org.larpconnect.data;

/** Service interface for running database migrations. */
public interface DatabaseMigrator {
  /** Runs database schema migrations (using Flyway). */
  void migrate();
}
