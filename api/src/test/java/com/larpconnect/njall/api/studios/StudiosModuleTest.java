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
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.AddressDAO;
import com.larpconnect.njall.data.dao.LinkDAO;
import com.larpconnect.njall.data.dao.LocationDAO;
import com.larpconnect.njall.data.dao.StudioDAO;
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
    var studioLookupCache = mock(StudioLookupCache.class);

    var testModule =
        createTestModule(studioDao, linkDao, locationDao, addressDao, studioLookupCache);
    var injector = Guice.createInjector(new StudiosModule(), testModule);

    var studiosRoute = injector.getInstance(StudiosRoute.class);
    var linksRoute = injector.getInstance(LinksRoute.class);
    var locationsRoute = injector.getInstance(LocationsRoute.class);
    var actorFactory = injector.getInstance(StudioActorFactory.class);
    var linkActorFactory = injector.getInstance(LinkActorFactory.class);
    var locationActorFactory = injector.getInstance(LocationActorFactory.class);
    var addressActorFactory = injector.getInstance(AddressActorFactory.class);
    var actorRef = injector.getInstance(Key.get(new TypeLiteral<ActorRef<StudioCommand>>() {}));
    var linkActorRef = injector.getInstance(Key.get(new TypeLiteral<ActorRef<LinkCommand>>() {}));
    var locationActorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<LocationCommand>>() {}));
    var addressActorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<AddressCommand>>() {}));
    var routeProviders = injector.getInstance(Key.get(new TypeLiteral<Set<RouteProvider>>() {}));

    assertThat(studiosRoute).isInstanceOf(StudiosRoute.class);
    assertThat(linksRoute).isInstanceOf(LinksRoute.class);
    assertThat(locationsRoute).isInstanceOf(LocationsRoute.class);
    assertThat(actorFactory).isInstanceOf(DefaultStudioActorFactory.class);
    assertThat(linkActorFactory).isInstanceOf(DefaultLinkActorFactory.class);
    assertThat(locationActorFactory).isInstanceOf(DefaultLocationActorFactory.class);
    assertThat(addressActorFactory).isInstanceOf(DefaultAddressActorFactory.class);
    assertThat(actorRef).isInstanceOf(ActorRef.class);
    assertThat(linkActorRef).isInstanceOf(ActorRef.class);
    assertThat(locationActorRef).isInstanceOf(ActorRef.class);
    assertThat(addressActorRef).isInstanceOf(ActorRef.class);
    assertThat(routeProviders)
        .hasAtLeastOneElementOfType(StudiosRoute.class)
        .hasAtLeastOneElementOfType(LinksRoute.class)
        .hasAtLeastOneElementOfType(LocationsRoute.class);
  }

  private static Module createTestModule(
      StudioDAO studioDao,
      LinkDAO linkDao,
      LocationDAO locationDao,
      AddressDAO addressDao,
      StudioLookupCache studioLookupCache) {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(new TypeLiteral<ActorSystem<Void>>() {}).toInstance(system);
        bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
        bind(StudioDAO.class).toInstance(studioDao);
        bind(LinkDAO.class).toInstance(linkDao);
        bind(LocationDAO.class).toInstance(locationDao);
        bind(AddressDAO.class).toInstance(addressDao);
        bind(StudioLookupCache.class).toInstance(studioLookupCache);
        bind(ObjectMapper.class).toInstance(new ObjectMapper());
      }
    };
  }
}
