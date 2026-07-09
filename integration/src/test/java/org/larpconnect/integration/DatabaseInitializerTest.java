package org.larpconnect.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.larpconnect.data.DataModule;
import org.larpconnect.data.DatabaseConfiguration;
import org.larpconnect.data.DatabaseInitializer;
import org.larpconnect.data.TestTableDao;
import org.larpconnect.data.TestTableEntity;
import org.testcontainers.containers.PostgreSQLContainer;

/** Integration test for verifying Flyway migrations and Hibernate operations with PostgreSQL. */
public final class DatabaseInitializerTest {
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:18-alpine");

  private static Injector injector;

  @BeforeAll
  public static void setUp() {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        org.testcontainers.DockerClientFactory.instance().isDockerAvailable(),
        "Docker is not available. Skipping database integration tests.");

    postgres.start();

    DatabaseConfiguration testConfig =
        new DatabaseConfiguration(
            postgres.getHost(),
            postgres.getMappedPort(5432),
            postgres.getDatabaseName(),
            postgres.getUsername(),
            postgres.getPassword());

    injector =
        Guice.createInjector(
            Modules.override(new DataModule())
                .with(
                    new AbstractModule() {
                      @Override
                      protected void configure() {
                        bind(DatabaseConfiguration.class).toInstance(testConfig);
                      }
                    }));
  }

  @AfterAll
  public static void tearDown() {
    postgres.stop();
  }

  @Test
  public void migrateAndAccess_withTestcontainers_succeeds() {
    DatabaseInitializer initializer = injector.getInstance(DatabaseInitializer.class);
    initializer.migrate();

    TestTableDao dao = injector.getInstance(TestTableDao.class);
    UUID id = UUID.randomUUID();
    TestTableEntity entity = new TestTableEntity(id, "Verification Value");
    dao.save(entity);

    Optional<TestTableEntity> retrieved = dao.findById(id);
    assertThat(retrieved).hasValue(entity);
  }
}
