package com.larpconnect.njall.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.util.Modules;
import com.larpconnect.njall.common.CommonModule;
import com.larpconnect.njall.data.config.DatabaseConfig;
import com.larpconnect.njall.data.dao.ServerDAO;
import com.larpconnect.njall.data.migration.DatabaseMigrator;
import com.larpconnect.njall.data.session.SessionFactoryFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DataModuleTest {

  @Test
  @DisplayName("DataModule successfully installs all data submodules")
  void configure_whenInjected_providesAllDataBindings() {
    var mockFactory = mock(SessionFactoryFactory.class);
    when(mockFactory.create(any(), any())).thenReturn(mock(SessionFactory.class));

    var testOverride =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(SessionFactoryFactory.class).toInstance(mockFactory);
          }
        };

    var injector =
        Guice.createInjector(
            Modules.override(new CommonModule(), new DataModule()).with(testOverride));

    var dbConfig = injector.getInstance(DatabaseConfig.class);
    var migrator = injector.getInstance(DatabaseMigrator.class);
    var serverDao = injector.getInstance(ServerDAO.class);

    assertThat(dbConfig).isNotNull();
    assertThat(migrator).isNotNull();
    assertThat(serverDao).isNotNull();
  }
}
