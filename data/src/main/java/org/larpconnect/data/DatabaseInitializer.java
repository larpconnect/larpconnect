package org.larpconnect.data;

/** Service interface for initializing the database schema. */
public interface DatabaseInitializer {
  /** Runs database schema migrations (using Flyway). */
  void migrate();
}
