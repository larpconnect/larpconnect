package com.larpconnect.njall.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.larpconnect.njall.common.CommonModule;
import com.larpconnect.njall.data.config.DatabaseConfig;
import com.larpconnect.njall.data.migration.DatabaseMigrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DataModuleTest {

  @Test
  @DisplayName("DataModule successfully installs DatabaseConfigModule and MigrationModule")
  void configure_whenInjected_providesAllDataBindings() {
    var injector = Guice.createInjector(new CommonModule(), new DataModule());

    var dbConfig = injector.getInstance(DatabaseConfig.class);
    var migrator = injector.getInstance(DatabaseMigrator.class);

    assertThat(dbConfig).isNotNull();
    assertThat(migrator).isNotNull();
  }
}
