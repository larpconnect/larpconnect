package com.larpconnect.njall.data.migration;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/** Guice module binding database migration components. */
public final class MigrationModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DataSourceFactory.class).to(DefaultDataSourceFactory.class).in(Singleton.class);
    bind(FlywayFactory.class).to(DefaultFlywayFactory.class).in(Singleton.class);
    bind(DatabaseMigrator.class).to(FlywayDatabaseMigrator.class).in(Singleton.class);
  }
}
