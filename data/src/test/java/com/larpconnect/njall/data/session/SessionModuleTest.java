package com.larpconnect.njall.data.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.larpconnect.njall.common.CommonModule;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.config.DatabaseConfigModule;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class SessionModuleTest {

  @Test
  @DisplayName("SessionModule binds SessionFactory instances for admin and users")
  void configure_whenInjected_providesSessionFactories() {
    var mockFactory = mock(SessionFactoryFactory.class);
    var mockAdminSession = mock(SessionFactory.class);
    var mockUsersSession = mock(SessionFactory.class);

    when(mockFactory.create(any(), any())).thenReturn(mockAdminSession, mockUsersSession);

    var overrideModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(SessionFactoryFactory.class).toInstance(mockFactory);
          }
        };

    var injector =
        Guice.createInjector(
            new CommonModule(),
            new DatabaseConfigModule(),
            com.google.inject.util.Modules.override(new SessionModule()).with(overrideModule));

    var adminSessionFactory = injector.getInstance(Key.get(SessionFactory.class, NjallAdmin.class));
    var usersSessionFactory = injector.getInstance(Key.get(SessionFactory.class, NjallUsers.class));
    var activeFactories = injector.getInstance(ActiveSessionFactories.class);

    assertThat(adminSessionFactory).isSameAs(mockAdminSession);
    assertThat(usersSessionFactory).isSameAs(mockUsersSession);
    assertThat(activeFactories.activeCount()).isEqualTo(2);
  }
}
