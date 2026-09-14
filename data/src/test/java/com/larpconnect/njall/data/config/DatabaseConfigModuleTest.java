package com.larpconnect.njall.data.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.larpconnect.njall.common.CommonModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DatabaseConfigModuleTest {

  @Test
  @DisplayName("DatabaseConfigModule provides DatabaseConfig and MigrationConfig")
  void configure_whenInjected_providesConfigs() {
    var injector = Guice.createInjector(new CommonModule(), new DatabaseConfigModule());

    var dbConfig = injector.getInstance(DatabaseConfig.class);
    var migrationConfig = injector.getInstance(MigrationConfig.class);

    assertThat(dbConfig).isNotNull();
    assertThat(migrationConfig).isNotNull();
    assertThat(dbConfig.migration()).isSameAs(migrationConfig);
    assertThat(migrationConfig.username()).isEqualTo("njall");
    assertThat(migrationConfig.defaultSchema()).isEqualTo("njall");
    assertThat(migrationConfig.schemas())
        .containsExactly("njall", "njall_admin", "njall_users", "njall_system");
  }
}
