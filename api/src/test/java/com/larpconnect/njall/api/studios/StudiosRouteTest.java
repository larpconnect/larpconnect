package com.larpconnect.njall.api.studios;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.Directives;
import org.apache.pekko.japi.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class StudiosRouteTest {

  private static final StudioErrorResponse NOT_FOUND_BODY =
      new StudioErrorResponse(404, "Studio not found");
  private static final StudioErrorResponse INTERNAL_ERROR_BODY =
      new StudioErrorResponse(500, "Internal server error");

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "studios-route-test");
    objectMapper =
        JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, SECONDS);
  }

  private static HttpResponse executeGet(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, String path) throws Exception {
    return handler.apply(HttpRequest.GET(path)).toCompletableFuture().get(5, SECONDS);
  }

  private static StudioErrorResponse errorBody(HttpResponse response) throws Exception {
    var entity = response.entity().toStrict(5000, system).toCompletableFuture().get(5, SECONDS);
    return objectMapper.readValue(entity.getData().utf8String(), StudioErrorResponse.class);
  }

  private static ActorRef<StudioCommand> replyingActor(StudioActorResponse reply) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              if (msg instanceof StudioCommand.GetStudio cmd) {
                cmd.replyTo().tell(reply);
              }
              return Behaviors.same();
            }),
        "studioActorReply" + UUID.randomUUID(),
        Props.empty());
  }

  private static ActorRef<StudioCommand> silentActor() {
    return system.systemActorOf(
        Behaviors.empty(), "studioActorSilent" + UUID.randomUUID(), Props.empty());
  }

  private StudiosRoute createRoute(StudioLookupCache cache, ActorRef<StudioCommand> actor) {
    var subRoutes = new StudioSubRoutes(Directives.reject());
    return new DefaultStudiosRoute(cache, actor, subRoutes, system, objectMapper);
  }

  private StudiosRoute createRoute(
      StudioLookupCache cache, ActorRef<StudioCommand> actor, Duration timeout) {
    var subRoutes = new StudioSubRoutes(Directives.reject());
    return new DefaultStudiosRoute(cache, actor, subRoutes, system, objectMapper, timeout);
  }

  private StudioLookup sampleLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  private StudioLookup sampleDeletedLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.of(now));
  }

  private StudioLookupCache cacheWithValiant(StudioLookup lookup) {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(lookup));
    return cache;
  }

  private HttpResponse getValiantWithReply(StudioActorResponse reply) throws Exception {
    var route = createRoute(cacheWithValiant(sampleLookup()), replyingActor(reply));
    return executeGet(route.route().seal().function(system), "/api/studios/valiant/v1/studio");
  }

  private HttpResponse getWithoutCacheHit(String path, StudioLookupCache cache) throws Exception {
    var route = createRoute(cache, silentActor());
    return executeGet(route.route().seal().function(system), path);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 200 OK when found in cache and actor")
  void getStudio_success() throws Exception {
    var studioResponse = new StudioResponse(studioId, "valiant", "Valiant Games");

    var response = getValiantWithReply(StudioActorResponse.success(studioResponse));

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 404 when cache does not contain studio")
  void getStudio_cacheMiss_returnsNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("missing")).thenReturn(Optional.empty());

    var response = getWithoutCacheHit("/api/studios/missing/v1/studio", cache);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns static 404 payload on cache miss")
  void getStudio_cacheMiss_returnsStaticNotFoundBody() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("missing")).thenReturn(Optional.empty());

    var response = getWithoutCacheHit("/api/studios/missing/v1/studio", cache);

    assertThat(errorBody(response)).isEqualTo(NOT_FOUND_BODY);
  }

  @Test
  @DisplayName(
      "GET /api/studios/{alias}/v1/studio returns 404 when studio in cache is soft-deleted")
  void getStudio_softDeletedInCache_returnsNotFound() throws Exception {
    var cache = cacheWithValiant(sampleDeletedLookup());

    var response = getWithoutCacheHit("/api/studios/valiant/v1/studio", cache);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 404 when actor replies with NotFound")
  void getStudio_actorRepliesNotFound() throws Exception {
    var response = getValiantWithReply(StudioActorResponse.notFound("Studio missing"));

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio hides tenant id from actor NotFound message")
  void getStudio_actorRepliesNotFound_hidesTenantId() throws Exception {
    var internal = StudioActorResponse.notFound("Studio not found for tenant: " + tenantId);

    var response = getValiantWithReply(internal);

    assertThat(errorBody(response)).isEqualTo(NOT_FOUND_BODY);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 500 when actor replies with Failure")
  void getStudio_actorRepliesFailure() throws Exception {
    var response = getValiantWithReply(StudioActorResponse.failure("Query error"));

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio hides actor Failure detail from client")
  void getStudio_actorRepliesFailure_hidesDetail() throws Exception {
    var response = getValiantWithReply(StudioActorResponse.failure("Sensitive SQL detail"));

    assertThat(errorBody(response)).isEqualTo(INTERNAL_ERROR_BODY);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 500 when actor times out")
  void getStudio_actorTimesOut() throws Exception {
    var cache = cacheWithValiant(sampleLookup());
    var route = createRoute(cache, silentActor(), Duration.ofMillis(50));

    var response =
        executeGet(route.route().seal().function(system), "/api/studios/valiant/v1/studio");

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns static 500 payload on actor timeout")
  void getStudio_actorTimesOut_returnsStaticBody() throws Exception {
    var cache = cacheWithValiant(sampleLookup());
    var route = createRoute(cache, silentActor(), Duration.ofMillis(50));

    var response =
        executeGet(route.route().seal().function(system), "/api/studios/valiant/v1/studio");

    assertThat(errorBody(response)).isEqualTo(INTERNAL_ERROR_BODY);
  }

  @Test
  @DisplayName("GET /api/studios/{control}/v1/studio returns static 404 without cache lookup")
  void getStudio_controlCharacterId_returnsNotFoundWithoutLookup() throws Exception {
    var cache = mock(StudioLookupCache.class);

    var response = getWithoutCacheHit("/api/studios/%07/v1/studio", cache);

    assertThat(errorBody(response)).isEqualTo(NOT_FOUND_BODY);
    verifyNoInteractions(cache);
  }

  @Test
  @DisplayName("GET /api/studios/{blank}/v1/studio returns static 404 without cache lookup")
  void getStudio_blankId_returnsNotFoundWithoutLookup() throws Exception {
    var cache = mock(StudioLookupCache.class);

    var response = getWithoutCacheHit("/api/studios/%20%20/v1/studio", cache);

    assertThat(errorBody(response)).isEqualTo(NOT_FOUND_BODY);
    verifyNoInteractions(cache);
  }

  @Test
  @DisplayName("GET /api/studios/{too-long}/v1/studio returns static 404 without cache lookup")
  void getStudio_overlongId_returnsNotFoundWithoutLookup() throws Exception {
    var cache = mock(StudioLookupCache.class);

    var response = getWithoutCacheHit("/api/studios/" + "a".repeat(129) + "/v1/studio", cache);

    assertThat(errorBody(response)).isEqualTo(NOT_FOUND_BODY);
    verifyNoInteractions(cache);
  }
}
