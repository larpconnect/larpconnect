package com.larpconnect.njall.api.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionsModuleTest {

  private static ActorSystem<Void> system;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "collections-module-test");
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName(
      "CollectionsModule binds CollectionsRoute, CollectionActorFactory, and CollectionActor")
  void configure_bindsExpectedDependencies() {
    var collectionDao = mock(CollectionDAO.class);
    var studioLookupCache = mock(StudioLookupCache.class);

    var testModule = createTestModule(collectionDao, studioLookupCache);
    var injector = Guice.createInjector(new CollectionsModule(), testModule);

    var collectionsRoute = injector.getInstance(CollectionsRoute.class);
    var factory = injector.getInstance(CollectionActorFactory.class);
    var actor = injector.getInstance(Key.get(new TypeLiteral<ActorRef<CollectionCommand>>() {}));

    assertThat(collectionsRoute).isInstanceOf(CollectionsRoute.class);
    assertThat(factory).isInstanceOf(DefaultCollectionActorFactory.class);
    assertThat(actor).isInstanceOf(ActorRef.class);
  }

  @Test
  @DisplayName("provideCollectionActor directly returns system actor from factory")
  void provideCollectionActor_returnsActor() {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "provide-collection-actor-test");
    try {
      var collectionDao = mock(CollectionDAO.class);
      var factory = new DefaultCollectionActorFactory(collectionDao);
      var module = new CollectionsModule();
      var actor = module.provideCollectionActor(testSystem, factory, Props.empty());

      assertThat(actor).isNotNull();
    } finally {
      testSystem.terminate();
    }
  }

  private Module createTestModule(
      CollectionDAO collectionDao, StudioLookupCache studioLookupCache) {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(CollectionDAO.class).toInstance(collectionDao);
        bind(StudioLookupCache.class).toInstance(studioLookupCache);
        bind(ObjectMapper.class).toInstance(new ObjectMapper());
        bind(Key.get(new TypeLiteral<ActorSystem<Void>>() {})).toInstance(system);
        bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
      }
    };
  }
}
