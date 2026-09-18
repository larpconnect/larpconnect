package com.larpconnect.njall.data.migration;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/** Guice module binding database migration components. */
public final class MigrationModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DataSourceFactory.class).to(DefaultDataSourceFactory.class).in(Scopes.SINGLETON);
    bind(FlywayFactory.class).to(DefaultFlywayFactory.class).in(Scopes.SINGLETON);
    bind(DatabaseMigrator.class).to(FlywayDatabaseMigrator.class).in(Scopes.SINGLETON);
  }
}
