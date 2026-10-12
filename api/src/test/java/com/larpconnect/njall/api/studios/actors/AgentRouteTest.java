package com.larpconnect.njall.api.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.larpconnect.njall.api.studios.collections.OrderedCollectionResponse;
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

final class AgentRouteTest {

  private static final String CREATE_ACTOR_BODY =
      "{\"ownerId\":\"018f2345-6789-7abc-def0-123456789def\","
          + "\"preferredUsername\":\"valkyrie_lead\",\"summary\":\"Lead singer\"}";
  private static final String PATCH_ACTOR_BODY = "{\"preferredUsername\":\"valkyrie_commander\"}";
  private static final String ADD_ENDPOINT_BODY =
      "{\"name\":\"sharedInbox\",\"endpoint\":\"https://fed.example.com/inbox\"}";
  private static final String REMOVE_ENDPOINT_BODY = "{\"name\":\"sharedInbox\"}";
  private static final String POST_PAYLOAD_BODY = "{\"type\":\"Create\"}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID ownerId = UUID.fromString("018f2345-6789-7abc-def0-123456789def");
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "agent-route-test");
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

  private ActorResponse sampleActorResponse() {
    return new ActorResponse(
        actorId,
        "Actor",
        ownerId,
        "valkyrie_lead",
        Optional.of("Lead singer"),
        "/api/studios/valiant/v1/actors/" + actorId + "/inbox",
        "/api/studios/valiant/v1/actors/" + actorId + "/outbox",
        ImmutableMap.of("sharedInbox", "https://fed.example.com/inbox"),
        now,
        now);
  }

  private OrderedCollectionResponse sampleCollectionResponse(String name) {
    return new OrderedCollectionResponse(
        UUID.randomUUID(),
        "OrderedCollection",
        name,
        Optional.empty(),
        actorId,
        0,
        ImmutableList.of(),
        now,
        now);
  }

  private ActorRef<AgentCommand> createAgentActor(AgentActorResponse response) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              switch (msg) {
                case AgentCommand.CreateAgent c -> c.replyTo().tell(response);
                case AgentCommand.GetAgent g -> g.replyTo().tell(response);
                case AgentCommand.PatchAgent p -> p.replyTo().tell(response);
                case AgentCommand.DeleteAgent d -> d.replyTo().tell(response);
                case AgentCommand.MutateEndpoint m -> m.replyTo().tell(response);
                case AgentCommand.AccessCollection a -> a.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testAgentActor" + UUID.randomUUID(),
        Props.empty());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache, ActorRef<AgentCommand> actor) {
    return new AgentRoute(cache, actor, system, objectMapper).route().seal().function(system);
  }

  @Test
  @DisplayName("POST /v1/actors creates actor")
  void postActor_returnsCreated() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createAgentActor(AgentActorResponse.success(sampleActorResponse()));
    var handler = createHandler(cache, actor);

    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_ACTOR_BODY);
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("GET /v1/actors returns 404 (listing not supported)")
  void getActorsList_returnsNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createAgentActor(AgentActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var req = HttpRequest.GET("/api/studios/valiant/v1/actors");
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /v1/actors/{id} returns actor detail")
  void getActor_returnsOk() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createAgentActor(AgentActorResponse.success(sampleActorResponse()));
    var handler = createHandler(cache, actor);

    var req = HttpRequest.GET("/api/studios/valiant/v1/actors/" + actorId);
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("PATCH /v1/actors/{id} returns updated actor")
  void patchActor_returnsOk() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createAgentActor(AgentActorResponse.success(sampleActorResponse()));
    var handler = createHandler(cache, actor);

    var req =
        HttpRequest.PATCH(
                "/api/studios/valiant/v1/actors/" + actorId + "?update_mask=preferredUsername")
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_ACTOR_BODY);
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("DELETE /v1/actors/{id} returns 204")
  void deleteActor_returnsNoContent() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createAgentActor(AgentActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var req = HttpRequest.DELETE("/api/studios/valiant/v1/actors/" + actorId);
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("POST /v1/actors/{id}:addEndpoint returns 204")
  void addEndpoint_returnsNoContent() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createAgentActor(AgentActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + ":addEndpoint")
            .withEntity(ContentTypes.APPLICATION_JSON, ADD_ENDPOINT_BODY);
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("POST /v1/actors/{id}:removeEndpoint returns 204")
  void removeEndpoint_returnsNoContent() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createAgentActor(AgentActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + ":removeEndpoint")
            .withEntity(ContentTypes.APPLICATION_JSON, REMOVE_ENDPOINT_BODY);
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("GET and POST /v1/actors/{id}/inbox return 200 and 201")
  void inbox_routes() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor =
        createAgentActor(AgentActorResponse.collectionSuccess(sampleCollectionResponse("inbox")));
    var handler = createHandler(cache, actor);

    var getReq = HttpRequest.GET("/api/studios/valiant/v1/actors/" + actorId + "/inbox");
    var getRes = executeRequest(handler, getReq);
    assertThat(getRes.status()).isEqualTo(StatusCodes.OK);

    var postActor = createAgentActor(AgentActorResponse.created());
    var postHandler = createHandler(cache, postActor);
    var postReq =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox")
            .withEntity(ContentTypes.APPLICATION_JSON, POST_PAYLOAD_BODY);
    var postRes = executeRequest(postHandler, postReq);
    assertThat(postRes.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("GET and POST /v1/actors/{id}/outbox return 200 and 201")
  void outbox_routes() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor =
        createAgentActor(AgentActorResponse.collectionSuccess(sampleCollectionResponse("outbox")));
    var handler = createHandler(cache, actor);

    var getReq = HttpRequest.GET("/api/studios/valiant/v1/actors/" + actorId + "/outbox");
    var getRes = executeRequest(handler, getReq);
    assertThat(getRes.status()).isEqualTo(StatusCodes.OK);

    var postActor = createAgentActor(AgentActorResponse.created());
    var postHandler = createHandler(cache, postActor);
    var postReq =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/outbox")
            .withEntity(ContentTypes.APPLICATION_JSON, POST_PAYLOAD_BODY);
    var postRes = executeRequest(postHandler, postReq);
    assertThat(postRes.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("Route returns 404 when studio is deleted or not found")
  void studioNotFound_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleDeletedLookup()));
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());

    var actor = createAgentActor(AgentActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var deletedReq = HttpRequest.GET("/api/studios/valiant/v1/actors/" + actorId);
    assertThat(executeRequest(handler, deletedReq).status()).isEqualTo(StatusCodes.NOT_FOUND);

    var unknownReq = HttpRequest.GET("/api/studios/unknown/v1/actors/" + actorId);
    assertThat(executeRequest(handler, unknownReq).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("Route returns 404 when actor ID is invalid")
  void invalidActorId_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createAgentActor(AgentActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var req = HttpRequest.GET("/api/studios/valiant/v1/actors/not-a-valid-uuid");
    assertThat(executeRequest(handler, req).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("Route returns 404 when actor ID is invalid in custom methods")
  void invalidActorIdInCustomMethods_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createAgentActor(AgentActorResponse.noContent());
    var handler = createHandler(cache, actor);

    var addReq =
        HttpRequest.POST("/api/studios/valiant/v1/actors/not-a-valid-uuid:addEndpoint")
            .withEntity(ContentTypes.APPLICATION_JSON, ADD_ENDPOINT_BODY);
    assertThat(executeRequest(handler, addReq).status()).isEqualTo(StatusCodes.NOT_FOUND);

    var removeReq =
        HttpRequest.POST("/api/studios/valiant/v1/actors/not-a-valid-uuid:removeEndpoint")
            .withEntity(ContentTypes.APPLICATION_JSON, REMOVE_ENDPOINT_BODY);
    assertThat(executeRequest(handler, removeReq).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("Route maps actor failure and ask failure to appropriate HTTP status")
  void actorFailure_returnsMappedStatus() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var badActor = createAgentActor(AgentActorResponse.badRequest("Invalid input"));
    var badHandler = createHandler(cache, badActor);
    var getReq = HttpRequest.GET("/api/studios/valiant/v1/actors/" + actorId);
    assertThat(executeRequest(badHandler, getReq).status()).isEqualTo(StatusCodes.BAD_REQUEST);

    ActorRef<AgentCommand> emptyActor =
        system.systemActorOf(Behaviors.empty(), "emptyActor" + UUID.randomUUID(), Props.empty());
    var timeoutHandler =
        new AgentRoute(cache, emptyActor, system, objectMapper, Duration.ofMillis(50))
            .route()
            .seal()
            .function(system);
    assertThat(executeRequest(timeoutHandler, getReq).status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
