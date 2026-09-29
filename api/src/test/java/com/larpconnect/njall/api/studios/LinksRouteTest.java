package com.larpconnect.njall.api.studios;

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

final class LinksRouteTest {

  private static final String CREATE_BODY =
      "{\"linkType\":\"website\",\"url\":\"https://valiant.example.com\"}";
  private static final String PATCH_BODY = "{\"url\":\"https://updated.example.com\"}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID linkId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "links-route-test");
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

  private LinkResponse sampleLinkResponse() {
    return new LinkResponse(
        linkId,
        "website",
        "https://valiant.example.com",
        "text/html",
        Optional.of("Homepage"),
        now,
        now);
  }

  private ActorRef<LinkCommand> createActor(LinkActorResponse response) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              switch (msg) {
                case LinkCommand.CreateLink c -> c.replyTo().tell(response);
                case LinkCommand.GetLink g -> g.replyTo().tell(response);
                case LinkCommand.PatchLink p -> p.replyTo().tell(response);
                case LinkCommand.DeleteLink d -> d.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testActor" + UUID.randomUUID(),
        Props.empty());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache, ActorRef<LinkCommand> actor) {
    return new LinksRoute(cache, actor, system, objectMapper).route().seal().function(system);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/links returns 201 Created on success")
  void postLink_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler =
        createHandler(cache, createActor(LinkActorResponse.success(sampleLinkResponse())));
    var request =
        HttpRequest.POST("/api/studios/valiant/v1/links")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/links returns 400 when actor reports BadRequest")
  void postLink_badRequest() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler =
        createHandler(cache, createActor(LinkActorResponse.badRequest("Invalid linkType")));
    var request =
        HttpRequest.POST("/api/studios/valiant/v1/links")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/links returns 404 when studio not in cache")
  void postLink_studioNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());
    var handler = createHandler(cache, createActor(LinkActorResponse.badRequest("unused")));
    var request =
        HttpRequest.POST("/api/studios/unknown/v1/links")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/links returns 404 when studio is deleted")
  void postLink_studioDeleted() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleDeletedLookup()));
    var handler = createHandler(cache, createActor(LinkActorResponse.badRequest("unused")));
    var request =
        HttpRequest.POST("/api/studios/valiant/v1/links")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/links returns 500 when actor reports Failure")
  void postLink_actorFailure() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler = createHandler(cache, createActor(LinkActorResponse.failure("DB error")));
    var request =
        HttpRequest.POST("/api/studios/valiant/v1/links")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_BODY);
    assertThat(executeRequest(handler, request).status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links/{id} returns 200 OK on success")
  void getLink_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler =
        createHandler(cache, createActor(LinkActorResponse.success(sampleLinkResponse())));
    var request = HttpRequest.GET("/api/studios/valiant/v1/links/" + linkId);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links/{id} returns 404 when studio not in cache")
  void getLink_studioNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());
    var handler = createHandler(cache, createActor(LinkActorResponse.badRequest("unused")));
    var request = HttpRequest.GET("/api/studios/unknown/v1/links/" + linkId);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links/{id} returns 404 when linkId invalid")
  void getLink_invalidLinkId() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler = createHandler(cache, createActor(LinkActorResponse.badRequest("unused")));
    var request = HttpRequest.GET("/api/studios/valiant/v1/links/not-a-uuid");
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links/{id} returns 500 when actor reports Failure")
  void getLink_actorFailure() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler = createHandler(cache, createActor(LinkActorResponse.failure("DB error")));
    var request = HttpRequest.GET("/api/studios/valiant/v1/links/" + linkId);
    assertThat(executeRequest(handler, request).status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links/{id} returns 500 when ask fails")
  void getLink_askFailure() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    ActorRef<LinkCommand> deadActor =
        system.systemActorOf(
            Behaviors.stopped(), "stoppedActor" + UUID.randomUUID(), Props.empty());
    var handler =
        new LinksRoute(cache, deadActor, system, objectMapper, Duration.ofMillis(50))
            .route()
            .seal()
            .function(system);
    var request = HttpRequest.GET("/api/studios/valiant/v1/links/" + linkId);
    assertThat(executeRequest(handler, request).status())
        .isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("PATCH /api/studios/{alias}/v1/links/{id} returns 200 OK on success")
  void patchLink_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler =
        createHandler(cache, createActor(LinkActorResponse.success(sampleLinkResponse())));
    var request =
        HttpRequest.PATCH("/api/studios/valiant/v1/links/" + linkId)
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("PATCH /api/studios/{alias}/v1/links/{id} returns 404 when link not found")
  void patchLink_notFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler = createHandler(cache, createActor(LinkActorResponse.notFound("Link not found")));
    var request =
        HttpRequest.PATCH("/api/studios/valiant/v1/links/" + linkId)
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_BODY);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("PATCH /api/studios/{alias}/v1/links/{id} returns 400 when actor reports BadRequest")
  void patchLink_badRequest() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler = createHandler(cache, createActor(LinkActorResponse.badRequest("Invalid URL")));
    var request =
        HttpRequest.PATCH("/api/studios/valiant/v1/links/" + linkId)
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"url\":\"bad\"}");
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("DELETE /api/studios/{alias}/v1/links/{id} returns 204 No Content on success")
  void deleteLink_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler = createHandler(cache, createActor(LinkActorResponse.deleted()));
    var request = HttpRequest.DELETE("/api/studios/valiant/v1/links/" + linkId);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("DELETE /api/studios/{alias}/v1/links/{id} returns 404 when link not found")
  void deleteLink_notFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    var handler = createHandler(cache, createActor(LinkActorResponse.notFound("Link not found")));
    var request = HttpRequest.DELETE("/api/studios/valiant/v1/links/" + linkId);
    assertThat(executeRequest(handler, request).status()).isEqualTo(StatusCodes.NOT_FOUND);
  }
}
