package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.larpconnect.njall.data.DataModule;
import com.larpconnect.njall.data.dao.ExternalResourceDao;
import com.larpconnect.njall.data.entity.ExternalResource;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Disabled("Testcontainers not working in sandbox")
@Testcontainers
final class DatabaseIntegrationTest {

  @Container
  @SuppressWarnings("deprecation")
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:18-alpine");

  private static Injector injector;
  private static ExternalResourceDao dao;

  @BeforeAll
  static void setUp() {
    System.setProperty("jakarta.persistence.jdbc.url", postgres.getJdbcUrl());
    System.setProperty("jakarta.persistence.jdbc.user", postgres.getUsername());
    System.setProperty("jakarta.persistence.jdbc.password", postgres.getPassword());
    System.setProperty("hibernate.hbm2ddl.auto", "update");

    injector = Guice.createInjector(new DataModule());
    dao = injector.getInstance(ExternalResourceDao.class);
  }

  @AfterAll
  static void tearDown() {}

  @Test
  void serializationAndDeserialization_validEntity_succeeds() {
    ExternalResource resource = Mockito.mock(ExternalResource.class, Mockito.CALLS_REAL_METHODS);
    UUID id = UUID.randomUUID();
    resource.setId(id);
    resource.setExternalUri("https://example.com");
    resource.setData("{\"key\": \"value\"}");
    resource.setLastRefresh(OffsetDateTime.now(java.time.ZoneId.systemDefault()));

    dao.persist(resource).await().indefinitely();

    ExternalResource fetched = dao.findById(id).await().indefinitely();
    assertThat(fetched).isNotNull();
    assertThat(fetched.getId()).isEqualTo(id);
    assertThat(fetched.getExternalUri()).isEqualTo("https://example.com");
  }
}
