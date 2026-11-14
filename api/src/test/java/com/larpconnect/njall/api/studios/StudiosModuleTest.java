package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.campaigns.CampaignCommand;
import com.larpconnect.njall.api.studios.campaigns.CampaignsRoute;
import com.larpconnect.njall.api.studios.common.StudioSubRoute;
import com.larpconnect.njall.api.studios.games.GameCommand;
import com.larpconnect.njall.api.studios.games.GamesRoute;
import com.larpconnect.njall.api.studios.systems.GameSystemCommand;
import com.larpconnect.njall.api.studios.systems.SystemsRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.studios.AddressDAO;
import com.larpconnect.njall.data.dao.studios.HashtagDAO;
import com.larpconnect.njall.data.dao.studios.LinkDAO;
import com.larpconnect.njall.data.dao.studios.LocationDAO;
import com.larpconnect.njall.data.dao.studios.StudioDAO;
import com.larpconnect.njall.data.dao.studios.actors.AgentDAO;
import com.larpconnect.njall.data.dao.studios.campaigns.CampaignDAO;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import com.larpconnect.njall.data.dao.studios.events.EventDAO;
import com.larpconnect.njall.data.dao.studios.games.GameDAO;
import com.larpconnect.njall.data.dao.studios.individuals.IndividualDAO;
import com.larpconnect.njall.data.dao.studios.systems.GameSystemDAO;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.Collection;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.Directives;
import org.apache.pekko.http.javadsl.server.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class StudiosModuleTest {

  @Test
  @DisplayName("StudiosModule RouteProvider serves actor-parented collections with 201 Created")
  void routeProvider_actorParentedCollections_returnsCreated() throws Exception {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "studios-module-collections-test");
    try {
      var tenantId = UUID.randomUUID();
      var studioId = UUID.randomUUID();
      var actorId = UUID.randomUUID();
      var collectionId = UUID.randomUUID();
      var now = Instant.now();

      var daos = mockDaos();
      when(daos.collectionDao().isActiveActor(tenantId, actorId)).thenReturn(true);
      when(daos.collectionDao()
              .create(eq(tenantId), eq(actorId), eq(actorId), eq("Featured Cast"), any()))
          .thenReturn(
              new Collection(
                  collectionId,
                  actorId,
                  actorId,
                  "Featured Cast",
                  Optional.empty(),
                  0,
                  ImmutableList.of(),
                  now,
                  now,
                  Optional.empty()));

      var studioLookupCache = mock(StudioLookupCache.class);
      when(studioLookupCache.findByIdOrAlias("valiant"))
          .thenReturn(
              Optional.of(
                  new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty())));

      var injector =
          Guice.createInjector(
              new StudiosModule(), createTestModule(daos, studioLookupCache, testSystem));
      var handler = combinedRoute(injector).seal().function(testSystem);

      var request =
          HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/collections")
              .withEntity(ContentTypes.APPLICATION_JSON, "{\"name\":\"Featured Cast\"}");
      var response = handler.apply(request).toCompletableFuture().get(5, TimeUnit.SECONDS);

      assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
    } finally {
      testSystem.terminate();
      testSystem.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  @Test
  @DisplayName("StudiosModule RouteProvider returns 404 for legacy /v1/collections routes")
  void studioSubRoutes_legacyCollections_returns404() throws Exception {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "studios-subroutes-legacy-test");
    try {
      var studioLookupCache = mock(StudioLookupCache.class);
      var now = Instant.now();
      when(studioLookupCache.findByIdOrAlias("valiant"))
          .thenReturn(
              Optional.of(
                  new StudioLookup(
                      UUID.randomUUID(),
                      UUID.randomUUID(),
                      "valiant",
                      now,
                      now,
                      Optional.empty())));
      var injector =
          Guice.createInjector(
              new StudiosModule(), createTestModule(mockDaos(), studioLookupCache, testSystem));
      var handler = combinedRoute(injector).seal().function(testSystem);

      var postLegacy =
          HttpRequest.POST("/api/studios/valiant/v1/collections")
              .withEntity(ContentTypes.APPLICATION_JSON, "{\"name\":\"Legacy\"}");
      assertThat(handler.apply(postLegacy).toCompletableFuture().get(5, TimeUnit.SECONDS).status())
          .isEqualTo(StatusCodes.NOT_FOUND);

      var getLegacy = HttpRequest.GET("/api/studios/valiant/v1/collections/" + UUID.randomUUID());
      assertThat(handler.apply(getLegacy).toCompletableFuture().get(5, TimeUnit.SECONDS).status())
          .isEqualTo(StatusCodes.NOT_FOUND);
    } finally {
      testSystem.terminate();
      testSystem.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  @Test
  @DisplayName("StudiosModule wires Systems, Campaigns, and Games routes, subroutes, and actors")
  void studiosModule_wiresSystemsCampaignsGamesAndSubRoutes() throws Exception {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "studios-systems-campaigns-games-test");
    try {
      var tenantId = UUID.randomUUID();
      var daos = mockDaos();
      when(daos.gameSystemDao().listAll(tenantId)).thenReturn(ImmutableList.of());
      when(daos.campaignDao().listAll(tenantId, Optional.empty())).thenReturn(ImmutableList.of());
      when(daos.gameDao().listAll(tenantId, Optional.empty(), Optional.empty()))
          .thenReturn(ImmutableList.of());

      var injector =
          Guice.createInjector(
              new StudiosModule(), createTestModule(daos, activeCache(tenantId), testSystem));
      verifySubRoutesAndActors(injector);

      var handler = combinedRoute(injector).seal().function(testSystem);
      for (var path : ImmutableList.of("systems", "campaigns", "games")) {
        var res =
            handler
                .apply(HttpRequest.GET("/api/studios/valiant/v1/" + path))
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);
        assertThat(res.status()).isEqualTo(StatusCodes.OK);
      }
    } finally {
      testSystem.terminate();
      testSystem.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  private void verifySubRoutesAndActors(Injector injector) {
    var subRoutes =
        injector.getInstance(
            Key.get(new TypeLiteral<Set<RouteProvider>>() {}, StudioSubRoute.class));
    assertThat(subRoutes).hasSize(9);
    assertThat(subRoutes.stream().map(Object::getClass).toList())
        .contains(SystemsRoute.class, CampaignsRoute.class, GamesRoute.class);

    assertThat(injector.getInstance(Key.get(new TypeLiteral<ActorRef<GameSystemCommand>>() {})))
        .isSameAs(injector.getInstance(Key.get(new TypeLiteral<ActorRef<GameSystemCommand>>() {})));
    assertThat(injector.getInstance(Key.get(new TypeLiteral<ActorRef<CampaignCommand>>() {})))
        .isSameAs(injector.getInstance(Key.get(new TypeLiteral<ActorRef<CampaignCommand>>() {})));
    assertThat(injector.getInstance(Key.get(new TypeLiteral<ActorRef<GameCommand>>() {})))
        .isSameAs(injector.getInstance(Key.get(new TypeLiteral<ActorRef<GameCommand>>() {})));
  }

  private StudioLookupCache activeCache(UUID tenantId) {
    var cache = mock(StudioLookupCache.class);
    var now = Instant.now();
    when(cache.findByIdOrAlias("valiant"))
        .thenReturn(
            Optional.of(
                new StudioLookup(
                    tenantId, UUID.randomUUID(), "valiant", now, now, Optional.empty())));
    return cache;
  }

  private Route combinedRoute(Injector injector) {
    var routeProviders = injector.getInstance(Key.get(new TypeLiteral<Set<RouteProvider>>() {}));
    return routeProviders.stream()
        .map(RouteProvider::route)
        .reduce((first, second) -> Directives.concat(first, second))
        .orElseThrow();
  }

  private Daos mockDaos() {
    return new Daos(
        mock(StudioDAO.class),
        mock(LinkDAO.class),
        mock(LocationDAO.class),
        mock(AddressDAO.class),
        mock(HashtagDAO.class),
        mock(EventDAO.class),
        mock(GameSystemDAO.class),
        mock(CampaignDAO.class),
        mock(GameDAO.class),
        mock(IndividualDAO.class),
        mock(CollectionDAO.class),
        mock(AgentDAO.class));
  }

  private record Daos(
      StudioDAO studioDao,
      LinkDAO linkDao,
      LocationDAO locationDao,
      AddressDAO addressDao,
      HashtagDAO hashtagDao,
      EventDAO eventDao,
      GameSystemDAO gameSystemDao,
      CampaignDAO campaignDao,
      GameDAO gameDao,
      IndividualDAO individualDao,
      CollectionDAO collectionDao,
      AgentDAO agentDao) {}

  private Module createTestModule(
      Daos daos, StudioLookupCache studioLookupCache, ActorSystem<Void> actorSystem) {
    var mapper =
        JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(StudioDAO.class).toInstance(daos.studioDao());
        bind(LinkDAO.class).toInstance(daos.linkDao());
        bind(LocationDAO.class).toInstance(daos.locationDao());
        bind(AddressDAO.class).toInstance(daos.addressDao());
        bind(HashtagDAO.class).toInstance(daos.hashtagDao());
        bind(EventDAO.class).toInstance(daos.eventDao());
        bind(GameSystemDAO.class).toInstance(daos.gameSystemDao());
        bind(CampaignDAO.class).toInstance(daos.campaignDao());
        bind(GameDAO.class).toInstance(daos.gameDao());
        bind(IndividualDAO.class).toInstance(daos.individualDao());
        bind(CollectionDAO.class).toInstance(daos.collectionDao());
        bind(AgentDAO.class).toInstance(daos.agentDao());
        bind(StudioLookupCache.class).toInstance(studioLookupCache);
        bind(ObjectMapper.class).toInstance(mapper);
        bind(Key.get(new TypeLiteral<ActorSystem<Void>>() {})).toInstance(actorSystem);
        bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
      }
    };
  }
}
