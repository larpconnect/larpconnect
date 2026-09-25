package com.larpconnect.njall.data.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Key;
import com.larpconnect.njall.common.CommonModule;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DatabaseConfigModuleTest {

  @Test
  @DisplayName("DatabaseConfigModule provides DatabaseConfig, MigrationConfig, and session configs")
  void configure_whenInjected_providesConfigs() {
    var injector = Guice.createInjector(new CommonModule(), new DatabaseConfigModule());

    var dbConfig = injector.getInstance(DatabaseConfig.class);
    var migrationConfig = injector.getInstance(MigrationConfig.class);
    var adminConfig = injector.getInstance(Key.get(SessionConfig.class, NjallAdmin.class));
    var usersConfig = injector.getInstance(Key.get(SessionConfig.class, NjallUsers.class));

    assertThat(dbConfig.migration()).isSameAs(migrationConfig);
    assertThat(dbConfig.admin()).isSameAs(adminConfig);
    assertThat(dbConfig.users()).isSameAs(usersConfig);
    assertThat(injector.getInstance(SessionConfigFactory.class)).isNotNull();
    assertThat(adminConfig.username()).isEqualTo("njall_admin");
    assertThat(usersConfig.username()).isEqualTo("njall_users");
    assertThat(adminConfig.minPoolSize()).isEqualTo(2);
    assertThat(usersConfig.minPoolSize()).isEqualTo(5);
  }
}
