package com.larpconnect.njall.api.studios.collections;

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
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import com.larpconnect.njall.data.domain.studios.Collection;
import java.time.Instant;
import java.util.Optional;
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
import org.apache.pekko.http.javadsl.server.Directives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.japi.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionsModuleTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID collectionId = UUID.randomUUID();

  @Test
  @DisplayName("CollectionsModule wires CollectionsRoute and CollectionActor for POST /collections")
  void collectionsRoute_postCollection_returnsCreated() throws Exception {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "collections-module-post-test");
    try {
      var collectionDao = mock(CollectionDAO.class);
      var now = Instant.now();
      when(collectionDao.isActiveActor(tenantId, actorId)).thenReturn(true);
      when(collectionDao.create(eq(tenantId), eq(actorId), eq(actorId), eq("Featured Cast"), any()))
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

      var handler = createHandler(collectionDao, testSystem);
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
  @DisplayName(
      "CollectionsModule wires CollectionsRoute and CollectionActor for GET missing collection")
  void collectionsRoute_getMissingCollection_returnsNotFound() throws Exception {
    ActorSystem<Void> testSystem =
        ActorSystem.create(Behaviors.empty(), "collections-module-get-test");
    try {
      var collectionDao = mock(CollectionDAO.class);
      when(collectionDao.findById(tenantId, actorId, collectionId)).thenReturn(Optional.empty());

      var handler = createHandler(collectionDao, testSystem);
      var request =
          HttpRequest.GET(
              "/api/studios/valiant/v1/actors/" + actorId + "/collections/" + collectionId);
      var response = handler.apply(request).toCompletableFuture().get(5, TimeUnit.SECONDS);

      assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
    } finally {
      testSystem.terminate();
      testSystem.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      CollectionDAO collectionDao, ActorSystem<Void> actorSystem) {
    var injector =
        Guice.createInjector(new CollectionsModule(), createTestModule(collectionDao, actorSystem));
    var collectionsRoute = injector.getInstance(CollectionsRoute.class);
    return Directives.pathPrefix(
            PathMatchers.separateOnSlashes(
                "api/studios/valiant/v1/actors/" + actorId + "/collections"),
            () -> collectionsRoute.collectionsSubroutes(tenantId, "valiant", actorId))
        .seal()
        .function(actorSystem);
  }

  private Module createTestModule(CollectionDAO collectionDao, ActorSystem<Void> actorSystem) {
    var mapper =
        JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(CollectionDAO.class).toInstance(collectionDao);
        bind(ObjectMapper.class).toInstance(mapper);
        bind(Key.get(new TypeLiteral<ActorSystem<Void>>() {})).toInstance(actorSystem);
        bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
      }
    };
  }
}
