package com.larpconnect.njall.api.studios.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.StudioLookup;
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

final class EventsRouteTest {

  private static final String CREATE_EVENT_BODY =
      "{\"title\":\"Autumn Harvest Festival\",\"summary\":\"Gathering\"}";
  private static final String PATCH_EVENT_BODY =
      "{\"title\":\"Autumn Harvest Festival - Rescheduled\"}";

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID eventId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "events-route-test");
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

  private EventResponse sampleEventResponse() {
    return new EventResponse(
        eventId,
        "Autumn Harvest Festival",
        Optional.of("Gathering"),
        Optional.of(locationId),
        Optional.of(now),
        Optional.of(now.plusSeconds(3600)),
        now,
        now);
  }

  private ActorRef<EventCommand> createEventActor(EventActorResponse response) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              switch (msg) {
                case EventCommand.CreateEvent c -> c.replyTo().tell(response);
                case EventCommand.GetEvent g -> g.replyTo().tell(response);
                case EventCommand.ListEvents l -> l.replyTo().tell(response);
                case EventCommand.PatchEvent p -> p.replyTo().tell(response);
                case EventCommand.DeleteEvent d -> d.replyTo().tell(response);
              }
              return Behaviors.same();
            }),
        "testEventActor" + UUID.randomUUID(),
        Props.empty());
  }

  private Function<HttpRequest, CompletionStage<HttpResponse>> createHandler(
      StudioLookupCache cache, ActorRef<EventCommand> actor) {
    return new EventsRoute(cache, actor, system, objectMapper).route().seal().function(system);
  }

  @Test
  @DisplayName("POST /api/studios/{studio}/v1/events creates an event and returns 201")
  void createEvent_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createEventActor(EventActorResponse.success(sampleEventResponse()));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/events")
            .withEntity(ContentTypes.APPLICATION_JSON, CREATE_EVENT_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("GET /api/studios/{studio}/v1/events lists active events and returns 200")
  void listEvents_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createEventActor(EventActorResponse.items(ImmutableList.of(sampleEventResponse())));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/events");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/studios/{studio}/v1/events/{id} returns 200 when event exists")
  void getEvent_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createEventActor(EventActorResponse.success(sampleEventResponse()));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/events/" + eventId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/studios/{studio}/v1/events/{id} returns 404 when missing")
  void getEvent_notFound() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createEventActor(EventActorResponse.notFound("Event not found"));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/events/" + eventId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("PATCH /api/studios/{studio}/v1/events/{id} applies update and returns 200")
  void patchEvent_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createEventActor(EventActorResponse.success(sampleEventResponse()));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.PATCH("/api/studios/valiant/v1/events/" + eventId + "?update_mask=title")
            .withEntity(ContentTypes.APPLICATION_JSON, PATCH_EVENT_BODY);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("DELETE /api/studios/{studio}/v1/events/{id} soft-deletes and returns 204")
  void deleteEvent_success() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createEventActor(EventActorResponse.deleted());
    var handler = createHandler(cache, actor);

    var request = HttpRequest.DELETE("/api/studios/valiant/v1/events/" + eventId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NO_CONTENT);
  }

  @Test
  @DisplayName("Returns 404 when studio is not found in cache")
  void studioNotFound_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("unknown")).thenReturn(Optional.empty());

    var actor = createEventActor(EventActorResponse.notFound("Studio not found"));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/unknown/v1/events");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("Returns 404 when event UUID is invalid")
  void invalidEventId_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createEventActor(EventActorResponse.notFound("Event not found"));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/events/not-a-uuid");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("Returns 400 when actor emits BadRequest")
  void actorBadRequest_returns400() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createEventActor(EventActorResponse.badRequest("Invalid title"));
    var handler = createHandler(cache, actor);

    var request =
        HttpRequest.POST("/api/studios/valiant/v1/events")
            .withEntity(ContentTypes.APPLICATION_JSON, "{\"title\":\"\"}");
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("Returns 500 when actor emits Failure")
  void actorFailure_returns500() throws Exception {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));

    var actor = createEventActor(EventActorResponse.failure("Internal DB error"));
    var handler = createHandler(cache, actor);

    var request = HttpRequest.GET("/api/studios/valiant/v1/events/" + eventId);
    var response = executeRequest(handler, request);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
