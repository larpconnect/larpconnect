package org.larpconnect.data;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DefaultDatabaseInitializerService}. */
public final class DefaultDatabaseInitializerServiceTest {
  private DatabaseConfiguration config;
  private FlywayMigrator migrator;
  private DefaultDatabaseInitializerService service;

  @BeforeEach
  public void setUp() {
    config = mock(DatabaseConfiguration.class);
    migrator = mock(FlywayMigrator.class);
    service = new DefaultDatabaseInitializerService(config, migrator);
  }

  @Test
  public void migrate_callsMigrator() {
    service.migrate();
    verify(migrator).migrate(config);
  }

  @Test
  public void startUp_callsMigrate() throws Exception {
    service.startUp();
    verify(migrator).migrate(config);
  }

  @Test
  public void shutDown_isNoOp() throws Exception {
    service.shutDown();
    // Verify no exceptions or errors occurred
  }
}
