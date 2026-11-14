package com.larpconnect.njall.api.studios.games;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioSubRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.studios.games.GameDAO;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.Game;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.japi.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class GamesModuleTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final UUID gameId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-06-01T18:00:00Z");

  private static HttpResponse send(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, HttpRequest request)
      throws Exception {
    return handler.apply(request).toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static void terminateSystem(ActorSystem<Void> system) throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static Injector createInjector(
      GameDAO gameDao, StudioLookupCache cache, ActorSystem<Void> system) {
    return Guice.createInjector(new GamesModule(), createSupportModule(gameDao, cache, system));
  }

  private static Module createSupportModule(
      GameDAO gameDao, StudioLookupCache cache, ActorSystem<Void> system) {
    var mapper =
        JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    return (Binder binder) -> {
      binder.bind(new TypeLiteral<ActorSystem<Void>>() {}).toInstance(system);
      binder.bind(StudioLookupCache.class).toInstance(cache);
      binder.bind(ObjectMapper.class).toInstance(mapper);
      binder.bind(GameDAO.class).toInstance(gameDao);
      binder.bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
      Multibinder.newSetBinder(binder, RouteProvider.class, StudioSubRoute.class);
    };
  }

  private StudioLookup activeLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  private Game sampleGame() {
    return new Game(
        gameId,
        actorId,
        systemId,
        Optional.empty(),
        Optional.empty(),
        "Chronicles of Dusk",
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        now,
        now,
        Optional.empty());
  }

  @Test
  @DisplayName("GamesModule binds singleton route, sub-route, actor factory, and game actor ref")
  void gamesModule_configuresAllExpectedBindings() throws Exception {
    var system = ActorSystem.<Void>create(Behaviors.empty(), "games-module-test");
    try {
      var injector = createInjector(mock(GameDAO.class), mock(StudioLookupCache.class), system);

      var route1 = injector.getInstance(GamesRoute.class);
      var route2 = injector.getInstance(GamesRoute.class);
      assertThat(route1).isSameAs(route2);

      var subRoutesKey = Key.get(new TypeLiteral<Set<RouteProvider>>() {}, StudioSubRoute.class);
      var subRoutes = injector.getInstance(subRoutesKey);
      assertThat(subRoutes).containsExactly(route1);

      var factory = injector.getInstance(GameActorFactory.class);
      assertThat(factory).isInstanceOf(DefaultGameActorFactory.class);

      var actorKey = Key.get(new TypeLiteral<ActorRef<GameCommand>>() {});
      var actor1 = injector.getInstance(actorKey);
      var actor2 = injector.getInstance(actorKey);
      assertThat(actor1).isSameAs(actor2);
      assertThat(actor1.path().name()).isEqualTo("gameActor");
    } finally {
      terminateSystem(system);
    }
  }

  private GameDAO mockCrudGameDao() {
    var gameDao = mock(GameDAO.class);
    var domain = sampleGame();
    when(gameDao.create(eq(tenantId), any())).thenReturn(domain);
    when(gameDao.listAll(tenantId, Optional.empty(), Optional.empty()))
        .thenReturn(ImmutableList.of(domain));
    when(gameDao.patch(eq(tenantId), eq(gameId), any())).thenReturn(Optional.of(domain));
    when(gameDao.softDelete(tenantId, gameId)).thenReturn(true);
    return gameDao;
  }

  @Test
  @DisplayName("GamesModule wires GamesRoute and GameActor end-to-end across CRUD endpoints")
  void gamesModule_executesCrudAgainstWiredActor() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var system = ActorSystem.<Void>create(Behaviors.empty(), "games-module-crud-test");
    try {
      var handler =
          createInjector(mockCrudGameDao(), cache, system)
              .getInstance(GamesRoute.class)
              .route()
              .seal()
              .function(system);
      var postBody =
          "{\"generator\":\""
              + actorId
              + "\",\"system\":\""
              + systemId
              + "\",\"title\":\"Chronicles of Dusk\"}";
      var baseUrl = "/api/studios/valiant/v1/games";
      var itemUrl = baseUrl + "/" + gameId;
      var postReq = HttpRequest.POST(baseUrl).withEntity(ContentTypes.APPLICATION_JSON, postBody);
      var patchReq =
          HttpRequest.PATCH(itemUrl + "?update_mask=summary")
              .withEntity(ContentTypes.APPLICATION_JSON, "{\"title\":null,\"summary\":null}");

      assertThat(send(handler, postReq).status()).isEqualTo(StatusCodes.CREATED);
      assertThat(send(handler, HttpRequest.GET(baseUrl)).status()).isEqualTo(StatusCodes.OK);
      assertThat(send(handler, patchReq).status()).isEqualTo(StatusCodes.OK);
      assertThat(send(handler, HttpRequest.DELETE(itemUrl)).status())
          .isEqualTo(StatusCodes.NO_CONTENT);
    } finally {
      terminateSystem(system);
    }
  }
}
