package com.larpconnect.njall.data.migration;

import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import com.larpconnect.njall.data.config.MigrationConfig;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/** Default implementation of {@link FlywayFactory} configuring Flyway. */
final class DefaultFlywayFactory implements FlywayFactory {

  @Inject
  DefaultFlywayFactory() {}

  @Override
  public Flyway create(MigrationConfig config, DataSource dataSource) {
    requireNonNull(config, "config cannot be null");
    requireNonNull(dataSource, "dataSource cannot be null");
    return Flyway.configure()
        .dataSource(dataSource)
        .schemas(config.schemas().toArray(String[]::new))
        .defaultSchema(config.defaultSchema())
        .placeholders(config.placeholders())
        .load();
  }
}
