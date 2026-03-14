package com.larpconnect.njall.data;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith(VertxExtension.class)
public class GreetingTest {

  @Container
  public final PostgreSQLContainer<?> postgresContainer =
      new PostgreSQLContainer<>("postgres:18")
          .withDatabaseName("test")
          .withUsername("test")
          .withPassword("test");

  private Mutiny.SessionFactory sessionFactory;

  @Test
  public void saveAndRetrieveGreeting_success(VertxTestContext testContext) {
    Map<String, String> properties = new HashMap<>();
    properties.put("jakarta.persistence.jdbc.url", postgresContainer.getJdbcUrl());
    properties.put("jakarta.persistence.jdbc.user", postgresContainer.getUsername());
    properties.put("jakarta.persistence.jdbc.password", postgresContainer.getPassword());
    properties.put("hibernate.connection.pool_size", "10");

    sessionFactory =
        Persistence.createEntityManagerFactory("larpconnect", properties)
            .unwrap(Mutiny.SessionFactory.class);

    Greeting greeting = new Greeting("Hello, Vert.x and Hibernate Reactive!");

    sessionFactory
        .withTransaction(session -> session.persist(greeting))
        .chain(
            () ->
                sessionFactory.withSession(
                    session -> session.find(Greeting.class, greeting.getId())))
        .subscribe()
        .with(
            savedGreeting -> {
              testContext.verify(
                  () -> {
                    assertThat(savedGreeting).isNotNull();
                    assertThat(savedGreeting.getMessage())
                        .isEqualTo("Hello, Vert.x and Hibernate Reactive!");
                  });
              testContext.completeNow();
            },
            testContext::failNow);
  }
}
