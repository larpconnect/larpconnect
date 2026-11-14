package com.larpconnect.njall.api.studios.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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
      "{\"generator\":\"019273c5-9271-7000-8000-000000000001\","
          + "\"title\":\"Autumn Harvest Festival\",\"summary\":\"Gathering\"}";
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
        "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001",
        "Autumn Harvest Festival",
        Optional.of("Gathering"),
        EventType.EVENT_TYPE_UNSPECIFIED,
        Optional.of(locationId),
        Optional.of(now),
        Optional.of(now.plusSeconds(3600)),
        now,
        now);
  }

  private ActorRef<EventCommand> createEventActor(EventActorResponse response) {
    return createEventActor(response, cmd -> {});
  }

  private ActorRef<EventCommand> createEventActor(
      EventActorResponse response, Consumer<EventCommand> recorder) {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              recorder.accept(msg);
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

  @Test
  @DisplayName("POST /v1/events rejects malformed, empty, and non-object JSON bodies with 400")
  void postMalformedOrEmptyBody_returnsAip193BadRequest() throws Exception {
    var cache = activeCache();
    var actor = createEventActor(EventActorResponse.success(sampleEventResponse()));
    var handler = createHandler(cache, actor);

    var malformed =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/events")
                .withEntity(ContentTypes.APPLICATION_JSON, "{not json"));
    assertThat(malformed.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBody(malformed)).contains("\"code\":400", "\"message\"");

    var empty = executeRequest(handler, HttpRequest.POST("/api/studios/valiant/v1/events"));
    assertThat(empty.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    var emptyJson = objectMapper.readTree(readBody(empty));
    assertThat(emptyJson.get("code").asInt()).isEqualTo(400);
    assertThat(emptyJson.get("message").asText()).isNotBlank();

    var arrayBody =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/events")
                .withEntity(ContentTypes.APPLICATION_JSON, "[]"));
    assertThat(arrayBody.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBody(arrayBody)).contains("Request body must be a JSON object");
  }

  @Test
  @DisplayName("POST and PATCH reject null/non-string eventType, unknown enum, and null title")
  void invalidEventTypeAndNullTitle_returnsAip193BadRequest() throws Exception {
    var cache = activeCache();
    var actor = createEventActor(EventActorResponse.success(sampleEventResponse()));
    var handler = createHandler(cache, actor);

    for (var badBody :
        ImmutableList.of(
            "{\"generator\":\"" + UUID.randomUUID() + "\",\"title\":\"Fest\",\"eventType\":null}",
            "{\"generator\":\"" + UUID.randomUUID() + "\",\"title\":\"Fest\",\"eventType\":0}",
            "{\"generator\":\""
                + UUID.randomUUID()
                + "\",\"title\":\"Fest\",\"eventType\":\"UNKNOWN\"}")) {
      var res =
          executeRequest(
              handler,
              HttpRequest.POST("/api/studios/valiant/v1/events")
                  .withEntity(ContentTypes.APPLICATION_JSON, badBody));
      assertThat(res.status()).isEqualTo(StatusCodes.BAD_REQUEST);
      assertThat(readBody(res)).contains("\"code\":400");
    }

    var nullTitlePatch =
        executeRequest(
            handler,
            HttpRequest.PATCH("/api/studios/valiant/v1/events/" + eventId)
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"title\":null}"));
    assertThat(nullTitlePatch.status()).isEqualTo(StatusCodes.BAD_REQUEST);
    assertThat(readBody(nullTitlePatch)).contains("Event title must be a string");

    var unmaskedNullRequiredPatch =
        executeRequest(
            handler,
            HttpRequest.PATCH("/api/studios/valiant/v1/events/" + eventId + "?update_mask=summary")
                .withEntity(
                    ContentTypes.APPLICATION_JSON,
                    "{\"title\":null,\"eventType\":null,\"summary\":\"Updated\"}"));
    assertThat(unmaskedNullRequiredPatch.status()).isEqualTo(StatusCodes.OK);

    var nonTextualUnderMaskPatch =
        executeRequest(
            handler,
            HttpRequest.PATCH("/api/studios/valiant/v1/events/" + eventId + "?update_mask=summary")
                .withEntity(ContentTypes.APPLICATION_JSON, "{\"title\":123,\"summary\":null}"));
    assertThat(nonTextualUnderMaskPatch.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /v1/events accepts explicit CONFERENCE eventType and returns 201")
  void createEvent_withEventType_returns201() throws Exception {
    var cache = activeCache();
    var captured = new AtomicReference<EventCommand>();
    var actor = createEventActor(EventActorResponse.success(sampleEventResponse()), captured::set);
    var handler = createHandler(cache, actor);

    var body =
        "{\"generator\":\"019273c5-9271-7000-8000-000000000001\","
            + "\"title\":\"Annual Summit\",\"eventType\":\"CONFERENCE\"}";
    var response =
        executeRequest(
            handler,
            HttpRequest.POST("/api/studios/valiant/v1/events")
                .withEntity(ContentTypes.APPLICATION_JSON, body));

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
    var cmd = (EventCommand.CreateEvent) captured.get();
    assertThat(cmd.request().eventType()).contains(EventType.CONFERENCE);
  }

  @Test
  @DisplayName("Returns 404 when studio in cache is soft-deleted")
  void softDeletedStudio_returns404() throws Exception {
    var cache = mock(StudioLookupCache.class);
    var deleted = new StudioLookup(tenantId, studioId, "valiant", now, now, Optional.of(now));
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(deleted));

    var actor = createEventActor(EventActorResponse.success(sampleEventResponse()));
    var response =
        executeRequest(
            createHandler(cache, actor), HttpRequest.GET("/api/studios/valiant/v1/events"));

    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  private StudioLookupCache activeCache() {
    var cache = mock(StudioLookupCache.class);
    when(cache.findByIdOrAlias("valiant")).thenReturn(Optional.of(sampleLookup()));
    return cache;
  }

  private String readBody(HttpResponse response) throws Exception {
    return response
        .entity()
        .toStrict(1000, system)
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS)
        .getData()
        .utf8String();
  }
}
