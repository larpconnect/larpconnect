package com.larpconnect.njall.api.studios.systems;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.larpconnect.njall.api.studios.common.StudioSubRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.studios.systems.GameSystemDAO;
import com.larpconnect.njall.data.dao.studios.systems.PatchGameSystemParams;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.GameSystem;
import java.sql.SQLException;
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
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class SystemsModuleTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final Instant now = Instant.now();

  private static HttpResponse send(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, HttpRequest request)
      throws Exception {
    return handler.apply(request).toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static void terminateSystem(ActorSystem<Void> testSystem) throws Exception {
    testSystem.terminate();
    testSystem.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private StudioLookup activeLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  @Test
  @DisplayName("SystemsModule binds singleton SystemsRoute into StudioSubRoute multibinder")
  void configure_bindsRouteFactoryAndActorRef() throws Exception {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "systems-module-bindings-test");
    try {
      var injector =
          createInjector(mock(GameSystemDAO.class), mock(StudioLookupCache.class), testSystem);

      var routeFirst = injector.getInstance(SystemsRoute.class);
      var routeSecond = injector.getInstance(SystemsRoute.class);
      var factory = injector.getInstance(GameSystemActorFactory.class);
      var actorKey = Key.get(new TypeLiteral<ActorRef<GameSystemCommand>>() {});
      var actorFirst = injector.getInstance(actorKey);
      var actorSecond = injector.getInstance(actorKey);
      var subRoutes =
          injector.getInstance(
              Key.get(new TypeLiteral<Set<RouteProvider>>() {}, StudioSubRoute.class));

      assertThat(routeFirst).isSameAs(routeSecond);
      assertThat(factory).isInstanceOf(DefaultGameSystemActorFactory.class);
      assertThat(actorFirst).isSameAs(actorSecond);
      assertThat(actorFirst.path().name()).isEqualTo("gameSystemActor");
      assertThat(subRoutes).containsExactly(routeFirst);
    } finally {
      terminateSystem(testSystem);
    }
  }

  @Test
  @DisplayName("SystemsModule wires create, list, and conflict handling end-to-end")
  void routeProvider_executesCreateListAndConflictAgainstWiredActor() throws Exception {
    var dao = mock(GameSystemDAO.class);
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var domain =
        new GameSystem(
            systemId, actorId, "Daggerheart", Optional.of("Fantasy"), now, now, Optional.empty());
    when(dao.create(tenantId, actorId, "Daggerheart", Optional.of("Fantasy"))).thenReturn(domain);
    when(dao.create(tenantId, actorId, "Duplicate", Optional.empty()))
        .thenThrow(
            new ConstraintViolationException(
                "Duplicate", new SQLException("dup", "23505"), "idx_systems_tenant_name"));
    when(dao.listAll(tenantId)).thenReturn(ImmutableList.of(domain));

    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "systems-module-create-test");
    try {
      var handler =
          createInjector(dao, cache, testSystem)
              .getInstance(SystemsRoute.class)
              .route()
              .seal()
              .function(testSystem);
      var postBody =
          "{\"generator\":\"" + actorId + "\",\"name\":\"Daggerheart\",\"summary\":\"Fantasy\"}";
      var dupBody = "{\"generator\":\"" + actorId + "\",\"name\":\"Duplicate\"}";

      assertThat(
              send(
                      handler,
                      HttpRequest.POST("/api/studios/valiant/v1/systems")
                          .withEntity(ContentTypes.APPLICATION_JSON, postBody))
                  .status())
          .isEqualTo(StatusCodes.CREATED);
      assertThat(
              send(
                      handler,
                      HttpRequest.POST("/api/studios/valiant/v1/systems")
                          .withEntity(ContentTypes.APPLICATION_JSON, dupBody))
                  .status())
          .isEqualTo(StatusCodes.CONFLICT);
      assertThat(send(handler, HttpRequest.GET("/api/studios/valiant/v1/systems")).status())
          .isEqualTo(StatusCodes.OK);
    } finally {
      terminateSystem(testSystem);
    }
  }

  @Test
  @DisplayName("SystemsModule wires patch clear-summary and delete end-to-end")
  void routeProvider_executesPatchAndDeleteAgainstWiredActor() throws Exception {
    var dao = mock(GameSystemDAO.class);
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var cleared =
        new GameSystem(
            systemId, actorId, "Daggerheart", Optional.empty(), now, now, Optional.empty());
    var params = new PatchGameSystemParams(Optional.empty(), Optional.empty(), true);
    when(dao.patch(tenantId, systemId, params)).thenReturn(Optional.of(cleared));
    when(dao.softDelete(tenantId, systemId)).thenReturn(true);

    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "systems-module-patch-test");
    try {
      var handler =
          createInjector(dao, cache, testSystem)
              .getInstance(SystemsRoute.class)
              .route()
              .seal()
              .function(testSystem);
      var patchUrl = "/api/studios/valiant/v1/systems/" + systemId + "?update_mask=summary";
      assertThat(
              send(
                      handler,
                      HttpRequest.PATCH(patchUrl)
                          .withEntity(ContentTypes.APPLICATION_JSON, "{\"summary\":null}"))
                  .status())
          .isEqualTo(StatusCodes.OK);
      var deleteUrl = "/api/studios/valiant/v1/systems/" + systemId;
      assertThat(send(handler, HttpRequest.DELETE(deleteUrl)).status())
          .isEqualTo(StatusCodes.NO_CONTENT);
    } finally {
      terminateSystem(testSystem);
    }
  }

  private Injector createInjector(
      GameSystemDAO dao, StudioLookupCache cache, ActorSystem<Void> testSystem) {
    return Guice.createInjector(new SystemsModule(), createTestModule(dao, cache, testSystem));
  }

  private Module createTestModule(
      GameSystemDAO dao, StudioLookupCache cache, ActorSystem<Void> testSystem) {
    var mapper =
        JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(GameSystemDAO.class).toInstance(dao);
        bind(StudioLookupCache.class).toInstance(cache);
        bind(ObjectMapper.class).toInstance(mapper);
        bind(Key.get(new TypeLiteral<ActorSystem<Void>>() {})).toInstance(testSystem);
        bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
      }
    };
  }
}
