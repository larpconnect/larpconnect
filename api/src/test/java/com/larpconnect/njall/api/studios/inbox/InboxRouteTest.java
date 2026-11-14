package com.larpconnect.njall.api.studios.inbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpHeader;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.model.headers.RawHeader;
import org.apache.pekko.http.javadsl.server.Directives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.japi.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class InboxRouteTest {

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "inbox-route-test");
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

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      ActorRef<InboxCommand> inboxActor) {
    return createHandlerForActor(inboxActor, actorId.toString());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandlerForActor(
      ActorRef<InboxCommand> inboxActor, String actorIdentifier) {
    var digestVerifier = new DefaultContentDigestVerifier();
    var route =
        new DefaultInboxRoute(
            inboxActor, digestVerifier, system, objectMapper, Duration.ofSeconds(5));
    var lookup = new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());

    return Directives.pathPrefix(
            PathMatchers.separateOnSlashes("api/studios/valiant/v1/actors/" + actorIdentifier),
            () -> route.inboxSubroutes(lookup, "valiant", actorIdentifier))
        .seal()
        .function(system);
  }

  private ActorRef<InboxCommand> createMockInboxActor(InboxActorResponse response) {
    return createCapturingInboxActor(response, new AtomicReference<>());
  }

  private ActorRef<InboxCommand> createCapturingInboxActor(
      InboxActorResponse response, AtomicReference<InboxCommand> captured) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              captured.set(msg);
              if (msg instanceof InboxCommand.DeliverActivity single) {
                single.replyTo().tell(response);
              } else if (msg instanceof InboxCommand.DeliverBatch batch) {
                batch.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testInboxActor" + UUID.randomUUID(),
        Props.empty());
  }

  @Test
  @DisplayName("POST /inbox delivers single activity without blocking route and returns 202")
  void postInbox_single_returnsAccepted() throws Exception {
    var captured = new AtomicReference<InboxCommand>();
    var inboxActor = createCapturingInboxActor(new InboxActorResponse.Success(), captured);
    var handler = createHandlerForActor(inboxActor, "valkyrie_lead");

    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/valkyrie_lead/inbox")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"type\":\"Create\"}");
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.ACCEPTED);
    assertThat(captured.get()).isInstanceOf(InboxCommand.DeliverActivity.class);
    assertThat(((InboxCommand.DeliverActivity) captured.get()).actorIdentifier())
        .isEqualTo("valkyrie_lead");
  }

  @Test
  @DisplayName("POST /inbox:message custom method alias returns 202 Accepted")
  void postInboxMessage_customMethod_returnsAccepted() throws Exception {
    var inboxActor = createMockInboxActor(new InboxActorResponse.Success());
    var handler = createHandler(inboxActor);

    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox:message")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"type\":\"Offer\"}");
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.ACCEPTED);
  }

  @Test
  @DisplayName("POST /inbox:batchMessage delivers batch and returns 202 Accepted")
  void postInboxBatch_returnsAccepted() throws Exception {
    var captured = new AtomicReference<InboxCommand>();
    var batchResponse = new BatchInboxActivitiesResponse(ImmutableMap.of());
    var inboxActor =
        createCapturingInboxActor(new InboxActorResponse.BatchSuccess(batchResponse), captured);
    var handler = createHandlerForActor(inboxActor, "valkyrie_lead");

    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/valkyrie_lead/inbox:batchMessage")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"requests\":[{\"type\":\"Create\"}]}");
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.ACCEPTED);
    assertThat(captured.get()).isInstanceOf(InboxCommand.DeliverBatch.class);
    assertThat(((InboxCommand.DeliverBatch) captured.get()).actorIdentifier())
        .isEqualTo("valkyrie_lead");
  }

  @Test
  @DisplayName("POST /inbox with invalid Content-Digest header returns 400 Bad Request")
  void postInbox_invalidContentDigest_returnsBadRequest() throws Exception {
    var inboxActor = createMockInboxActor(new InboxActorResponse.Success());
    var handler = createHandler(inboxActor);

    HttpHeader digestHeader = RawHeader.create("Content-Digest", "sha-256=:bad-digest=:");
    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox")
            .addHeader(digestHeader)
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"type\":\"Create\"}");
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /inbox returns 404 when actor lookup in InboxActor reports missing actor")
  void postInbox_missingActor_returnsNotFound() throws Exception {
    var inboxActor =
        createMockInboxActor(new InboxActorResponse.Failure(404, "Actor not found: " + actorId));
    var handler = createHandler(inboxActor);

    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"type\":\"Create\"}");
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("POST /inbox:batchMessage returns 404 when InboxActor reports missing actor")
  void postInboxBatch_missingActor_returnsNotFound() throws Exception {
    var inboxActor =
        createMockInboxActor(new InboxActorResponse.Failure(404, "Actor not found: " + actorId));
    var handler = createHandler(inboxActor);

    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox:batchMessage")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"requests\":[]}");
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  private static BatchInboxErrorResponse readBatchError(HttpResponse response) throws Exception {
    byte[] bytes =
        response
            .entity()
            .toStrict(5000, system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS)
            .getData()
            .toArray();
    return objectMapper.readValue(bytes, BatchInboxErrorResponse.class);
  }

  @Test
  @DisplayName("POST /inbox:batchMessage with invalid Content-Digest header returns 400")
  void postInboxBatch_invalidContentDigest_returnsBadRequest() throws Exception {
    var inboxActor = createMockInboxActor(new InboxActorResponse.Success());
    var handler = createHandler(inboxActor);

    HttpHeader digestHeader = RawHeader.create("Content-Digest", "sha-256=:bad-digest=:");
    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox:batchMessage")
            .addHeader(digestHeader)
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"requests\":[]}");
    var res = executeRequest(handler, req);

    assertThat(res.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    var errBody = readBatchError(res);
    assertThat(errBody.code()).isEqualTo(400);
    assertThat(errBody.failedRequests()).isEmpty();
  }

  @Test
  @DisplayName("POST /inbox and batch with malformed, empty, or @context wrapper JSON return 400")
  void postInbox_malformedJson_returnsBadRequest() throws Exception {
    var inboxActor = createMockInboxActor(new InboxActorResponse.Success());
    var handler = createHandler(inboxActor);

    var reqSingle =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox")
            .withEntity(ContentTypes.APPLICATION_JSON, "{not-valid-json");
    assertThat(executeRequest(handler, reqSingle).status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var reqEmptySingle =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox")
            .withEntity(ContentTypes.APPLICATION_JSON, "");
    assertThat(executeRequest(handler, reqEmptySingle).status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var reqBatch =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox:batchMessage")
            .withEntity(ContentTypes.APPLICATION_JSON, "{not-valid-json");
    var resBatch = executeRequest(handler, reqBatch);
    assertThat(resBatch.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBatchError(resBatch).failedRequests()).isEmpty();

    var reqMissingKey =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox:batchMessage")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"items\":[]}");
    var resMissingKey = executeRequest(handler, reqMissingKey);
    assertThat(resMissingKey.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBatchError(resMissingKey).failedRequests()).isEmpty();

    var reqContextWrapper =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox:batchMessage")
            .withEntity(
                ContentTypes.APPLICATION_JSON,
                "{\"@context\":[\"https://larpconnect.app/schemas/featherpub\"],\"requests\":[]}");
    var resContextWrapper = executeRequest(handler, reqContextWrapper);
    assertThat(resContextWrapper.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBatchError(resContextWrapper).message()).contains("@context");

    var reqUnknownKey =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox:batchMessage")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"requests\":[],\"extra\":true}");
    assertThat(executeRequest(handler, reqUnknownKey).status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var reqArrayRoot =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox:batchMessage")
            .withEntity(ContentTypes.APPLICATION_JSON, "[]");
    assertThat(executeRequest(handler, reqArrayRoot).status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /inbox actor response mappings (400, 404, 503, 500, unexpected BatchSuccess)")
  void postInbox_actorResponseMappings() throws Exception {
    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"type\":\"Create\"}");

    assertThat(
            executeRequest(
                    createHandler(createMockInboxActor(new InboxActorResponse.Failure(400, "bad"))),
                    req)
                .status())
        .isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(
            executeRequest(
                    createHandler(
                        createMockInboxActor(new InboxActorResponse.Failure(404, "missing"))),
                    req)
                .status())
        .isEqualTo(StatusCodes.NOT_FOUND);
    assertThat(
            executeRequest(
                    createHandler(
                        createMockInboxActor(new InboxActorResponse.Failure(503, "down"))),
                    req)
                .status())
        .isEqualTo(StatusCodes.SERVICE_UNAVAILABLE);
    assertThat(
            executeRequest(
                    createHandler(
                        createMockInboxActor(new InboxActorResponse.Failure(500, "internal"))),
                    req)
                .status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
    assertThat(
            executeRequest(
                    createHandler(
                        createMockInboxActor(
                            new InboxActorResponse.BatchSuccess(
                                new BatchInboxActivitiesResponse(ImmutableMap.of())))),
                    req)
                .status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("POST /inbox:batchMessage actor response mappings (BatchBadRequest, 400, 404, 503)")
  void postInboxBatch_actorResponseMappings() throws Exception {
    var req =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox:batchMessage")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"requests\":[{\"type\":\"Create\"}]}");

    var batchError = new BatchInboxErrorResponse(400, "all failed", ImmutableMap.of());
    var bbrActor =
        createMockInboxActor(
            new InboxActorResponse.Failure(400, "all failed", Optional.of(batchError)));
    assertThat(executeRequest(createHandler(bbrActor), req).status())
        .isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(
            executeRequest(
                    createHandler(createMockInboxActor(new InboxActorResponse.Failure(400, "bad"))),
                    req)
                .status())
        .isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(
            executeRequest(
                    createHandler(
                        createMockInboxActor(new InboxActorResponse.Failure(404, "missing"))),
                    req)
                .status())
        .isEqualTo(StatusCodes.NOT_FOUND);
    assertThat(
            executeRequest(
                    createHandler(
                        createMockInboxActor(new InboxActorResponse.Failure(503, "down"))),
                    req)
                .status())
        .isEqualTo(StatusCodes.SERVICE_UNAVAILABLE);
    assertThat(
            executeRequest(
                    createHandler(createMockInboxActor(new InboxActorResponse.Success())), req)
                .status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("POST /inbox and batch handle actor timeout with 500")
  void postInbox_actorTimeout_returns500() throws Exception {
    ActorRef<InboxCommand> silentActor =
        system.systemActorOf(
            Behaviors.receiveMessage(_ -> Behaviors.same()),
            "silentActor" + UUID.randomUUID(),
            Props.empty());

    var route =
        new DefaultInboxRoute(
            silentActor,
            new DefaultContentDigestVerifier(),
            system,
            objectMapper,
            Duration.ofMillis(50));
    var lookup = new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
    var handler =
        Directives.pathPrefix(
                PathMatchers.separateOnSlashes("api/studios/valiant/v1/actors/" + actorId),
                () -> route.inboxSubroutes(lookup, "valiant", actorId.toString()))
            .seal()
            .function(system);

    var reqSingle =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"type\":\"Create\"}");
    assertThat(executeRequest(handler, reqSingle).status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);

    var reqBatch =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox:batchMessage")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"requests\":[{\"type\":\"Create\"}]}");
    assertThat(executeRequest(handler, reqBatch).status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("InboxRoute default constructor and immutable batch DTO defensive copies")
  void dtoDefensiveCopies_andDefaultRouteConstructor() throws Exception {
    var inboxActor = createMockInboxActor(new InboxActorResponse.Success());
    var route =
        new DefaultInboxRoute(inboxActor, new DefaultContentDigestVerifier(), system, objectMapper);
    var lookup = new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
    var defaultHandler =
        Directives.pathPrefix(
                PathMatchers.separateOnSlashes("api/studios/valiant/v1/actors/" + actorId),
                () -> route.inboxSubroutes(lookup, "valiant", actorId.toString()))
            .seal()
            .function(system);
    var defaultReq =
        HttpRequest.POST("/api/studios/valiant/v1/actors/" + actorId + "/inbox")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"type\":\"Create\"}");
    assertThat(executeRequest(defaultHandler, defaultReq).status()).isEqualTo(StatusCodes.ACCEPTED);

    ObjectNode mutableItem = objectMapper.createObjectNode().put("type", "Create");
    var mutableList = new ArrayList<>(ImmutableList.of((JsonNode) mutableItem));
    var batchReq = new BatchInboxActivitiesRequest(mutableList);
    mutableItem.put("type", "Mutated");
    mutableList.clear();
    assertThat(batchReq.requests()).hasSize(1);
    assertThat(batchReq.requests().getFirst().path("type").asText()).isEqualTo("Create");
    ((ObjectNode) batchReq.requests().getFirst()).put("type", "MutatedAccessor");
    assertThat(batchReq.requests().getFirst().path("type").asText()).isEqualTo("Create");

    var mutableErrors = new HashMap<String, BatchInboxItemError>();
    mutableErrors.put("0", new BatchInboxItemError(400, "invalid"));
    var resp = new BatchInboxActivitiesResponse(mutableErrors);
    var errResp = new BatchInboxErrorResponse(400, "failed", mutableErrors);
    mutableErrors.clear();
    assertThat(resp.failedRequests()).hasSize(1);
    assertThat(errResp.failedRequests()).hasSize(1);

    var deserResp =
        objectMapper.readValue(
            objectMapper.writeValueAsBytes(resp), BatchInboxActivitiesResponse.class);
    var deserErr =
        objectMapper.readValue(
            objectMapper.writeValueAsBytes(errResp), BatchInboxErrorResponse.class);
    assertThat(deserResp).isEqualTo(resp);
    assertThat(deserErr).isEqualTo(errResp);
  }

  @Test
  @DisplayName("Batch DTO @JsonCreator constructors normalize omitted and null JSON elements")
  void batchDtos_normalizeNullAndOmittedJsonProperties() throws Exception {
    var emptyResp = new BatchInboxActivitiesResponse(ImmutableMap.of());
    byte[] emptyRespJson = objectMapper.writeValueAsBytes(emptyResp);
    assertThat(objectMapper.readTree(emptyRespJson)).isEmpty();
    var roundTrippedEmptyResp =
        objectMapper.readValue(emptyRespJson, BatchInboxActivitiesResponse.class);
    assertThat(roundTrippedEmptyResp.failedRequests()).isEmpty();

    var omittedFailedErr =
        objectMapper.readValue("{\"code\":400,\"message\":\"bad\"}", BatchInboxErrorResponse.class);
    assertThat(omittedFailedErr.code()).isEqualTo(400);
    assertThat(omittedFailedErr.failedRequests()).isEmpty();

    var nullRequestsDto =
        objectMapper.readValue("{\"requests\":null}", BatchInboxActivitiesRequest.class);
    assertThat(nullRequestsDto.requests()).isEmpty();

    var listWithJavaNull = new ArrayList<JsonNode>();
    listWithJavaNull.add(objectMapper.createObjectNode().put("type", "Create"));
    listWithJavaNull.add(null);
    var normalizedBatch = new BatchInboxActivitiesRequest(listWithJavaNull);
    assertThat(normalizedBatch.requests()).hasSize(2);
    assertThat(normalizedBatch.requests().get(1).isNull()).isTrue();
  }
}
