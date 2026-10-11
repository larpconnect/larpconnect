package com.larpconnect.njall.api.studios.individuals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
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

final class IndividualsRouteTest {

  private static final String CREATE_INDIVIDUAL_BODY =
      "{\"name\":\"Jane Eyre\",\"summary\":\"Visiting scholar\"}";
  private static final String PATCH_INDIVIDUAL_BODY = "{\"name\":\"Jane Rochester\"}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID individualId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "individuals-route-test");
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

  private IndividualResponse sampleIndividualResponse() {
    return new IndividualResponse(
        individualId, "Jane Eyre", Optional.of("Visiting scholar"), now, now);
  }

  private ActorRef<IndividualCommand> createIndividualActor(IndividualActorResponse response) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              switch (msg) {
                case IndividualCommand.CreateIndividual c -> c.replyTo().tell(response);
                case IndividualCommand.GetIndividual g -> g.replyTo().tell(response);
                case IndividualCommand.PatchIndividual p -> p.replyTo().tell(response);
                case IndividualCommand.DeleteIndividual d -> d.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testIndividualActor" + UUID.randomUUID(),
        Props.empty());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache, ActorRef<IndividualCommand> actor) {
    return new IndividualsRoute(cache, actor, system, objectMapper).route().seal().function(system);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/individuals returns 201 Created on success")
  void postIndividual_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createIndividualActor(IndividualActorResponse.success(sampleIndividualResponse()));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/individuals")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_INDIVIDUAL_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("POST returns 404 when studio not found")
  void postIndividual_studioNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());

    var actor = createIndividualActor(IndividualActorResponse.success(sampleIndividualResponse()));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/unknown/v1/individuals")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_INDIVIDUAL_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/individuals (collection) returns 404 (no list)")
  void getIndividuals_noListEndpoint() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createIndividualActor(IndividualActorResponse.success(sampleIndividualResponse()));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/individuals");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/individuals/{id} returns 200 OK when found")
  void getIndividual_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createIndividualActor(IndividualActorResponse.success(sampleIndividualResponse()));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/individuals/" + individualId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET returns 404 when individual ID is invalid UUID")
  void getIndividual_invalidUuid() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createIndividualActor(IndividualActorResponse.success(sampleIndividualResponse()));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/individuals/not-a-uuid");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET returns 404 when individual not found in actor")
  void getIndividual_notFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createIndividualActor(IndividualActorResponse.notFound("Not found"));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/individuals/" + individualId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("PATCH /api/studios/{alias}/v1/individuals/{id} returns 200 OK on success")
  void patchIndividual_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createIndividualActor(IndividualActorResponse.success(sampleIndividualResponse()));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.PATCH(
                "/api/studios/valiant/v1/individuals/" + individualId + "?update_mask=name")
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_INDIVIDUAL_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("PATCH returns 400 when validation fails in actor")
  void patchIndividual_badRequest() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createIndividualActor(IndividualActorResponse.badRequest("Name blank"));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.PATCH("/api/studios/valiant/v1/individuals/" + individualId)
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_INDIVIDUAL_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("DELETE /api/studios/{alias}/v1/individuals/{id} returns 204 No Content on delete")
  void deleteIndividual_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createIndividualActor(IndividualActorResponse.deleted());
    var handler = createHandler(cache, actor);

    var request = HttpRequest.DELETE("/api/studios/valiant/v1/individuals/" + individualId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("GET returns 404 when studio in cache is soft-deleted")
  void getIndividual_softDeletedStudio_returnsNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleDeletedLookup()));

    var actor = createIndividualActor(IndividualActorResponse.success(sampleIndividualResponse()));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/individuals/" + individualId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET returns 500 when actor communication fails")
  void getIndividual_actorFailure_returns500() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<IndividualCommand> failingActor =
        system.systemActorOf(
            Behaviors.receiveMessage(msg -> Behaviors.stopped()),
            "failingIndividualActor" + UUID.randomUUID(),
            Props.empty());

    var route =
        new IndividualsRoute(cache, failingActor, system, objectMapper, Duration.ofMillis(50));
    var handler = route.route().seal().function(system);

    var request = HttpRequest.GET("/api/studios/valiant/v1/individuals/" + individualId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
