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
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import com.larpconnect.njall.data.domain.studios.Collection;
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
import org.apache.pekko.http.javadsl.server.Directives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.japi.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionsRouteTest {

  private static final String CREATE_OMITTED_OWNER_BODY = "{\"name\":\"Featured Cast\"}";
  private static final String CREATE_WITH_OWNER_BODY =
      "{\"ownerId\":\"019273c5-9271-7000-8000-000000000001\",\"name\":\"Featured Cast\"}";
  private static final String CREATE_WITH_GENERATOR_BODY =
      "{\"generator\":\"019273c5-9271-7000-8000-000000000001\",\"name\":\"Featured Cast\"}";
  private static final String PATCH_COLLECTION_BODY = "{\"name\":\"Updated Cast\"}";
  private static final String ADD_ITEM_BODY =
      "{\"refersToId\":\"019273c5-9271-7000-8000-000000000002\"}";
  private static final String REMOVE_ITEM_BODY =
      "{\"refersToId\":\"019273c5-9271-7000-8000-000000000002\"}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID collectionId = UUID.randomUUID();
  private final UUID ownerId = UUID.fromString("019273c5-9271-7000-8000-000000000001");
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

  private String collectionsBasePath() {
    return "/api/studios/valiant/v1/actors/" + actorId + "/collections";
  }

  private OrderedCollectionResponse sampleCollectionResponse() {
    return new OrderedCollectionResponse(
        collectionId,
        "OrderedCollection",
        "Featured Cast",
        Optional.of("Primary cast"),
        "/api/studios/valiant/v1/actors/" + actorId,
        ownerId,
        0,
        ImmutableList.of(),
        now,
        now);
  }

  private ActorRef<CollectionCommand> createStubActor(CollectionActorResponse response) {
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
      ActorRef<CollectionCommand> actor) {
    return createHandler(actor, Duration.ofSeconds(20));
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      ActorRef<CollectionCommand> actor, Duration timeout) {
    var collectionsRoute = new CollectionsRoute(actor, system, objectMapper, timeout);
    var mounted =
        Directives.pathPrefix(
            PathMatchers.separateOnSlashes(
                "api/studios/valiant/v1/actors/" + actorId + "/collections"),
            () -> collectionsRoute.collectionsSubroutes(tenantId, "valiant", actorId));
    return mounted.seal().function(system);
  }

  @Test
  @DisplayName("POST /collections returns 201 when ownerId omitted or explicit")
  void postCollection_omittedAndExplicitOwner_returns201() throws Exception {
    var actor = createStubActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(actor);

    var omittedReq =
        HttpRequest.POST(collectionsBasePath())
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_OMITTED_OWNER_BODY);
    assertThat(executeRequest(handler, omittedReq).status()).isEqualTo(StatusCodes.CREATED);

    var explicitReq =
        HttpRequest.POST(collectionsBasePath())
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_WITH_OWNER_BODY);
    assertThat(executeRequest(handler, explicitReq).status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("POST /collections returns 400 when unknown generator property is sent")
  void postCollection_withGeneratorField_returns400() throws Exception {
    var actor = createStubActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(actor);

    var request =
        HttpRequest.POST(collectionsBasePath())
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_WITH_GENERATOR_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /collections returns 409 on conflict")
  void postCollection_conflict_returns409() throws Exception {
    var actor = createStubActor(CollectionActorResponse.conflict("Duplicate name"));
    var handler = createHandler(actor);

    var request =
        HttpRequest.POST(collectionsBasePath())
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_WITH_OWNER_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.CONFLICT);
  }

  @Test
  @DisplayName("GET /collections returns 404 (listing not supported, not 405)")
  void getCollectionsList_returns404() throws Exception {
    var actor = createStubActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(actor);

    var request = HttpRequest.GET(collectionsBasePath());
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
    var body =
        response.entity().toStrict(5000, system).toCompletableFuture().get().getData().utf8String();
    assertThat(body).contains("Listing collections is not supported");
  }

  @Test
  @DisplayName("GET /collections/{id} and ?form=expanded return 200 OK")
  void getCollection_success_returns200() throws Exception {
    var actor = createStubActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(actor);

    assertThat(
            executeRequest(handler, HttpRequest.GET(collectionsBasePath() + "/" + collectionId))
                .status())
        .isEqualTo(StatusCodes.OK);
    assertThat(
            executeRequest(
                    handler,
                    HttpRequest.GET(collectionsBasePath() + "/" + collectionId + "?form=expanded"))
                .status())
        .isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("PATCH /collections/{id} returns 200 OK")
  void patchCollection_success_returns200() throws Exception {
    var actor = createStubActor(CollectionActorResponse.success(sampleCollectionResponse()));
    var handler = createHandler(actor);

    var request =
        HttpRequest.PATCH(collectionsBasePath() + "/" + collectionId)
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_COLLECTION_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("DELETE /collections/{id} returns 204 No Content")
  void deleteCollection_success_returns204() throws Exception {
    var actor = createStubActor(CollectionActorResponse.noContent());
    var handler = createHandler(actor);

    var request = HttpRequest.DELETE(collectionsBasePath() + "/" + collectionId);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("POST /collections/{id}:addItem and :removeItem return 204 No Content")
  void mutateItem_colonCustomMethod_returns204() throws Exception {
    var actor = createStubActor(CollectionActorResponse.noContent());
    var handler = createHandler(actor);

    var addReq =
        HttpRequest.POST(collectionsBasePath() + "/" + collectionId + ":addItem")
            .withEntity(ContentTypes.APPLICATION_JSON, ADD_ITEM_BODY);
    assertThat(executeRequest(handler, addReq).status()).isEqualTo(StatusCodes.NO_CONTENT);

    var removeReq =
        HttpRequest.POST(collectionsBasePath() + "/" + collectionId + ":removeItem")
            .withEntity(ContentTypes.APPLICATION_JSON, REMOVE_ITEM_BODY);
    assertThat(executeRequest(handler, removeReq).status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("Invalid collection UUIDs return 404 across GET, :addItem, and :removeItem")
  void invalidCollectionUuid_returns404() throws Exception {
    var actor = createStubActor(CollectionActorResponse.noContent());
    var handler = createHandler(actor);

    assertThat(
            executeRequest(handler, HttpRequest.GET(collectionsBasePath() + "/not-a-uuid"))
                .status())
        .isEqualTo(StatusCodes.NOT_FOUND);

    var addReq =
        HttpRequest.POST(collectionsBasePath() + "/not-a-uuid:addItem")
            .withEntity(ContentTypes.APPLICATION_JSON, ADD_ITEM_BODY);
    assertThat(executeRequest(handler, addReq).status()).isEqualTo(StatusCodes.NOT_FOUND);

    var removeReq =
        HttpRequest.POST(collectionsBasePath() + "/not-a-uuid:removeItem")
            .withEntity(ContentTypes.APPLICATION_JSON, REMOVE_ITEM_BODY);
    assertThat(executeRequest(handler, removeReq).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("POST /collections/{id}:addItem returns 400 on invalid payload")
  void addItem_invalidPayload_returns400() throws Exception {
    var actor = createStubActor(CollectionActorResponse.noContent());
    var handler = createHandler(actor);

    var request =
        HttpRequest.POST(collectionsBasePath() + "/" + collectionId + ":addItem")
            .withEntity(
                ContentTypes.APPLICATION_JSON,
                "{\"refersToId\":\"019273c5-9271-7000-8000-000000000002\",\"index\":-1}");
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("GET /collections/{id} returns 500 when actor fails")
  void actorFailure_returns500() throws Exception {
    ActorRef<CollectionCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                (CollectionCommand msg) -> {
                  throw new IllegalStateException("Simulated actor crash");
                }),
            "failingActor" + UUID.randomUUID(),
            Props.empty());
    var handler = createHandler(actor, Duration.ofMillis(200));

    var request = HttpRequest.GET(collectionsBasePath() + "/" + collectionId);
    assertThat(executeRequest(handler, request).status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  private ActorRef<CollectionCommand> spawnRealActor(CollectionDAO dao) {
    return system.systemActorOf(
        Behaviors.setup(ctx -> new CollectionActor(ctx, dao)),
        "realColActor" + UUID.randomUUID(),
        Props.empty());
  }

  @Test
  @DisplayName("POST /collections with CollectionActor validates active and missing ownerId")
  void routeWithRealActor_ownerRules() throws Exception {
    var dao = mock(CollectionDAO.class);
    var entityOwnerId = UUID.randomUUID();
    var missingOwnerId = UUID.randomUUID();
    when(dao.isActiveActor(tenantId, actorId)).thenReturn(true);
    when(dao.create(eq(tenantId), eq(actorId), eq(entityOwnerId), eq("Gallery"), any()))
        .thenReturn(
            new Collection(
                collectionId,
                actorId,
                entityOwnerId,
                "Gallery",
                Optional.empty(),
                0,
                ImmutableList.of(),
                now,
                now,
                Optional.empty()));
    when(dao.create(eq(tenantId), eq(actorId), eq(missingOwnerId), eq("Bad"), any()))
        .thenThrow(new IllegalArgumentException("Owner entity not found"));
    when(dao.create(eq(tenantId), eq(actorId), eq(entityOwnerId), eq("Duplicate"), any()))
        .thenThrow(
            new IllegalStateException("Collection with name 'Duplicate' already exists for owner"));

    var handler = createHandler(spawnRealActor(dao));
    var validReq =
        HttpRequest.POST(collectionsBasePath())
            .withEntity(
                ContentTypes.APPLICATION_JSON,
                "{\"ownerId\":\"" + entityOwnerId + "\",\"name\":\"Gallery\"}");
    assertThat(executeRequest(handler, validReq).status()).isEqualTo(StatusCodes.CREATED);

    var missingReq =
        HttpRequest.POST(collectionsBasePath())
            .withEntity(
                ContentTypes.APPLICATION_JSON,
                "{\"ownerId\":\"" + missingOwnerId + "\",\"name\":\"Bad\"}");
    assertThat(executeRequest(handler, missingReq).status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var duplicateReq =
        HttpRequest.POST(collectionsBasePath())
            .withEntity(
                ContentTypes.APPLICATION_JSON,
                "{\"ownerId\":\"" + entityOwnerId + "\",\"name\":\"Duplicate\"}");
    assertThat(executeRequest(handler, duplicateReq).status()).isEqualTo(StatusCodes.CONFLICT);
  }

  @Test
  @DisplayName("CollectionsRoute with CollectionActor returns 404 for inactive or mismatched actor")
  void routeWithRealActor_missingOrMismatchedActorReturns404() throws Exception {
    var dao = mock(CollectionDAO.class);
    var otherActorId = UUID.randomUUID();
    when(dao.isActiveActor(tenantId, otherActorId)).thenReturn(false);
    when(dao.findById(tenantId, otherActorId, collectionId)).thenReturn(Optional.empty());

    var otherCollectionsRoute = new CollectionsRoute(spawnRealActor(dao), system, objectMapper);
    var otherHandler =
        Directives.pathPrefix(
                PathMatchers.separateOnSlashes(
                    "api/studios/valiant/v1/actors/" + otherActorId + "/collections"),
                () -> otherCollectionsRoute.collectionsSubroutes(tenantId, "valiant", otherActorId))
            .seal()
            .function(system);

    var createMissingActor =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + otherActorId + "/collections")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_OMITTED_OWNER_BODY);
    assertThat(executeRequest(otherHandler, createMissingActor).status())
        .isEqualTo(StatusCodes.NOT_FOUND);
    assertThat(
            executeRequest(
                    otherHandler,
                    HttpRequest.GET(
                        "/api/studios/valiant/v1/actors/"
                            + otherActorId
                            + "/collections/"
                            + collectionId))
                .status())
        .isEqualTo(StatusCodes.NOT_FOUND);
  }
}
