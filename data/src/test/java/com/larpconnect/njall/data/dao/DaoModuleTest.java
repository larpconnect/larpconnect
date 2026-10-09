package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.admin.AdminRoleDAO;
import com.larpconnect.njall.data.dao.admin.AdminUserDAO;
import com.larpconnect.njall.data.dao.admin.StudioRoleDAO;
import com.larpconnect.njall.data.dao.servers.ServerDAO;
import com.larpconnect.njall.data.dao.studios.AddressDAO;
import com.larpconnect.njall.data.dao.studios.LinkDAO;
import com.larpconnect.njall.data.dao.studios.LocationDAO;
import com.larpconnect.njall.data.dao.studios.StudioDAO;
import com.larpconnect.njall.data.dao.studios.StudioLookupDAO;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DaoModuleTest {

  @Test
  @DisplayName("DaoModule binds DAOs to their default implementations")
  void configure_whenInjected_providesDaos() {
    var mockAdminSession = mock(SessionFactory.class);
    var mockUsersSession = mock(SessionFactory.class);
    var mockModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(SessionFactory.class).annotatedWith(NjallAdmin.class).toInstance(mockAdminSession);
            bind(SessionFactory.class).annotatedWith(NjallUsers.class).toInstance(mockUsersSession);
          }
        };

    var injector = Guice.createInjector(mockModule, new DaoModule());

    var serverDao = injector.getInstance(ServerDAO.class);
    var roleDao = injector.getInstance(AdminRoleDAO.class);
    var userDao = injector.getInstance(AdminUserDAO.class);
    var studioLookupDao = injector.getInstance(StudioLookupDAO.class);
    var studioDao = injector.getInstance(StudioDAO.class);
    var studioRoleDao = injector.getInstance(StudioRoleDAO.class);
    var linkDao = injector.getInstance(LinkDAO.class);
    var locationDao = injector.getInstance(LocationDAO.class);
    var addressDao = injector.getInstance(AddressDAO.class);

    assertThat(serverDao).isInstanceOf(ServerDAO.class);
    assertThat(roleDao).isInstanceOf(AdminRoleDAO.class);
    assertThat(userDao).isInstanceOf(AdminUserDAO.class);
    assertThat(studioLookupDao).isInstanceOf(StudioLookupDAO.class);
    assertThat(studioDao).isInstanceOf(StudioDAO.class);
    assertThat(studioRoleDao).isInstanceOf(StudioRoleDAO.class);
    assertThat(linkDao).isInstanceOf(LinkDAO.class);
    assertThat(locationDao).isInstanceOf(LocationDAO.class);
    assertThat(addressDao).isInstanceOf(AddressDAO.class);
  }
}
