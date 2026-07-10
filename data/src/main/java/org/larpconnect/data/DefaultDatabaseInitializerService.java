package org.larpconnect.data;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Concrete implementation of {@link DatabaseInitializerService} using Flyway. */
@ThreadSafe
final class DefaultDatabaseInitializerService extends AbstractIdleService
    implements DatabaseInitializerService {
  private final Logger logger = LoggerFactory.getLogger(DefaultDatabaseInitializerService.class);
  private final DatabaseConfiguration config;
  private final FlywayMigrator migrator;

  @Inject
  DefaultDatabaseInitializerService(DatabaseConfiguration config, FlywayMigrator migrator) {
    this.config = config;
    this.migrator = migrator;
  }

  @Override
  public void migrate() {
    logger.info("Running database migration with Flyway...");
    migrator.migrate(config);
    logger.info("Database migration completed successfully.");
  }

  @Override
  protected void startUp() throws Exception {
    migrate();
  }

  @Override
  protected void shutDown() throws Exception {
    // No-op
  }
}
