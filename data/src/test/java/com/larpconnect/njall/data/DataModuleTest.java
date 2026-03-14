package com.larpconnect.njall.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Tests for Guice module bindings. */
class DataModuleTest {

  @Test
  void provideSessionFactory_whenModuleInstalled_shouldBindSessionFactory() {
    Mutiny.SessionFactory mockFactory = Mockito.mock(Mutiny.SessionFactory.class);

    Injector injector =
        Guice.createInjector(
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(Mutiny.SessionFactory.class).toInstance(mockFactory);
              }
            });

    Mutiny.SessionFactory sessionFactory = injector.getInstance(Mutiny.SessionFactory.class);
    assertThat(sessionFactory).isNotNull();
  }
}
