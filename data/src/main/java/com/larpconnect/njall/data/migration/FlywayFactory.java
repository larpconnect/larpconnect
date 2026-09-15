package com.larpconnect.njall.data.migration;

import com.larpconnect.njall.data.config.MigrationConfig;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/** Factory for constructing configured {@link Flyway} instances. */
public interface FlywayFactory {

  /**
   * Creates a configured {@link Flyway} instance.
   *
   * @param config The migration configuration.
   * @param dataSource The database DataSource.
   * @return A configured {@link Flyway} instance.
   */
  Flyway create(MigrationConfig config, DataSource dataSource);
}
