package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.events.EventsRoute;
import com.larpconnect.njall.api.studios.individuals.IndividualsRoute;
import com.larpconnect.njall.api.studios.links.LinksRoute;
import com.larpconnect.njall.api.studios.locations.LocationsRoute;
import com.larpconnect.njall.api.studios.tags.TagsRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.studios.AddressDAO;
import com.larpconnect.njall.data.dao.studios.EventDAO;
import com.larpconnect.njall.data.dao.studios.HashtagDAO;
import com.larpconnect.njall.data.dao.studios.LinkDAO;
import com.larpconnect.njall.data.dao.studios.LocationDAO;
import com.larpconnect.njall.data.dao.studios.StudioDAO;
import com.larpconnect.njall.data.dao.studios.individuals.IndividualDAO;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class StudiosModuleTest {

  private static ActorSystem<Void> system;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "studios-module-test");
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("StudiosModule binds StudiosRoute, RouteProvider, and StudioActor")
  void configure_bindsExpectedDependencies() {
    var studioDao = mock(StudioDAO.class);
    var linkDao = mock(LinkDAO.class);
    var locationDao = mock(LocationDAO.class);
    var addressDao = mock(AddressDAO.class);
    var hashtagDao = mock(HashtagDAO.class);
    var eventDao = mock(EventDAO.class);
    var individualDao = mock(IndividualDAO.class);
    var studioLookupCache = mock(StudioLookupCache.class);

    var testModule =
        createTestModule(
            studioDao,
            linkDao,
            locationDao,
            addressDao,
            hashtagDao,
            eventDao,
            individualDao,
            studioLookupCache);
    var injector = Guice.createInjector(new StudiosModule(), testModule);

    var studiosRoute = injector.getInstance(StudiosRoute.class);
    var linksRoute = injector.getInstance(LinksRoute.class);
    var locationsRoute = injector.getInstance(LocationsRoute.class);
    var tagsRoute = injector.getInstance(TagsRoute.class);
    var eventsRoute = injector.getInstance(EventsRoute.class);
    var individualsRoute = injector.getInstance(IndividualsRoute.class);
    var studioActor = injector.getInstance(Key.get(new TypeLiteral<ActorRef<StudioCommand>>() {}));

    assertThat(studiosRoute).isInstanceOf(StudiosRoute.class);
    assertThat(linksRoute).isInstanceOf(LinksRoute.class);
    assertThat(locationsRoute).isInstanceOf(LocationsRoute.class);
    assertThat(tagsRoute).isInstanceOf(TagsRoute.class);
    assertThat(eventsRoute).isInstanceOf(EventsRoute.class);
    assertThat(individualsRoute).isInstanceOf(IndividualsRoute.class);
    assertThat(studioActor).isInstanceOf(ActorRef.class);

    var routeProviders = injector.getInstance(Key.get(new TypeLiteral<Set<RouteProvider>>() {}));
    assertThat(routeProviders).contains(studiosRoute);
  }

  private Module createTestModule(
      StudioDAO studioDao,
      LinkDAO linkDao,
      LocationDAO locationDao,
      AddressDAO addressDao,
      HashtagDAO hashtagDao,
      EventDAO eventDao,
      IndividualDAO individualDao,
      StudioLookupCache studioLookupCache) {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(StudioDAO.class).toInstance(studioDao);
        bind(LinkDAO.class).toInstance(linkDao);
        bind(LocationDAO.class).toInstance(locationDao);
        bind(AddressDAO.class).toInstance(addressDao);
        bind(HashtagDAO.class).toInstance(hashtagDao);
        bind(EventDAO.class).toInstance(eventDao);
        bind(IndividualDAO.class).toInstance(individualDao);
        bind(StudioLookupCache.class).toInstance(studioLookupCache);
        bind(ObjectMapper.class).toInstance(new ObjectMapper());
        bind(Key.get(new TypeLiteral<ActorSystem<Void>>() {})).toInstance(system);
        bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
      }
    };
  }
}
