package org.larpconnect.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.larpconnect.common.Environment;
import org.larpconnect.common.MapEnvironment;
import org.larpconnect.data.DataModule;
import org.larpconnect.data.DatabaseMigrator;
import org.larpconnect.data.TestTable;
import org.larpconnect.data.TestTableDao;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integration test for verifying Flyway migrations and Hibernate operations with PostgreSQL. */
public final class DatabaseMigratorTest {
  private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  private static Injector injector;

  @BeforeAll
  public static void setUp() {
    System.setProperty("api.version", "1.44");
    Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available. Skipping database integration tests.");

    postgres.start();

    MapEnvironment testEnv =
        new MapEnvironment(
            Map.of(
                "DB_HOST", postgres.getHost(),
                "DB_PORT", String.valueOf(postgres.getMappedPort(5432)),
                "DB_DATABASE", postgres.getDatabaseName(),
                "DB_USERNAME", postgres.getUsername(),
                "DB_PASSWORD", postgres.getPassword()));

    injector =
        Guice.createInjector(
            Modules.override(new DataModule())
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        bind(Environment.class).toInstance(testEnv);
                      }
                    }));
  }

  @AfterAll
  public static void tearDown() {
    postgres.stop();
  }

  @Test
  public void migrateAndAccess_withTestcontainers_succeeds() {
    DatabaseMigrator initializer = injector.getInstance(DatabaseMigrator.class);
    initializer.migrate();

    TestTableDao dao = injector.getInstance(TestTableDao.class);
    UUID id = UUID.randomUUID();
    TestTable entity = new TestTable(id, "Verification Value");
    dao.save(entity);

    Optional<TestTable> retrieved = dao.findById(id);
    assertThat(retrieved).hasValue(entity);
  }
}
