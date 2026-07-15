package org.larpconnect.data;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concrete implementation of {@link DatabaseMigrator} using Flyway. */
@ThreadSafe
final class DefaultDatabaseMigrator implements DatabaseMigrator {
  private final Logger logger = LoggerFactory.getLogger(DefaultDatabaseMigrator.class);
  private final DatabaseConfiguration config;
  private final FlywayMigrator migrator;

  @Inject
  DefaultDatabaseMigrator(DatabaseConfiguration config, FlywayMigrator migrator) {
    this.config = config;
    this.migrator = migrator;
  }

  @Override
  public void migrate() {
    logger.info("Running database migration with Flyway...");
    migrator.migrate(config);
    logger.info("Database migration completed successfully.");
  }
}
