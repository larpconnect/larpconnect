package com.larpconnect.njall.data.dao.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AgentsDaoModuleTest {

  @Test
  @DisplayName("AgentsDaoModule binds AgentDAO and registers entities")
  void configure_bindsDependencies() {
    var sessionFactory = mock(SessionFactory.class);
    var session = mock(Session.class);
    var tx = mock(Transaction.class);
    NativeQuery<String> cfgQuery = mock();
    NativeQuery<UUID> lookupQuery = mock();
    when(sessionFactory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("set_config"), eq(String.class))).thenReturn(cfgQuery);
    when(cfgQuery.setParameter(any(String.class), any())).thenReturn(cfgQuery);
    when(cfgQuery.getSingleResult()).thenReturn("ok");
    when(session.createNativeQuery(contains("FROM njall_users.actors"), eq(UUID.class)))
        .thenReturn(lookupQuery);
    when(lookupQuery.setParameter(any(String.class), any())).thenReturn(lookupQuery);
    when(lookupQuery.getResultList()).thenReturn(List.of());

    var testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(SessionFactory.class).annotatedWith(NjallUsers.class).toInstance(sessionFactory);
            bind(Clock.class).toInstance(Clock.systemUTC());
          }
        };

    var injector = Guice.createInjector(new AgentsDaoModule(), testModule);

    var dao = injector.getInstance(AgentDAO.class);
    assertThat(dao.findByPreferredUsername(UUID.randomUUID(), "unknown")).isEmpty();

    var adminEntities =
        injector.getInstance(Key.get(new TypeLiteral<Set<Class<?>>>() {}, NjallAdmin.class));
    assertThat(adminEntities).contains(AgentEntity.class, AgentEndpointEntity.class);

    var userEntities =
        injector.getInstance(Key.get(new TypeLiteral<Set<Class<?>>>() {}, NjallUsers.class));
    assertThat(userEntities).contains(AgentEntity.class, AgentEndpointEntity.class);
  }
}
