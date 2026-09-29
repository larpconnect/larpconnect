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

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/links returns 201 Created on success")
  void postLink_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var linkResp = sampleLinkResponse();
    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof LinkCommand.CreateLink cmd) {
                    cmd.replyTo().tell(LinkActorResponse.success(linkResp));
                  }
                  return Behaviors.same();
                }),
            "linkCreateActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var body =
        """
        {"linkType":"website","url":"https://valiant.example.com"}
        """;
    var request =
        HttpRequest.POST("/api/studios/valiant/v1/links")
            .withEntity(ContentTypes.APPLICATION_JSON, body);

    var response = executeRequest(handler, request);
    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/links returns 400 when actor reports BadRequest")
  void postLink_badRequest() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof LinkCommand.CreateLink cmd) {
                    cmd.replyTo().tell(LinkActorResponse.badRequest("Invalid linkType"));
                  }
                  return Behaviors.same();
                }),
            "linkCreateBadActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/links")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"linkType\":\"\",\"url\":\"\"}");

    var response = executeRequest(handler, request);
    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/links returns 404 when studio does not exist")
  void postLink_studioMissing_returnsNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());

    ActorRef<LinkCommand> actor =
        system.systemActorOf(Behaviors.empty(), "linkActorNoop" + UUID.randomUUID(), Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var request =
        HttpRequest.POST("/api/studios/unknown/v1/links")
            .withEntity(
                ContentTypes.APPLICATION_JSON,
                "{\"linkType\":\"website\",\"url\":\"https://example.com\"}");

    var response = executeRequest(handler, request);
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/links returns 404 when studio is soft-deleted")
  void postLink_studioDeleted_returnsNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleDeletedLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.empty(), "linkActorNoopDeleted" + UUID.randomUUID(), Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/links")
            .withEntity(
                ContentTypes.APPLICATION_JSON,
                "{\"linkType\":\"website\",\"url\":\"https://example.com\"}");

    var response = executeRequest(handler, request);
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links/{id} returns 200 OK when found")
  void getLink_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var linkResp = sampleLinkResponse();
    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof LinkCommand.GetLink cmd) {
                    cmd.replyTo().tell(LinkActorResponse.success(linkResp));
                  }
                  return Behaviors.same();
                }),
            "linkGetActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executeRequest(handler, HttpRequest.GET("/api/studios/valiant/v1/links/" + linkId));
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links/{id} returns 404 when link not found")
  void getLink_notFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof LinkCommand.GetLink cmd) {
                    cmd.replyTo().tell(LinkActorResponse.notFound("Link not found"));
                  }
                  return Behaviors.same();
                }),
            "linkGetNotFoundActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executeRequest(handler, HttpRequest.GET("/api/studios/valiant/v1/links/" + linkId));
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links/{id} returns 404 when linkId is not UUID")
  void getLink_nonUuid_returnsNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.empty(), "linkActorNoop2" + UUID.randomUUID(), Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executeRequest(handler, HttpRequest.GET("/api/studios/valiant/v1/links/not-a-uuid"));
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("PATCH /api/studios/{alias}/v1/links/{id} returns 200 OK on success")
  void patchLink_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var linkResp = sampleLinkResponse();
    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof LinkCommand.PatchLink cmd) {
                    cmd.replyTo().tell(LinkActorResponse.success(linkResp));
                  }
                  return Behaviors.same();
                }),
            "linkPatchActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var body = "{\"url\":\"https://updated.example.com\"}";
    var request =
        HttpRequest.PATCH("/api/studios/valiant/v1/links/" + linkId + "?update_mask=url")
            .withEntity(ContentTypes.APPLICATION_JSON, body);

    var response = executeRequest(handler, request);
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("DELETE /api/studios/{alias}/v1/links/{id} returns 204 No Content on success")
  void deleteLink_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof LinkCommand.DeleteLink cmd) {
                    cmd.replyTo().tell(LinkActorResponse.deleted());
                  }
                  return Behaviors.same();
                }),
            "linkDeleteActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executeRequest(handler, HttpRequest.DELETE("/api/studios/valiant/v1/links/" + linkId));
    assertThat(response.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links rejects listing with 405 Method Not Allowed")
  void getLinks_listingRejected() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.empty(), "linkActorNoop3" + UUID.randomUUID(), Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeRequest(handler, HttpRequest.GET("/api/studios/valiant/v1/links"));
    assertThat(response.status()).isEqualTo(StatusCodes.METHOD_NOT_ALLOWED);
  }

  @Test
  @DisplayName("POST /api/studios/{alias}/v1/links returns 500 when actor reports Failure")
  void postLink_actorFailure_returns500() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof LinkCommand.CreateLink cmd) {
                    cmd.replyTo().tell(LinkActorResponse.failure("Failed creation"));
                  }
                  return Behaviors.same();
                }),
            "linkCreateFailActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var body = "{\"linkType\":\"website\",\"url\":\"https://valiant.example.com\"}";
    var response =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/links")
                .withEntity(ContentTypes.APPLICATION_JSON, body));
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links/{id} returns 500 when actor reports Failure")
  void getLink_actorFailure_returns500() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof LinkCommand.GetLink cmd) {
                    cmd.replyTo().tell(LinkActorResponse.failure("DB error"));
                  }
                  return Behaviors.same();
                }),
            "linkGetFailActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executeRequest(handler, HttpRequest.GET("/api/studios/valiant/v1/links/" + linkId));
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/links/{id} returns 500 when ask fails")
  void getLink_actorAskFailure_returns500() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  return Behaviors.stopped();
                }),
            "linkAskFailActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper, Duration.ofMillis(50));
    var handler = route.route().seal().function(system);

    var response =
        executeRequest(handler, HttpRequest.GET("/api/studios/valiant/v1/links/" + linkId));
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("DELETE /api/studios/{alias}/v1/links/{id} returns 404 when link not found")
  void deleteLink_notFound_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof LinkCommand.DeleteLink cmd) {
                    cmd.replyTo().tell(LinkActorResponse.notFound("Link not found"));
                  }
                  return Behaviors.same();
                }),
            "linkDeleteNotFoundActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executeRequest(handler, HttpRequest.DELETE("/api/studios/valiant/v1/links/" + linkId));
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("PATCH /api/studios/{alias}/v1/links/{id} returns 400 when actor reports BadRequest")
  void patchLink_badRequest_returns400() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<LinkCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof LinkCommand.PatchLink cmd) {
                    cmd.replyTo().tell(LinkActorResponse.badRequest("Invalid URL"));
                  }
                  return Behaviors.same();
                }),
            "linkPatchBadActor" + UUID.randomUUID(),
            Props.empty());

    var route = new LinksRoute(cache, actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executeRequest(
            handler,
            HttpRequest.PATCH("/api/studios/valiant/v1/links/" + linkId)
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"url\":\"bad\"}"));
    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }
}
