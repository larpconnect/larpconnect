package com.larpconnect.njall.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.admin.AdminRoute;
import com.larpconnect.njall.api.http.RootRoute;
import com.larpconnect.njall.api.studios.StudiosRoute;
import com.larpconnect.njall.api.studios.events.EventsRoute;
import com.larpconnect.njall.api.studios.individuals.IndividualsRoute;
import com.larpconnect.njall.api.studios.links.LinksRoute;
import com.larpconnect.njall.api.studios.locations.LocationsRoute;
import com.larpconnect.njall.api.studios.tags.TagsRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.common.telemetry.TelemetryModule;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.admin.AdminRoleDAO;
import com.larpconnect.njall.data.dao.admin.AdminUserDAO;
import com.larpconnect.njall.data.dao.admin.StudioRoleDAO;
import com.larpconnect.njall.data.dao.servers.ServerDAO;
import com.larpconnect.njall.data.dao.studios.AddressDAO;
import com.larpconnect.njall.data.dao.studios.EventDAO;
import com.larpconnect.njall.data.dao.studios.HashtagDAO;
import com.larpconnect.njall.data.dao.studios.LinkDAO;
import com.larpconnect.njall.data.dao.studios.LocationDAO;
import com.larpconnect.njall.data.dao.studios.StudioDAO;
import com.larpconnect.njall.data.dao.studios.StudioLookupDAO;
import com.larpconnect.njall.data.dao.studios.individuals.IndividualDAO;
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
  @DisplayName("configure installs HttpModule and AdminModule binding RootRoute and AdminRoute")
  void configure_createsInjector_bindsRoutes() {
    var testModule =
        new AbstractModule() {
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
            bind(IndividualDAO.class).toInstance(mock(IndividualDAO.class));
            bind(StudioLookupCache.class).toInstance(mock(StudioLookupCache.class));
            bind(StudioRoleDAO.class).toInstance(mock(StudioRoleDAO.class));
            bind(AdminRoleDAO.class).toInstance(mock(AdminRoleDAO.class));
            bind(AdminUserDAO.class).toInstance(mock(AdminUserDAO.class));
            bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
          }
        };

    var injector = Guice.createInjector(new ApiModule(), new TelemetryModule(), testModule);
    var rootRoute = injector.getInstance(RootRoute.class);
    var adminRoute = injector.getInstance(AdminRoute.class);
    var studiosRoute = injector.getInstance(StudiosRoute.class);
    var linksRoute = injector.getInstance(LinksRoute.class);
    var locationsRoute = injector.getInstance(LocationsRoute.class);
    var tagsRoute = injector.getInstance(TagsRoute.class);
    var eventsRoute = injector.getInstance(EventsRoute.class);
    var individualsRoute = injector.getInstance(IndividualsRoute.class);

    assertThat(rootRoute).isInstanceOf(RootRoute.class);
    assertThat(adminRoute).isInstanceOf(AdminRoute.class);
    assertThat(studiosRoute).isInstanceOf(StudiosRoute.class);
    assertThat(linksRoute).isInstanceOf(LinksRoute.class);
    assertThat(locationsRoute).isInstanceOf(LocationsRoute.class);
    assertThat(tagsRoute).isInstanceOf(TagsRoute.class);
    assertThat(eventsRoute).isInstanceOf(EventsRoute.class);
    assertThat(individualsRoute).isInstanceOf(IndividualsRoute.class);
  }
}
