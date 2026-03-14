package com.larpconnect.njall.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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

  @Test
  public void saveAndRetrieveGreeting_success(VertxTestContext testContext) {
    Injector injector =
        Guice.createInjector(
            new DataModule(),
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(String.class)
                    .annotatedWith(Names.named("db.url"))
                    .toInstance(postgresContainer.getJdbcUrl());
                bind(String.class)
                    .annotatedWith(Names.named("db.user"))
                    .toInstance(postgresContainer.getUsername());
                bind(String.class)
                    .annotatedWith(Names.named("db.password"))
                    .toInstance(postgresContainer.getPassword());
              }
            });

    Mutiny.SessionFactory sessionFactory = injector.getInstance(Mutiny.SessionFactory.class);
    Greeting greeting = new Greeting("Hello, Vert.x and Hibernate Reactive via Guice!");

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
                        .isEqualTo("Hello, Vert.x and Hibernate Reactive via Guice!");
                  });
              testContext.completeNow();
            },
            testContext::failNow);
  }
}
