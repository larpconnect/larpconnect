package com.larpconnect.njall.api.studios.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioSubRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.studios.events.EventDAO;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.Event;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
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

final class EventsModuleTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID eventId = UUID.randomUUID();

  @Test
  @DisplayName("EventsModule registers StudioSubRoute multibinding and handles POST eventType")
  void eventsModule_wiresMultibindingAndCreateRoute() throws Exception {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "events-module-create-test");
    try {
      var eventDao = mock(EventDAO.class);
      var cache = mockActiveCache();
      when(eventDao.create(
              eq(tenantId),
              eq(actorId),
              eq(EventType.WORKSHOP),
              eq("Prop Making"),
              eq(Optional.empty()),
              eq(Optional.empty()),
              eq(Optional.empty()),
              eq(Optional.empty())))
          .thenReturn(sampleWorkshopEvent());

      var injector = createInjector(eventDao, cache, testSystem);
      var subRoutes =
          injector.getInstance(
              Key.get(new TypeLiteral<Set<RouteProvider>>() {}, StudioSubRoute.class));
      assertThat(subRoutes).hasSize(1);

      var handler = createHandler(injector, testSystem);
      var body =
          "{\"generator\":\""
              + actorId
              + "\",\"title\":\"Prop Making\",\"eventType\":\"WORKSHOP\"}";
      var res =
          execute(
              handler,
              HttpRequest.POST("/api/studios/valiant/v1/events")
                  .withEntity(ContentTypes.APPLICATION_JSON, body));
      assertThat(res.status()).isEqualTo(StatusCodes.CREATED);
      assertThat(readBody(res, testSystem))
          .contains("\"eventType\":\"WORKSHOP\"", "/api/studios/valiant/v1/actors/" + actorId);
    } finally {
      testSystem.terminate();
      testSystem.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  private Event sampleWorkshopEvent() {
    var now = Instant.now();
    return new Event(
        eventId,
        actorId,
        EventType.WORKSHOP,
        Optional.empty(),
        "Prop Making",
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        now,
        now,
        Optional.empty());
  }

  @Test
  @DisplayName("EventsModule rejects GAME on POST and returns 404 for excluded Game IDs on GET")
  void eventsModule_rejectsGameCreateAndReturns404ForGameId() throws Exception {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "events-module-game-guard-test");
    try {
      var eventDao = mock(EventDAO.class);
      when(eventDao.findById(tenantId, eventId)).thenReturn(Optional.empty());
      var injector = createInjector(eventDao, mockActiveCache(), testSystem);
      var handler = createHandler(injector, testSystem);

      var gameBody =
          "{\"generator\":\"" + actorId + "\",\"title\":\"Game\",\"eventType\":\"GAME\"}";
      var createRes =
          execute(
              handler,
              HttpRequest.POST("/api/studios/valiant/v1/events")
                  .withEntity(ContentTypes.APPLICATION_JSON, gameBody));
      assertThat(createRes.status()).isEqualTo(StatusCodes.BAD_REQUEST);
      assertThat(readBody(createRes, testSystem)).contains("\"code\":400");

      var getRes = execute(handler, HttpRequest.GET("/api/studios/valiant/v1/events/" + eventId));
      assertThat(getRes.status()).isEqualTo(StatusCodes.NOT_FOUND);
    } finally {
      testSystem.terminate();
      testSystem.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  private StudioLookupCache mockActiveCache() {
    var cache = mock(StudioLookupCache.class);
    var now = Instant.now();
    when(cache.findByIdOrAlias("valiant"))
        .thenReturn(
            Optional.of(
                new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty())));
    return cache;
  }

  private Injector createInjector(
      EventDAO eventDao, StudioLookupCache cache, ActorSystem<Void> actorSystem) {
    return Guice.createInjector(new EventsModule(), createTestModule(eventDao, cache, actorSystem));
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      Injector injector, ActorSystem<Void> actorSystem) {
    return injector.getInstance(EventsRoute.class).route().seal().function(actorSystem);
  }

  private Module createTestModule(
      EventDAO eventDao, StudioLookupCache cache, ActorSystem<Void> actorSystem) {
    var mapper =
        JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(EventDAO.class).toInstance(eventDao);
        bind(StudioLookupCache.class).toInstance(cache);
        bind(ObjectMapper.class).toInstance(mapper);
        bind(Key.get(new TypeLiteral<ActorSystem<Void>>() {})).toInstance(actorSystem);
        bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
      }
    };
  }

  private static HttpResponse execute(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, HttpRequest request)
      throws Exception {
    return handler.apply(request).toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static String readBody(HttpResponse response, ActorSystem<Void> actorSystem)
      throws Exception {
    return response
        .entity()
        .toStrict(1000, actorSystem)
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS)
        .getData()
        .utf8String();
  }
}
