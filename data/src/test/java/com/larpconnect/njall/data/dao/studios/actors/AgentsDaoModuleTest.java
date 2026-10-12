package com.larpconnect.njall.data.dao.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AgentsDaoModuleTest {

  @Test
  @DisplayName("AgentsDaoModule binds AgentDAO and registers entities")
  void configure_bindsDependencies() {
    var sessionFactory = mock(SessionFactory.class);
    var testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(SessionFactory.class).annotatedWith(NjallUsers.class).toInstance(sessionFactory);
          }
        };

    var injector = Guice.createInjector(new AgentsDaoModule(), testModule);

    var dao = injector.getInstance(AgentDAO.class);
    assertThat(dao).isInstanceOf(DefaultAgentDAO.class);

    var adminEntities =
        injector.getInstance(Key.get(new TypeLiteral<Set<Class<?>>>() {}, NjallAdmin.class));
    assertThat(adminEntities).contains(AgentEntity.class, AgentEndpointEntity.class);

    var userEntities =
        injector.getInstance(Key.get(new TypeLiteral<Set<Class<?>>>() {}, NjallUsers.class));
    assertThat(userEntities).contains(AgentEntity.class, AgentEndpointEntity.class);
  }
}
