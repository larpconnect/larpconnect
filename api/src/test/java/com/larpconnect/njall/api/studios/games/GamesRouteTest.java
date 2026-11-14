package com.larpconnect.njall.api.studios.games;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Duration;
import java.time.Instant;
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
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.japi.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class GamesRouteTest {

  private static final String CREATE_BODY =
      "{\"generator\":\"/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001\","
          + "\"system\":\"/api/studios/valiant/v1/systems/019273c5-9271-7000-8000-000000000002\","
          + "\"title\":\"Chronicles of Dusk\"}";
  private static final String PATCH_BODY = "{\"title\":\"Updated Dusk\",\"campaign\":null}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID gameId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-06-01T18:00:00Z");

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "games-route-test");
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

  private static HttpResponse execute(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, HttpRequest request)
      throws Exception {
    return handler.apply(request).toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static JsonNode readJson(HttpResponse response) throws Exception {
    var body =
        response
            .entity()
            .toStrict(5000, system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS)
            .getData()
            .utf8String();
    return objectMapper.readTree(body);
  }

  private StudioLookup activeLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  private StudioLookup deletedLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.of(now));
  }

  private GameResponse sampleGameResponse(Optional<String> campaign) {
    return new GameResponse(
        gameId,
        "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001",
        "/api/studios/valiant/v1/systems/019273c5-9271-7000-8000-000000000002",
        campaign,
        EventType.GAME,
        "Chronicles of Dusk",
        Optional.of("Opening chapter"),
        Optional.empty(),
        Optional.of(now),
        Optional.empty(),
        now,
        now);
  }

  private ActorRef<GameCommand> stubActor(
      GameActorResponse response, AtomicReference<GameCommand> captured) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              captured.set(msg);
              switch (msg) {
                case GameCommand.CreateGame c -> c.replyTo().tell(response);
                case GameCommand.GetGame g -> g.replyTo().tell(response);
                case GameCommand.ListGames l -> l.replyTo().tell(response);
                case GameCommand.PatchGame p -> p.replyTo().tell(response);
                case GameCommand.DeleteGame d -> d.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "stubGameActor-" + UUID.randomUUID(),
        Props.empty());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache, ActorRef<GameCommand> actor) {
    return new GamesRoute(cache, actor, system, objectMapper).route().seal().function(system);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/games creates a game with 201 and studio lookup")
  void postGame_success_returns201() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var lookup = activeLookup();
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(lookup));
    var captured = new AtomicReference<GameCommand>();
    var okActor =
        stubActor(GameActorResponse.success(sampleGameResponse(Optional.empty())), captured);
    var handler = createHandler(cache, okActor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/games")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY);
    var response = execute(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
    var json = readJson(response);
    assertThat(json.get("id").asText()).isEqualTo(gameId.toString());
    assertThat(json.get("eventType").asText()).isEqualTo("GAME");
    assertThat(json.get("campaign").isNull()).isTrue();
    assertThat(captured.get())
        .isInstanceOfSatisfying(
            GameCommand.CreateGame.class, cmd -> assertThat(cmd.lookup()).contains(lookup));
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/games maps 400 and missing/deleted studio 404")
  void postGame_errorAndStudioLookup_returnsExpectedStatus() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    when(cache.findByIdOrAlias("deleted")).thenReturn(Optional.of(deletedLookup()));
    when(cache.findByIdOrAlias("missing")).thenReturn(Optional.empty());
    var captured = new AtomicReference<GameCommand>();

    var badHandler =
        createHandler(cache, stubActor(GameActorResponse.badRequest("Invalid system"), captured));
    var badResp =
        execute(
            badHandler,
            HttpRequest.POST("/api/studios/valiant/v1/games")
                .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY));
    assertThat(badResp.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readJson(badResp).get("code").asInt()).isEqualTo(400);

    var missingResp =
        execute(
            badHandler,
            HttpRequest.POST("/api/studios/missing/v1/games")
                .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY));
    assertThat(missingResp.status()).isEqualTo(StatusCodes.NOT_FOUND);

    var deletedResp =
        execute(
            badHandler,
            HttpRequest.POST("/api/studios/deleted/v1/games")
                .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY));
    assertThat(deletedResp.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/games returns JSON array and forwards query filters")
  void listGames_filtersAndErrors_returnsExpected() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var lookup = activeLookup();
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(lookup));
    var captured = new AtomicReference<GameCommand>();
    var items = ImmutableList.of(sampleGameResponse(Optional.empty()));
    var handler = createHandler(cache, stubActor(GameActorResponse.items(items), captured));

    var response =
        execute(
            handler,
            HttpRequest.GET("/api/studios/valiant/v1/games?systemId=sys-1&campaignId=camp-1"));
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    var json = readJson(response);
    assertThat(json.isArray()).isTrue();
    assertThat(json.get(0).get("campaign").isNull()).isTrue();
    assertThat(captured.get())
        .isInstanceOfSatisfying(
            GameCommand.ListGames.class,
            cmd -> {
              assertThat(cmd.lookup()).contains(lookup);
              assertThat(cmd.systemId()).contains("sys-1");
              assertThat(cmd.campaignId()).contains("camp-1");
            });

    var badHandler =
        createHandler(cache, stubActor(GameActorResponse.badRequest("Bad filter"), captured));
    var badResp =
        execute(badHandler, HttpRequest.GET("/api/studios/valiant/v1/games?systemId=bad"));
    assertThat(badResp.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/games/{id} handles 200, invalid UUID 404, ask timeout")
  void getGame_successInvalidUuidAndTimeout_returnsExpected() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var captured = new AtomicReference<GameCommand>();
    var okActor =
        stubActor(GameActorResponse.success(sampleGameResponse(Optional.empty())), captured);
    var handler = createHandler(cache, okActor);

    var getOk = execute(handler, HttpRequest.GET("/api/studios/valiant/v1/games/" + gameId));
    assertThat(getOk.status()).isEqualTo(StatusCodes.OK);
    var invalidUuid = execute(handler, HttpRequest.GET("/api/studios/valiant/v1/games/not-a-uuid"));
    assertThat(invalidUuid.status()).isEqualTo(StatusCodes.NOT_FOUND);

    ActorRef<GameCommand> stoppedActor =
        system.systemActorOf(
            Behaviors.stopped(), "stoppedGameActor-" + UUID.randomUUID(), Props.empty());
    var timeoutHandler =
        new GamesRoute(cache, stoppedActor, system, objectMapper, Duration.ofMillis(50))
            .route()
            .seal()
            .function(system);
    var timeoutResp =
        execute(timeoutHandler, HttpRequest.GET("/api/studios/valiant/v1/games/" + gameId));
    assertThat(timeoutResp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
    assertThat(readJson(timeoutResp).get("code").asInt()).isEqualTo(500);
  }

  @Test
  @DisplayName(
      "GET /api/studios/{alias}/v1/games/{id}/ handles trailing slash and rejects extra segments")
  void getGame_trailingSlashAndExtraSegments_returnsExpected() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var captured = new AtomicReference<GameCommand>();
    var okActor =
        stubActor(GameActorResponse.success(sampleGameResponse(Optional.empty())), captured);
    var handler = createHandler(cache, okActor);

    var trailingSlash =
        execute(handler, HttpRequest.GET("/api/studios/valiant/v1/games/" + gameId + "/"));
    assertThat(trailingSlash.status()).isEqualTo(StatusCodes.OK);

    var extraSegment =
        execute(handler, HttpRequest.GET("/api/studios/valiant/v1/games/" + gameId + "/extra"));
    assertThat(extraSegment.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("PATCH /api/studios/{alias}/v1/games/{id} forwards update_mask and maps responses")
  void patchGame_updateMaskAndErrors_returnsExpected() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var lookup = activeLookup();
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(lookup));
    var captured = new AtomicReference<GameCommand>();

    var okActor =
        stubActor(GameActorResponse.success(sampleGameResponse(Optional.empty())), captured);
    var okHandler = createHandler(cache, okActor);
    var okRequest =
        HttpRequest.PATCH("/api/studios/valiant/v1/games/" + gameId + "?update_mask=campaign")
            .withEntity(
                ContentTypes.APPLICATION_JSON,
                "{\"system\":null,\"title\":null,\"campaign\":null}");
    assertThat(execute(okHandler, okRequest).status()).isEqualTo(StatusCodes.OK);
    assertThat(captured.get())
        .isInstanceOfSatisfying(
            GameCommand.PatchGame.class,
            cmd -> {
              assertThat(cmd.lookup()).contains(lookup);
              assertThat(cmd.updateMask()).contains("campaign");
            });

    var notFoundHandler =
        createHandler(cache, stubActor(GameActorResponse.notFound("Not found"), captured));
    var notFoundReq =
        HttpRequest.PATCH("/api/studios/valiant/v1/games/" + gameId)
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_BODY);
    assertThat(execute(notFoundHandler, notFoundReq).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("DELETE /api/studios/{alias}/v1/games/{id} returns 204, 404, and 500 failure")
  void deleteGame_deletedNotFoundAndFailure_returnsExpected() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var captured = new AtomicReference<GameCommand>();

    var okHandler = createHandler(cache, stubActor(GameActorResponse.deleted(), captured));
    var deletedResp =
        execute(okHandler, HttpRequest.DELETE("/api/studios/valiant/v1/games/" + gameId));
    assertThat(deletedResp.status()).isEqualTo(StatusCodes.NO_CONTENT);

    var notFoundHandler =
        createHandler(cache, stubActor(GameActorResponse.notFound("Missing"), captured));
    var notFoundResp =
        execute(notFoundHandler, HttpRequest.DELETE("/api/studios/valiant/v1/games/" + gameId));
    assertThat(notFoundResp.status()).isEqualTo(StatusCodes.NOT_FOUND);

    var errorHandler =
        createHandler(cache, stubActor(GameActorResponse.failure("Server error"), captured));
    var errorResp =
        execute(errorHandler, HttpRequest.DELETE("/api/studios/valiant/v1/games/" + gameId));
    assertThat(errorResp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
    assertThat(readJson(errorResp).get("code").asInt()).isEqualTo(500);
  }

  @Test
  @DisplayName("POST and PATCH reject malformed, empty, non-object, and non-textual JSON with 400")
  void jsonRejections_malformedAndEmptyPayloads_returnJson400() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var captured = new AtomicReference<GameCommand>();
    var handler = createHandler(cache, stubActor(GameActorResponse.badRequest("unused"), captured));

    var malformed =
        execute(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/games")
                .withEntity(ContentTypes.APPLICATION_JSON, "{not-json"));
    assertThat(malformed.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readJson(malformed).get("code").asInt()).isEqualTo(400);

    var emptyEntity = execute(handler, HttpRequest.POST("/api/studios/valiant/v1/games"));
    assertThat(emptyEntity.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readJson(emptyEntity).get("code").asInt()).isEqualTo(400);

    var arrayBody =
        execute(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/games")
                .withEntity(ContentTypes.APPLICATION_JSON, "[]"));
    assertThat(arrayBody.status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var nullSystemPatch =
        execute(
            handler,
            HttpRequest.PATCH("/api/studios/valiant/v1/games/" + gameId)
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"system\":null}"));
    assertThat(nullSystemPatch.status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var nonTextualTitleUnderMask =
        execute(
            handler,
            HttpRequest.PATCH("/api/studios/valiant/v1/games/" + gameId + "?update_mask=summary")
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"title\":123,\"summary\":null}"));
    assertThat(nonTextualTitleUnderMask.status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var badOptionalType =
        execute(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/games")
                .withEntity(
                    ContentTypes.APPLICATION_JSON,
                    "{\"generator\":\"a\",\"system\":\"s\",\"title\":\"t\",\"campaign\":123}"));
    assertThat(badOptionalType.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("handleJsonBody returns 400 JSON when treeToValue throws JsonProcessingException")
  void jsonDecoding_jsonProcessingException_returnJson400() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var captured = new AtomicReference<GameCommand>();
    var spyMapper = spy(objectMapper);
    var jsonEx = mock(JsonProcessingException.class);
    doThrow(jsonEx).when(spyMapper).treeToValue(any(JsonNode.class), eq(CreateGameRequest.class));
    var spyHandler =
        new GamesRoute(cache, stubActor(GameActorResponse.deleted(), captured), system, spyMapper)
            .route()
            .seal()
            .function(system);

    var thrownTree =
        execute(
            spyHandler,
            HttpRequest.POST("/api/studios/valiant/v1/games")
                .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY));
    assertThat(thrownTree.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }
}
