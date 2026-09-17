package com.larpconnect.njall.data.migration;

import com.google.inject.Inject;
import com.larpconnect.njall.data.config.MigrationConfig;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flyway-backed implementation of {@link DatabaseMigrator} with transient connection lifecycles.
 */
public final class FlywayDatabaseMigrator implements DatabaseMigrator {

  private final Logger logger = LoggerFactory.getLogger(FlywayDatabaseMigrator.class);

  private final MigrationConfig config;
  private final DataSourceFactory dataSourceFactory;
  private final FlywayFactory flywayFactory;

  @Inject
  FlywayDatabaseMigrator(
      MigrationConfig config, DataSourceFactory dataSourceFactory, FlywayFactory flywayFactory) {
    this.config = config;
    this.dataSourceFactory = dataSourceFactory;
    this.flywayFactory = flywayFactory;
  }

  @Override
  public int migrate() {
    var dataSource = createDataSource();
    try {
      return executeMigrations(dataSource);
    } finally {
      closeDataSource(dataSource);
    }
  }

  private DataSource createDataSource() {
    logger.info("Starting database migration for schemas: {}", config.schemas());
    return dataSourceFactory.create(config);
  }

  private int executeMigrations(DataSource dataSource) {
    var flyway = flywayFactory.create(config, dataSource);
    var result = flyway.migrate();
    logger.info("Successfully executed {} database migrations.", result.migrationsExecuted);
    return result.migrationsExecuted;
  }

  private void closeDataSource(DataSource dataSource) {
    if (dataSource instanceof AutoCloseable closeable) {
      try {
        closeable.close();
      } catch (Exception e) {
        logger.warn("Failed to close migration data source cleanly", e);
      }
    }
  }
}
