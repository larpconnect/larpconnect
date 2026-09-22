package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DaoModuleTest {

  @Test
  @DisplayName("DaoModule binds ServerDAO to DefaultServerDAO")
  void configure_whenInjected_providesServerDao() {
    var mockAdminSession = mock(SessionFactory.class);
    var mockModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(SessionFactory.class).annotatedWith(NjallAdmin.class).toInstance(mockAdminSession);
          }
        };

    var injector = Guice.createInjector(mockModule, new DaoModule());

    var serverDao = injector.getInstance(ServerDAO.class);
    var roleDao = injector.getInstance(AdminRoleDAO.class);
    var userDao = injector.getInstance(AdminUserDAO.class);
    var studioDao = injector.getInstance(StudioDAO.class);

    assertThat(serverDao).isNotNull().isInstanceOf(DefaultServerDAO.class);
    assertThat(roleDao).isNotNull().isInstanceOf(DefaultAdminRoleDAO.class);
    assertThat(userDao).isNotNull().isInstanceOf(DefaultAdminUserDAO.class);
    assertThat(studioDao).isNotNull().isInstanceOf(DefaultStudioDAO.class);
  }
}
