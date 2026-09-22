package com.larpconnect.njall.data.migration;

import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import com.larpconnect.njall.data.config.MigrationConfig;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

/** Default implementation of {@link DataSourceFactory} constructing PostgreSQL data sources. */
final class DefaultDataSourceFactory implements DataSourceFactory {

  @Inject
  DefaultDataSourceFactory() {}

  @Override
  public DataSource create(MigrationConfig config) {
    requireNonNull(config, "config cannot be null");
    var dataSource = newDataSource();
    configureDataSource(dataSource, config);
    return dataSource;
  }

  private PGSimpleDataSource newDataSource() {
    return new PGSimpleDataSource();
  }

  private void configureDataSource(PGSimpleDataSource dataSource, MigrationConfig config) {
    dataSource.setUrl(config.jdbcUrl());
    dataSource.setUser(config.username());
    if (config.hasPassword()) {
      dataSource.setPassword(config.password());
    }
  }
}
