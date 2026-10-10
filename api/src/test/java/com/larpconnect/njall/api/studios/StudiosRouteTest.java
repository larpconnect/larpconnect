package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.larpconnect.njall.api.studios.links.LinksRoute;
import com.larpconnect.njall.api.studios.locations.LocationsRoute;
import com.larpconnect.njall.api.studios.tags.TagsRoute;
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
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static HttpResponse executeGet(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, String path) throws Exception {
    return handler.apply(HttpRequest.GET(path)).toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private StudiosRoute createRoute(StudioLookupCache cache, ActorRef<StudioCommand> actor) {
    return createRoute(cache, actor, Duration.ofSeconds(20));
  }

  private StudiosRoute createRoute(
      StudioLookupCache cache, ActorRef<StudioCommand> actor, Duration timeout) {
    var linksRoute = mock(LinksRoute.class);
    var locationsRoute = mock(LocationsRoute.class);
    var tagsRoute = mock(TagsRoute.class);
    when(linksRoute.route()).thenReturn(Directives.reject());
    when(locationsRoute.route()).thenReturn(Directives.reject());
    when(tagsRoute.route()).thenReturn(Directives.reject());
    return new StudiosRoute(
        cache, actor, linksRoute, locationsRoute, tagsRoute, system, objectMapper, timeout);
  }

  private StudioLookup sampleLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.empty());
  }

  private StudioLookup sampleDeletedLookup() {
    return new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.of(now));
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 200 OK when found in cache and actor")
  void getStudio_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var studioResponse = new StudioResponse(studioId, "valiant", "Valiant Games");
    ActorRef<StudioCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioCommand.GetStudio cmd) {
                    cmd.replyTo().tell(StudioActorResponse.success(studioResponse));
                  }
                  return Behaviors.same();
                }),
            "studioActorSuccess" + UUID.randomUUID(),
            Props.empty());

    var route = createRoute(cache, actor);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/studios/valiant/v1/studio");
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 404 when cache does not contain studio")
  void getStudio_cacheMiss_returnsNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("missing")).thenReturn(Optional.empty());

    ActorRef<StudioCommand> actor =
        system.systemActorOf(
            Behaviors.empty(), "studioActorUnreached" + UUID.randomUUID(), Props.empty());

    var route = createRoute(cache, actor);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/studios/missing/v1/studio");
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName(
      "GET /api/studios/{alias}/v1/studio returns 404 when studio in cache is soft-deleted")
  void getStudio_softDeletedInCache_returnsNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleDeletedLookup()));

    ActorRef<StudioCommand> actor =
        system.systemActorOf(
            Behaviors.empty(), "studioActorDeletedUnreached" + UUID.randomUUID(), Props.empty());

    var route = createRoute(cache, actor);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/studios/valiant/v1/studio");
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 404 when actor replies with NotFound")
  void getStudio_actorRepliesNotFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<StudioCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioCommand.GetStudio cmd) {
                    cmd.replyTo().tell(StudioActorResponse.notFound("Studio missing"));
                  }
                  return Behaviors.same();
                }),
            "studioActorNotFound" + UUID.randomUUID(),
            Props.empty());

    var route = createRoute(cache, actor);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/studios/valiant/v1/studio");
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 500 when actor replies with Failure")
  void getStudio_actorRepliesFailure() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<StudioCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioCommand.GetStudio cmd) {
                    cmd.replyTo().tell(StudioActorResponse.failure("Query error"));
                  }
                  return Behaviors.same();
                }),
            "studioActorFailure" + UUID.randomUUID(),
            Props.empty());

    var route = createRoute(cache, actor);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/studios/valiant/v1/studio");
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 500 when actor times out")
  void getStudio_actorTimesOut() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    ActorRef<StudioCommand> actor =
        system.systemActorOf(
            Behaviors.empty(), "studioActorTimeout" + UUID.randomUUID(), Props.empty());

    var route = createRoute(cache, actor, Duration.ofMillis(50));
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/studios/valiant/v1/studio");
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
