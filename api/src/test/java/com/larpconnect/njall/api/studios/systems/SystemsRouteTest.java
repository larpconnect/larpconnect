package com.larpconnect.njall.api.studios.systems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
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

final class SystemsRouteTest {

  private static final String CREATE_BODY =
      "{\"generator\":\"019273c5-9271-7000-8000-000000000001\","
          + "\"name\":\"Daggerheart\",\"summary\":\"Narrative fantasy RPG\"}";
  private static final String PATCH_BODY = "{\"name\":\"Daggerheart 2.0\",\"summary\":null}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "systems-route-test");
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

  private static String readBody(HttpResponse response) throws Exception {
    return response
        .entity()
        .toStrict(1000, system)
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS)
        .getData()
        .utf8String();
  }

  private StudioLookup activeLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  private GameSystemResponse sampleSystemResponse() {
    return new GameSystemResponse(
        systemId,
        "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001",
        "Daggerheart",
        Optional.of("Narrative fantasy RPG"),
        now,
        now);
  }

  private ActorRef<GameSystemCommand> createSystemActor(
      GameSystemActorResponse response, AtomicReference<GameSystemCommand> captured) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              captured.set(msg);
              switch (msg) {
                case GameSystemCommand.CreateGameSystem c -> c.replyTo().tell(response);
                case GameSystemCommand.GetGameSystem g -> g.replyTo().tell(response);
                case GameSystemCommand.ListGameSystems l -> l.replyTo().tell(response);
                case GameSystemCommand.PatchGameSystem p -> p.replyTo().tell(response);
                case GameSystemCommand.DeleteGameSystem d -> d.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testSystemActor" + UUID.randomUUID(),
        Props.empty());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache, ActorRef<GameSystemCommand> actor) {
    return new SystemsRoute(cache, actor, system, objectMapper).route().seal().function(system);
  }

  @Test
  @DisplayName("POST /api/studios/{studio}/v1/systems creates system and forwards lookup")
  void createSystem_success_returns201() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var lookup = activeLookup();
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(lookup));
    var captured = new AtomicReference<GameSystemCommand>();
    var actor =
        createSystemActor(GameSystemActorResponse.success(sampleSystemResponse()), captured);

    var response =
        executeRequest(
            createHandler(cache, actor),
            HttpRequest.POST("/api/studios/valiant/v1/systems")
                .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY));

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
    var body = objectMapper.readValue(readBody(response), GameSystemResponse.class);
    assertThat(body.name()).isEqualTo("Daggerheart");
    var cmd = (GameSystemCommand.CreateGameSystem) captured.get();
    assertThat(cmd.lookup()).contains(lookup);
  }

  @Test
  @DisplayName("GET /api/studios/{studio}/v1/systems lists game systems")
  void listSystems_success_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var captured = new AtomicReference<GameSystemCommand>();
    var actor =
        createSystemActor(
            GameSystemActorResponse.items(ImmutableList.of(sampleSystemResponse())), captured);

    var response =
        executeRequest(
            createHandler(cache, actor), HttpRequest.GET("/api/studios/valiant/v1/systems"));

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    assertThat(captured.get()).isInstanceOf(GameSystemCommand.ListGameSystems.class);
    var tree = objectMapper.readTree(readBody(response));
    assertThat(tree.isArray()).isTrue();
    assertThat(tree).hasSize(1);
  }

  @Test
  @DisplayName("GET /api/studios/{studio}/v1/systems/{id} returns 200 and 404")
  void getSystem_foundAndMissing_returns200And404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var okActor =
        createSystemActor(
            GameSystemActorResponse.success(sampleSystemResponse()), new AtomicReference<>());
    var okResponse =
        executeRequest(
            createHandler(cache, okActor),
            HttpRequest.GET("/api/studios/valiant/v1/systems/" + systemId));
    assertThat(okResponse.status()).isEqualTo(StatusCodes.OK);

    var missingActor =
        createSystemActor(
            GameSystemActorResponse.notFound("Game system not found"), new AtomicReference<>());
    var missingResponse =
        executeRequest(
            createHandler(cache, missingActor),
            HttpRequest.GET("/api/studios/valiant/v1/systems/" + systemId));
    assertThat(missingResponse.status()).isEqualTo(StatusCodes.NOT_FOUND);
    var err = objectMapper.readValue(readBody(missingResponse), StudioErrorResponse.class);
    assertThat(err.code()).isEqualTo(404);
  }

  @Test
  @DisplayName("PATCH /api/studios/{studio}/v1/systems/{id} forwards update_mask and returns 200")
  void patchSystem_success_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var captured = new AtomicReference<GameSystemCommand>();
    var actor =
        createSystemActor(GameSystemActorResponse.success(sampleSystemResponse()), captured);

    var response =
        executeRequest(
            createHandler(cache, actor),
            HttpRequest.PATCH(
                    "/api/studios/valiant/v1/systems/" + systemId + "?update_mask=name,summary")
                .withEntity(ContentTypes.APPLICATION_JSON, PATCH_BODY));

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    var cmd = (GameSystemCommand.PatchGameSystem) captured.get();
    assertThat(cmd.updateMask()).contains("name,summary");
  }

  @Test
  @DisplayName("DELETE /api/studios/{studio}/v1/systems/{id} returns 204")
  void deleteSystem_success_returns204() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var actor = createSystemActor(GameSystemActorResponse.deleted(), new AtomicReference<>());

    var response =
        executeRequest(
            createHandler(cache, actor),
            HttpRequest.DELETE("/api/studios/valiant/v1/systems/" + systemId));
    assertThat(response.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("Returns 404 when studio is missing, soft-deleted, or systemId is invalid")
  void missingStudioDeletedStudioOrInvalidId_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var deletedLookup = new StudioLookup(tenantId, studioId, "deleted", now, now, Optional.of(now));
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());
    when(cache.findByIdOrAlias("deleted")).thenReturn(Optional.of(deletedLookup));
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var actor =
        createSystemActor(GameSystemActorResponse.notFound("unused"), new AtomicReference<>());
    var handler = createHandler(cache, actor);

    assertThat(executeRequest(handler, HttpRequest.GET("/api/studios/unknown/v1/systems")).status())
        .isEqualTo(StatusCodes.NOT_FOUND);
    assertThat(executeRequest(handler, HttpRequest.GET("/api/studios/deleted/v1/systems")).status())
        .isEqualTo(StatusCodes.NOT_FOUND);
    assertThat(
            executeRequest(handler, HttpRequest.GET("/api/studios/valiant/v1/systems/not-a-uuid"))
                .status())
        .isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("Maps actor 400, 409, and 500 failure responses to HTTP status codes")
  void actorFailures_mapToBadRequestConflictAndServerError() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    var badActor =
        createSystemActor(
            GameSystemActorResponse.badRequest("Invalid generator"), new AtomicReference<>());
    var badResponse =
        executeRequest(
            createHandler(cache, badActor),
            HttpRequest.POST("/api/studios/valiant/v1/systems")
                .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY));
    assertThat(badResponse.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(objectMapper.readValue(readBody(badResponse), StudioErrorResponse.class).code())
        .isEqualTo(400);

    var conflictActor =
        createSystemActor(
            GameSystemActorResponse.conflict("System name exists"), new AtomicReference<>());
    var conflictResponse =
        executeRequest(
            createHandler(cache, conflictActor),
            HttpRequest.POST("/api/studios/valiant/v1/systems")
                .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY));
    assertThat(conflictResponse.status()).isEqualTo(StatusCodes.CONFLICT);
    assertThat(objectMapper.readValue(readBody(conflictResponse), StudioErrorResponse.class))
        .isEqualTo(new StudioErrorResponse(409, "System name exists"));

    var errorActor =
        createSystemActor(GameSystemActorResponse.failure("DB failure"), new AtomicReference<>());
    assertThat(
            executeRequest(
                    createHandler(cache, errorActor),
                    HttpRequest.GET("/api/studios/valiant/v1/systems/" + systemId))
                .status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("Maps ask timeout failure to 500 Internal Server Error")
  void actorAskTimeout_returnsInternalServerError() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));

    ActorRef<GameSystemCommand> silentActor =
        system.systemActorOf(
            Behaviors.receiveMessage(msg -> Behaviors.same()),
            "silentSystemActor" + UUID.randomUUID(),
            Props.empty());
    var timeoutHandler =
        new SystemsRoute(cache, silentActor, system, objectMapper, Duration.ofMillis(50))
            .route()
            .seal()
            .function(system);
    var timeoutResponse =
        executeRequest(
            timeoutHandler, HttpRequest.GET("/api/studios/valiant/v1/systems/" + systemId));
    assertThat(timeoutResponse.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
    assertThat(objectMapper.readValue(readBody(timeoutResponse), StudioErrorResponse.class))
        .isEqualTo(new StudioErrorResponse(500, "Internal server error"));
  }

  @Test
  @DisplayName("Rejects malformed, empty, and non-textual POST JSON bodies with AIP-193 400")
  void invalidJsonPostRequests_returnStudioError400() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var actor =
        createSystemActor(
            GameSystemActorResponse.success(sampleSystemResponse()), new AtomicReference<>());
    var handler = createHandler(cache, actor);

    var malformed =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/systems")
                .withEntity(ContentTypes.APPLICATION_JSON, "{not-json"));
    assertThat(malformed.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(objectMapper.readValue(readBody(malformed), StudioErrorResponse.class).code())
        .isEqualTo(400);

    var nonObject =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/systems")
                .withEntity(ContentTypes.APPLICATION_JSON, "[]"));
    assertThat(nonObject.status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var nullName =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/systems")
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"generator\":\"g\",\"name\":null}"));
    assertThat(nullName.status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var unknownField =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/systems")
                .withEntity(
                    ContentTypes.APPLICATION_JSON,
                    "{\"generator\":\"g\",\"name\":\"n\",\"unknown\":\"bad\"}"));
    assertThat(unknownField.status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var emptyBody = executeRequest(handler, HttpRequest.POST("/api/studios/valiant/v1/systems"));
    assertThat(emptyBody.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    var emptyError = objectMapper.readValue(readBody(emptyBody), StudioErrorResponse.class);
    assertThat(emptyError.code()).isEqualTo(400);
    assertThat(emptyError.message()).isNotBlank();
  }

  @Test
  @DisplayName("PATCH rejects null name and non-string summary while allowing summary-only PATCH")
  void patchJsonValidation_rejectsNullNameAndAllowsPartialSummaryNull() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(activeLookup()));
    var actor =
        createSystemActor(
            GameSystemActorResponse.success(sampleSystemResponse()), new AtomicReference<>());
    var handler = createHandler(cache, actor);
    var url = "/api/studios/valiant/v1/systems/" + systemId;

    var patchNullName =
        executeRequest(
            handler,
            HttpRequest.PATCH(url).withEntity(ContentTypes.APPLICATION_JSON, "{\"name\":null}"));
    assertThat(patchNullName.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(objectMapper.readValue(readBody(patchNullName), StudioErrorResponse.class).code())
        .isEqualTo(400);

    var badPatchSummary =
        executeRequest(
            handler,
            HttpRequest.PATCH(url).withEntity(ContentTypes.APPLICATION_JSON, "{\"summary\":123}"));
    assertThat(badPatchSummary.status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var partialNullSummary =
        executeRequest(
            handler,
            HttpRequest.PATCH(url + "?update_mask=summary")
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"name\":null,\"summary\":null}"));
    assertThat(partialNullSummary.status()).isEqualTo(StatusCodes.OK);

    var nonTextualNameUnderMask =
        executeRequest(
            handler,
            HttpRequest.PATCH(url + "?update_mask=summary")
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"name\":123,\"summary\":null}"));
    assertThat(nonTextualNameUnderMask.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }
}
