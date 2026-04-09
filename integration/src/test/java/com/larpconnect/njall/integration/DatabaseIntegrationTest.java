package com.larpconnect.njall.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

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

@Disabled("Testcontainers not working in sandbox")
final class DatabaseIntegrationTest {

  private static Injector injector;
  private static ExternalResourceDao dao;

  @BeforeAll
  static void setUp() {
    System.setProperty("jakarta.persistence.jdbc.url", "jdbc:postgresql://localhost:5432/njall");
    System.setProperty("jakarta.persistence.jdbc.user", "test");
    System.setProperty("jakarta.persistence.jdbc.password", "test");
    System.setProperty("hibernate.hbm2ddl.auto", "update");

    injector = Guice.createInjector(new DataModule());
    dao = injector.getInstance(ExternalResourceDao.class);
  }

  @AfterAll
  static void tearDown() {}

  @Test
  void serializationAndDeserialization_validEntity_succeeds() {
    ExternalResource resource = mock(ExternalResource.class, CALLS_REAL_METHODS);
    var id = UUID.randomUUID();
    resource.setId(id);
    resource.setExternalUri("https://example.com");
    resource.setData("{\"key\": \"value\"}");
    resource.setLastRefresh(OffsetDateTime.now(java.time.ZoneId.systemDefault()));

    dao.persist("testserver", resource).await().indefinitely();

    var fetched = dao.findById("testserver", id).await().indefinitely();
    assertThat(fetched).isNotNull();
    assertThat(fetched.getId()).isEqualTo(id);
    assertThat(fetched.getExternalUri()).isEqualTo("https://example.com");
  }
}
