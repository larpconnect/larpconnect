package com.larpconnect.njall.api.studios.tags;

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
import org.apache.pekko.http.javadsl.model.HttpMethods;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.japi.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class TagsRouteTest {

  private static final String CREATE_BODY =
      "{\"tag\":\"SolarPunk\",\"summary\":\"Eco-futuristic\"}";
  private static final String BATCH_CREATE_BODY =
      "{\"requests\":[{\"tag\":\"SolarPunk\"},{\"tag\":\"CyberPunk\"}]}";
  private static final String PATCH_BODY = "{\"tag\":\"GreenPunk\"}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID tagId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "tags-route-test");
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

  private TagResponse sampleTagResponse() {
    return new TagResponse(
        tagId,
        "SolarPunk",
        "hashtag",
        "/api/studios/valiant/v1/tags/SolarPunk",
        "application/json",
        Optional.of("Eco-futuristic"),
        now,
        now);
  }

  private ActorRef<TagCommand> createActor(TagActorResponse response) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              switch (msg) {
                case TagCommand.CreateTags c -> c.replyTo().tell(response);
                case TagCommand.QueryTag q -> q.replyTo().tell(response);
                case TagCommand.PatchTag p -> p.replyTo().tell(response);
                case TagCommand.DeleteTag d -> d.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testActor" + UUID.randomUUID(),
        Props.empty());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache, ActorRef<TagCommand> actor) {
    return new TagsRoute(cache, actor, system, objectMapper, Duration.ofSeconds(20))
        .route()
        .seal()
        .function(system);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/tags returns 201 Created when newly created")
  void postTag_new_returns201() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.success(sampleTagResponse(), true));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/tags")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/tags returns 200 OK when already exists")
  void postTag_existing_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.success(sampleTagResponse(), false));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/tags")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/tags returns error status when actor fails")
  void postTag_failure_returnsError() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.badRequest("Invalid tag"));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/tags")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/tags:batchCreate returns 200 OK")
  void postBatchCreate_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.items(ImmutableList.of(sampleTagResponse())));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/tags:batchCreate")
            .withEntity(ContentTypes.APPLICATION_JSON, BATCH_CREATE_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/tags/:batchCreate alias path returns 200 OK")
  void postBatchCreate_aliasPath_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.items(ImmutableList.of(sampleTagResponse())));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/tags/:batchCreate")
            .withEntity(ContentTypes.APPLICATION_JSON, BATCH_CREATE_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName(
      "POST /api/studios/{alias}/v1/tags:batchCreate returns error status on actor failure")
  void postBatchCreate_failure_returnsError() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.badRequest("Empty batch"));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/tags:batchCreate")
            .withEntity(ContentTypes.APPLICATION_JSON, BATCH_CREATE_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/tags:batchCreate returns 404 for unknown studio")
  void postBatchCreate_unknownStudio_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());

    var actor = createActor(TagActorResponse.items(ImmutableList.of()));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/unknown/v1/tags:batchCreate")
            .withEntity(ContentTypes.APPLICATION_JSON, BATCH_CREATE_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/tags returns 200 OK with list")
  void listTags_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.items(ImmutableList.of(sampleTagResponse())));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/tags");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/tags/{id} returns 200 OK when found")
  void getTag_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.success(sampleTagResponse(), false));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/tags/SolarPunk");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/tags/{id} returns 404 when actor replies not found")
  void getTag_notFound_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.notFound("Tag not found"));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/tags/Unknown");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("PATCH /api/studios/{alias}/v1/tags/{id} returns 200 OK")
  void patchTag_returns200() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.success(sampleTagResponse(), false));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.create("/api/studios/valiant/v1/tags/SolarPunk?update_mask=tag")
            .withMethod(HttpMethods.PATCH)
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("DELETE /api/studios/{alias}/v1/tags/{id} returns 204 No Content")
  void deleteTag_returns204() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createActor(TagActorResponse.deleted());
    var handler = createHandler(cache, actor);

    var request = HttpRequest.DELETE("/api/studios/valiant/v1/tags/SolarPunk");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("Unknown studio returns 404 Not Found")
  void unknownStudio_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());

    var actor = createActor(TagActorResponse.deleted());
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/unknown/v1/tags");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("Deleted studio returns 404 Not Found")
  void deletedStudio_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var deletedLookup = new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.of(now));
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(deletedLookup));

    var actor = createActor(TagActorResponse.deleted());
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/tags");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("Actor failure returns 500 Internal Server Error")
  void actorFailure_returns500() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<TagCommand> stoppedActor =
        system.systemActorOf(
            Behaviors.stopped(), "stoppedActor" + UUID.randomUUID(), Props.empty());
    var handler =
        new TagsRoute(cache, stoppedActor, system, objectMapper, Duration.ofMillis(100))
            .route()
            .seal()
            .function(system);

    var request = HttpRequest.GET("/api/studios/valiant/v1/tags");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
