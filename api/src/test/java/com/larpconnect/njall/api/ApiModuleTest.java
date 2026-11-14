package com.larpconnect.njall.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.admin.AdminRoute;
import com.larpconnect.njall.api.http.RootRoute;
import com.larpconnect.njall.api.studios.StudiosRoute;
import com.larpconnect.njall.api.studios.actors.AgentRoute;
import com.larpconnect.njall.api.studios.campaigns.CampaignsRoute;
import com.larpconnect.njall.api.studios.collections.CollectionsRoute;
import com.larpconnect.njall.api.studios.events.EventsRoute;
import com.larpconnect.njall.api.studios.games.GamesRoute;
import com.larpconnect.njall.api.studios.individuals.IndividualsRoute;
import com.larpconnect.njall.api.studios.links.LinksRoute;
import com.larpconnect.njall.api.studios.locations.LocationsRoute;
import com.larpconnect.njall.api.studios.systems.SystemsRoute;
import com.larpconnect.njall.api.studios.tags.TagsRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.common.telemetry.TelemetryModule;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.admin.AdminRoleDAO;
import com.larpconnect.njall.data.dao.admin.AdminUserDAO;
import com.larpconnect.njall.data.dao.admin.StudioRoleDAO;
import com.larpconnect.njall.data.dao.servers.ServerDAO;
import com.larpconnect.njall.data.dao.studios.AddressDAO;
import com.larpconnect.njall.data.dao.studios.HashtagDAO;
import com.larpconnect.njall.data.dao.studios.LinkDAO;
import com.larpconnect.njall.data.dao.studios.LocationDAO;
import com.larpconnect.njall.data.dao.studios.StudioDAO;
import com.larpconnect.njall.data.dao.studios.StudioLookupDAO;
import com.larpconnect.njall.data.dao.studios.actors.AgentDAO;
import com.larpconnect.njall.data.dao.studios.campaigns.CampaignDAO;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import com.larpconnect.njall.data.dao.studios.events.EventDAO;
import com.larpconnect.njall.data.dao.studios.games.GameDAO;
import com.larpconnect.njall.data.dao.studios.individuals.IndividualDAO;
import com.larpconnect.njall.data.dao.studios.systems.GameSystemDAO;
import io.dropwizard.metrics5.health.HealthCheckRegistry;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ApiModuleTest {

  private static ActorSystem<Void> system;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "api-module-test");
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("configure installs HttpModule, AdminModule, and StudiosModule sub-routes")
  void configure_createsInjector_bindsRoutes() {
    var injector = Guice.createInjector(new ApiModule(), new TelemetryModule(), createTestModule());

    assertThat(injector.getInstance(RootRoute.class)).isInstanceOf(RootRoute.class);
    assertThat(injector.getInstance(AdminRoute.class)).isInstanceOf(AdminRoute.class);
    assertThat(injector.getInstance(StudiosRoute.class)).isInstanceOf(StudiosRoute.class);
    assertThat(injector.getInstance(LinksRoute.class)).isInstanceOf(LinksRoute.class);
    assertThat(injector.getInstance(LocationsRoute.class)).isInstanceOf(LocationsRoute.class);
    assertThat(injector.getInstance(TagsRoute.class)).isInstanceOf(TagsRoute.class);
    assertThat(injector.getInstance(EventsRoute.class)).isInstanceOf(EventsRoute.class);
    assertThat(injector.getInstance(SystemsRoute.class)).isInstanceOf(SystemsRoute.class);
    assertThat(injector.getInstance(CampaignsRoute.class)).isInstanceOf(CampaignsRoute.class);
    assertThat(injector.getInstance(GamesRoute.class)).isInstanceOf(GamesRoute.class);
    assertThat(injector.getInstance(IndividualsRoute.class)).isInstanceOf(IndividualsRoute.class);
    assertThat(injector.getInstance(CollectionsRoute.class)).isInstanceOf(CollectionsRoute.class);
    assertThat(injector.getInstance(AgentRoute.class)).isInstanceOf(AgentRoute.class);
  }

  private Module createTestModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(new TypeLiteral<ActorSystem<Void>>() {}).toInstance(system);
        bind(HealthCheckRegistry.class).toInstance(new HealthCheckRegistry());
        bind(ServerDAO.class).toInstance(mock(ServerDAO.class));
        bind(StudioLookupDAO.class).toInstance(mock(StudioLookupDAO.class));
        bind(StudioDAO.class).toInstance(mock(StudioDAO.class));
        bind(LinkDAO.class).toInstance(mock(LinkDAO.class));
        bind(LocationDAO.class).toInstance(mock(LocationDAO.class));
        bind(AddressDAO.class).toInstance(mock(AddressDAO.class));
        bind(HashtagDAO.class).toInstance(mock(HashtagDAO.class));
        bind(EventDAO.class).toInstance(mock(EventDAO.class));
        bind(GameSystemDAO.class).toInstance(mock(GameSystemDAO.class));
        bind(CampaignDAO.class).toInstance(mock(CampaignDAO.class));
        bind(GameDAO.class).toInstance(mock(GameDAO.class));
        bind(IndividualDAO.class).toInstance(mock(IndividualDAO.class));
        bind(CollectionDAO.class).toInstance(mock(CollectionDAO.class));
        bind(AgentDAO.class).toInstance(mock(AgentDAO.class));
        bind(StudioLookupCache.class).toInstance(mock(StudioLookupCache.class));
        bind(StudioRoleDAO.class).toInstance(mock(StudioRoleDAO.class));
        bind(AdminRoleDAO.class).toInstance(mock(AdminRoleDAO.class));
        bind(AdminUserDAO.class).toInstance(mock(AdminUserDAO.class));
        bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
      }
    };
  }
}
