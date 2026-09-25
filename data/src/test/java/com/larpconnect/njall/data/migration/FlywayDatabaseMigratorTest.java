package com.larpconnect.njall.data.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.config.MigrationConfig;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class FlywayDatabaseMigratorTest {

  private MigrationConfig config;
  private DataSourceFactory dataSourceFactory;
  private FlywayFactory flywayFactory;
  private TestCloseableDataSource closeableDataSource;
  private Flyway flyway;

  @BeforeEach
  void setUp() {
    config =
        new MigrationConfig(
            "jdbc:postgresql://localhost:5432/test",
            "njall",
            "pass",
            List.of("njall"),
            "njall",
            Map.of("server_name", "test"));
    dataSourceFactory = mock(DataSourceFactory.class);
    flywayFactory = mock(FlywayFactory.class);
    closeableDataSource = mock(TestCloseableDataSource.class);
    flyway = mock(Flyway.class);

    when(dataSourceFactory.create(config)).thenReturn(closeableDataSource);
    when(flywayFactory.create(config, closeableDataSource)).thenReturn(flyway);
  }

  @Test
  @DisplayName("migrate successfully executes migrations and closes data source")
  void migrate_whenInvoked_executesMigrationsAndClosesDataSource() {
    var migrateResult = new MigrateResult();
    migrateResult.migrationsExecuted = 3;
    when(flyway.migrate()).thenReturn(migrateResult);

    var migrator = new FlywayDatabaseMigrator(config, dataSourceFactory, flywayFactory);
    var count = migrator.migrate();

    assertThat(count).isEqualTo(3);
    verify(closeableDataSource).close();
  }

  @Test
  @DisplayName("migrate closes data source even when migration throws exception")
  void migrate_whenMigrationFails_closesDataSourceAndRethrows() {
    when(flyway.migrate()).thenThrow(new IllegalStateException("DB unreachable"));

    var migrator = new FlywayDatabaseMigrator(config, dataSourceFactory, flywayFactory);

    assertThatThrownBy(migrator::migrate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DB unreachable");
    verify(closeableDataSource).close();
  }

  @Test
  @DisplayName("migrate handles exception during data source close gracefully")
  void migrate_whenDataSourceCloseFails_returnsMigrationCount() {
    var migrateResult = new MigrateResult();
    migrateResult.migrationsExecuted = 1;
    when(flyway.migrate()).thenReturn(migrateResult);
    doThrow(new RuntimeException("Close failed")).when(closeableDataSource).close();

    var migrator = new FlywayDatabaseMigrator(config, dataSourceFactory, flywayFactory);
    var count = migrator.migrate();

    assertThat(count).isEqualTo(1);
    verify(closeableDataSource).close();
  }

  @Test
  @DisplayName("migrate handles non-AutoCloseable data source safely")
  void migrate_whenDataSourceNotCloseable_completesSuccessfully() {
    var plainDataSource = mock(DataSource.class);
    when(dataSourceFactory.create(config)).thenReturn(plainDataSource);
    when(flywayFactory.create(config, plainDataSource)).thenReturn(flyway);

    var migrateResult = new MigrateResult();
    migrateResult.migrationsExecuted = 2;
    when(flyway.migrate()).thenReturn(migrateResult);

    var migrator = new FlywayDatabaseMigrator(config, dataSourceFactory, flywayFactory);
    var count = migrator.migrate();

    assertThat(count).isEqualTo(2);
  }

  private interface TestCloseableDataSource extends DataSource, AutoCloseable {
    @Override
    void close();
  }
}
