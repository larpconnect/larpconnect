package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
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
import com.larpconnect.njall.data.dao.studios.campaigns.CampaignDAO;
import com.larpconnect.njall.data.dao.studios.events.EventDAO;
import com.larpconnect.njall.data.dao.studios.games.GameDAO;
import com.larpconnect.njall.data.dao.studios.systems.GameSystemDAO;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DaoModuleTest {

  @Test
  @DisplayName("DaoModule binds DAOs to their default implementations")
  void configure_whenInjected_providesDaos() {
    var injector = createInjector();

    assertThat(injector.getInstance(ServerDAO.class)).isInstanceOf(ServerDAO.class);
    assertThat(injector.getInstance(AdminRoleDAO.class)).isInstanceOf(AdminRoleDAO.class);
    assertThat(injector.getInstance(AdminUserDAO.class)).isInstanceOf(AdminUserDAO.class);
    assertThat(injector.getInstance(StudioLookupDAO.class)).isInstanceOf(StudioLookupDAO.class);
    assertThat(injector.getInstance(StudioDAO.class)).isInstanceOf(StudioDAO.class);
    assertThat(injector.getInstance(StudioRoleDAO.class)).isInstanceOf(StudioRoleDAO.class);
    assertThat(injector.getInstance(LinkDAO.class)).isInstanceOf(LinkDAO.class);
    assertThat(injector.getInstance(LocationDAO.class)).isInstanceOf(LocationDAO.class);
    assertThat(injector.getInstance(AddressDAO.class)).isInstanceOf(AddressDAO.class);
    assertThat(injector.getInstance(EventDAO.class)).isInstanceOf(EventDAO.class);
    assertThat(injector.getInstance(GameSystemDAO.class)).isInstanceOf(GameSystemDAO.class);
    assertThat(injector.getInstance(CampaignDAO.class)).isInstanceOf(CampaignDAO.class);
    assertThat(injector.getInstance(GameDAO.class)).isInstanceOf(GameDAO.class);
  }

  @Test
  @DisplayName("DaoModule registers studio event, system, campaign, and game JPA entities")
  void configure_registersStudioSubpackageEntities() {
    var injector = createInjector();
    var entitySetType = new TypeLiteral<Set<Class<?>>>() {};

    var adminEntityNames =
        injector.getInstance(Key.get(entitySetType, NjallAdmin.class)).stream()
            .map(Class::getSimpleName)
            .toList();
    var userEntityNames =
        injector.getInstance(Key.get(entitySetType, NjallUsers.class)).stream()
            .map(Class::getSimpleName)
            .toList();

    assertThat(adminEntityNames)
        .contains("EventEntity", "GameSystemEntity", "CampaignEntity", "GameEntity");
    assertThat(userEntityNames)
        .contains("EventEntity", "GameSystemEntity", "CampaignEntity", "GameEntity");
  }

  private static Injector createInjector() {
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
    return Guice.createInjector(mockModule, new DaoModule());
  }
}
