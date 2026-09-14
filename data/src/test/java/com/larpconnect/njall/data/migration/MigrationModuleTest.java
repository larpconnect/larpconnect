package com.larpconnect.njall.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.larpconnect.njall.common.CommonModule;
import com.larpconnect.njall.data.config.DatabaseConfigModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class MigrationModuleTest {

  @Test
  @DisplayName("MigrationModule provides DatabaseMigrator, DataSourceFactory, and FlywayFactory")
  void configure_whenInjected_providesMigrationBindings() {
    var injector =
        Guice.createInjector(new CommonModule(), new DatabaseConfigModule(), new MigrationModule());

    var migrator = injector.getInstance(DatabaseMigrator.class);
    var dataSourceFactory = injector.getInstance(DataSourceFactory.class);
    var flywayFactory = injector.getInstance(FlywayFactory.class);

    assertThat(migrator).isInstanceOf(FlywayDatabaseMigrator.class);
    assertThat(dataSourceFactory).isInstanceOf(DefaultDataSourceFactory.class);
    assertThat(flywayFactory).isInstanceOf(DefaultFlywayFactory.class);
  }
}
