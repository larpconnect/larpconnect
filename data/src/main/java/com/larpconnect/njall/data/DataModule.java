package com.larpconnect.njall.data;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.data.config.DatabaseConfigModule;
import com.larpconnect.njall.data.dao.DaoModule;
import com.larpconnect.njall.data.health.DataHealthModule;
import com.larpconnect.njall.data.migration.MigrationModule;
import com.larpconnect.njall.data.session.SessionModule;

/** Root Guice module for the :data persistence layer, installing subpackage modules. */
public final class DataModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new DatabaseConfigModule());
    install(new MigrationModule());
    install(new SessionModule());
    install(new DaoModule());
    install(new DataHealthModule());
  }
}
