package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.LinkDAO;
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
    var studioLookupCache = mock(StudioLookupCache.class);

    var testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(new TypeLiteral<ActorSystem<Void>>() {}).toInstance(system);
            bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
            bind(StudioDAO.class).toInstance(studioDao);
            bind(LinkDAO.class).toInstance(linkDao);
            bind(StudioLookupCache.class).toInstance(studioLookupCache);
            bind(ObjectMapper.class).toInstance(new ObjectMapper());
          }
        };

    var injector = Guice.createInjector(new StudiosModule(), testModule);

    var studiosRoute = injector.getInstance(StudiosRoute.class);
    var linksRoute = injector.getInstance(LinksRoute.class);
    var actorFactory = injector.getInstance(StudioActorFactory.class);
    var linkActorFactory = injector.getInstance(LinkActorFactory.class);
    var actorRef = injector.getInstance(Key.get(new TypeLiteral<ActorRef<StudioCommand>>() {}));
    var linkActorRef = injector.getInstance(Key.get(new TypeLiteral<ActorRef<LinkCommand>>() {}));
    var routeProviders = injector.getInstance(Key.get(new TypeLiteral<Set<RouteProvider>>() {}));

    assertThat(studiosRoute).isInstanceOf(StudiosRoute.class);
    assertThat(linksRoute).isInstanceOf(LinksRoute.class);
    assertThat(actorFactory).isInstanceOf(DefaultStudioActorFactory.class);
    assertThat(linkActorFactory).isInstanceOf(DefaultLinkActorFactory.class);
    assertThat(actorRef).isInstanceOf(ActorRef.class);
    assertThat(linkActorRef).isInstanceOf(ActorRef.class);
    assertThat(routeProviders)
        .hasAtLeastOneElementOfType(StudiosRoute.class)
        .hasAtLeastOneElementOfType(LinksRoute.class);
  }
}
