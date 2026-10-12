package com.larpconnect.njall.api.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionsRouteTest {

  private static final String CREATE_COLLECTION_BODY =
      "{\"ownerId\":\"019273c5-9271-7000-8000-000000000001\",\"name\":\"Featured Cast\"}";
  private static final String PATCH_COLLECTION_BODY = "{\"name\":\"Updated Cast\"}";
  private static final String ADD_ITEM_BODY =
      "{\"refersToId\":\"019273c5-9271-7000-8000-000000000002\"}";
  private static final String REMOVE_ITEM_BODY =
      "{\"refersToId\":\"019273c5-9271-7000-8000-000000000002\"}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID collectionId = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "collections-route-test");
    objectMapper =
        JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static HttpResponse executeRequest(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, HttpRequest request)
      throws Exception {
    return handler.apply(request).toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private StudioLookup sampleLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  private StudioLookup sampleDeletedLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.of(now));
  }

  private OrderedCollectionResponse sampleCollectionResponse() {
    return new OrderedCollectionResponse(
        collectionId,
        "OrderedCollection",
        "Featured Cast",
        Optional.of("Primary cast"),
        ownerId,
        0,
        ImmutableList.of(),
        now,
        now);
  }

  private ActorRef<CollectionCommand> createCollectionActor(CollectionActorResponse response) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              switch (msg) {
                case CollectionCommand.CreateCollection c -> c.replyTo().tell(response);
                case CollectionCommand.GetCollection g -> g.replyTo().tell(response);
                case CollectionCommand.PatchCollection p -> p.replyTo().tell(response);
                case CollectionCommand.DeleteCollection d -> d.replyTo().tell(response);
                case CollectionCommand.MutateItem m -> m.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testCollectionActor" + UUID.randomUUID(),
        Props.empty());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache, ActorRef<CollectionCommand> actor) {
    return new CollectionsRoute(cache, actor, system, objectMapper).route().seal().function(system);
  }

  @Test
  @DisplayName("POST /collections returns 201 on success")
  void postCollection_success_returns201() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/collections")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_COLLECTION_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("POST /collections returns 409 on conflict")
  void postCollection_conflict_returns409() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.conflict("Duplicate name"));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/collections")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_COLLECTION_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.CONFLICT);
  }

  @Test
  @DisplayName("GET /collections returns 404 (listing not supported)")
  void getCollectionsList_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/collections");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /collections/{id} returns 200 OK")
  void getCollection_success_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/collections/" + collectionId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /collections/{id}?form=expanded returns 200 OK")
  void getCollection_expanded_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.GET("/api/studios/valiant/v1/collections/" + collectionId + "?form=expanded");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("PATCH /collections/{id} returns 200 OK")
  void patchCollection_success_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.PATCH("/api/studios/valiant/v1/collections/" + collectionId)
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_COLLECTION_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("DELETE /collections/{id} returns 204 No Content")
  void deleteCollection_success_returns204() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var request = HttpRequest.DELETE("/api/studios/valiant/v1/collections/" + collectionId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("POST /collections/{id}:addItem returns 204 No Content")
  void addItem_colonCustomMethod_returns204() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/collections/" + collectionId + ":addItem")
            .withEntity(ContentTypes.APPLICATION_JSON, ADD_ITEM_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("POST /collections/{id}:removeItem returns 204 No Content")
  void removeItem_colonCustomMethod_returns204() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/collections/" + collectionId + ":removeItem")
            .withEntity(ContentTypes.APPLICATION_JSON, REMOVE_ITEM_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("GET /collections/{id} returns 404 for unknown studio")
  void unknownStudio_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());
    var actor = createCollectionActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/unknown/v1/collections/" + collectionId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /collections/{id} returns 404 for soft-deleted studio")
  void deletedStudio_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleDeletedLookup()));
    var actor = createCollectionActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/collections/" + collectionId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /collections/{id} returns 404 for invalid UUID")
  void invalidCollectionUuid_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/collections/not-a-uuid");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("POST /collections/{id}:addItem returns 400 on invalid payload")
  void addItem_invalidPayload_returns400() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/collections/" + collectionId + ":addItem")
            .withEntity(
                ContentTypes.APPLICATION_JSON,
                "{\"refersToId\":\"019273c5-9271-7000-8000-000000000002\",\"index\":-1}");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /collections/{id}:addItem returns 404 for invalid UUID")
  void addItem_invalidUuid_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/collections/not-a-uuid:addItem")
            .withEntity(ContentTypes.APPLICATION_JSON, ADD_ITEM_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("POST /collections/{id}:removeItem returns 404 for invalid UUID")
  void removeItem_invalidUuid_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var actor = createCollectionActor(CollectionActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/collections/not-a-uuid:removeItem")
            .withEntity(ContentTypes.APPLICATION_JSON, REMOVE_ITEM_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /collections/{id} returns 500 when actor fails")
  void actorFailure_returns500() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    ActorRef<CollectionCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                (CollectionCommand msg) -> {
                  throw new IllegalStateException("Simulated actor crash");
                }),
            "failingActor" + UUID.randomUUID(),
            Props.empty());
    var handler =
        new CollectionsRoute(cache, actor, system, objectMapper, Duration.ofMillis(200))
            .route()
            .seal()
            .function(system);

    var request = HttpRequest.GET("/api/studios/valiant/v1/collections/" + collectionId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
