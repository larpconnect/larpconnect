package org.larpconnect.data;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link DefaultDatabaseMigrator}. */
@ExtendWith(MockitoExtension.class)
public final class DefaultDatabaseMigratorTest {
  @Mock private DatabaseConfiguration config;
  @Mock private FlywayMigrator migrator;

  private DefaultDatabaseMigrator databaseMigrator;

  @BeforeEach
  public void setUp() {
    databaseMigrator = new DefaultDatabaseMigrator(config, migrator);
  }

  @Test
  public void migrate_callsMigrator() {
    databaseMigrator.migrate();
    verify(migrator).migrate(config);
  }
}
