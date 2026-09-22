package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.time.Duration;
import java.time.Instant;
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

final class StudioAdminRouteTest {

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "studio-route-test");
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

  private StudioLookup sampleStudio() {
    return StudioLookup.of(tenantId, studioId, "valhalla", now, now, null);
  }

  private static HttpResponse executeGet(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, String path) throws Exception {
    return handler.apply(HttpRequest.GET(path)).toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static HttpResponse executePost(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, String path, String json)
      throws Exception {
    return handler
        .apply(HttpRequest.POST(path).withEntity(ContentTypes.APPLICATION_JSON, json))
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("GET /api/admin/v1/studios returns 200 OK with list")
  void getStudios_returns200() throws Exception {
    var studio = sampleStudio();
    ActorRef<StudioAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case StudioAdminCommand.ListStudios cmd ->
                        cmd.replyTo().tell(StudioAdminResponse.list(ImmutableList.of(studio)));
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "studioListActor" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/studios?include_deleted=true"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("POST /api/admin/v1/studios returns 201 Created on success")
  void postStudios_returns201() throws Exception {
    var studio = sampleStudio();
    ActorRef<StudioAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case StudioAdminCommand.CreateStudio cmd ->
                        cmd.replyTo().tell(StudioAdminResponse.single(studio));
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "studioCreateActor" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(
                HttpRequest.POST("/api/admin/v1/studios")
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"alias\":\"valhalla\"}"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("GET /api/admin/v1/studios/{id} returns 200 when found and 404 when absent")
  void getStudioById_foundAndNotFound() throws Exception {
    var studio = sampleStudio();
    ActorRef<StudioAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case StudioAdminCommand.GetStudioByAlias cmd -> {
                      if ("valhalla".equals(cmd.alias())) {
                        cmd.replyTo().tell(StudioAdminResponse.single(studio));
                      } else {
                        cmd.replyTo().tell(StudioAdminResponse.notFound("Not found"));
                      }
                    }
                    case StudioAdminCommand.GetStudioById cmd -> {
                      if (studioId.equals(cmd.studioId())) {
                        cmd.replyTo().tell(StudioAdminResponse.single(studio));
                      } else {
                        cmd.replyTo().tell(StudioAdminResponse.notFound("Not found"));
                      }
                    }
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "studioGetActor" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var responseAlias = executeGet(handler, "/api/admin/v1/studios/valhalla");
    assertThat(responseAlias.status()).isEqualTo(StatusCodes.OK);

    var responseUuid = executeGet(handler, "/api/admin/v1/studios/" + studioId);
    assertThat(responseUuid.status()).isEqualTo(StatusCodes.OK);

    var responseMissing = executeGet(handler, "/api/admin/v1/studios/unknown");
    assertThat(responseMissing.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("POST /api/admin/v1/studios maps BadRequest, Conflict, and Failure correctly")
  void postStudios_errorMappings() throws Exception {
    ActorRef<StudioAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case StudioAdminCommand.CreateStudio cmd -> {
                      if ("bad".equals(cmd.alias())) {
                        cmd.replyTo().tell(StudioAdminResponse.badRequest("Bad format"));
                      } else if ("conflict".equals(cmd.alias())) {
                        cmd.replyTo().tell(StudioAdminResponse.conflict("Already exists"));
                      } else if ("failure".equals(cmd.alias())) {
                        cmd.replyTo().tell(StudioAdminResponse.failure("Boom"));
                      }
                    }
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "studioErrorsActor" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var badResp = executePost(handler, "/api/admin/v1/studios", "{\"alias\":\"bad\"}");
    assertThat(badResp.status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var conflictResp = executePost(handler, "/api/admin/v1/studios", "{\"alias\":\"conflict\"}");
    assertThat(conflictResp.status()).isEqualTo(StatusCodes.CONFLICT);

    var failResp = executePost(handler, "/api/admin/v1/studios", "{\"alias\":\"failure\"}");
    assertThat(failResp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/admin/v1/studios returns 500 on ask failure or timeout")
  void getStudios_timeoutReturns500() throws Exception {
    ActorRef<StudioAdminCommand> silentActor =
        system.systemActorOf(Behaviors.empty(), "silentStudio" + UUID.randomUUID(), Props.empty());

    var route = new StudioAdminRoute(silentActor, system, objectMapper, Duration.ofMillis(300));
    var handler = route.route().seal().function(system);

    var resp =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/studios"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    assertThat(resp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("POST /api/admin/v1/studios returns 500 on ask failure or timeout")
  void postStudios_timeoutReturns500() throws Exception {
    ActorRef<StudioAdminCommand> silentActor =
        system.systemActorOf(
            Behaviors.empty(), "silentStudioPost" + UUID.randomUUID(), Props.empty());

    var route = new StudioAdminRoute(silentActor, system, objectMapper, Duration.ofMillis(300));
    var handler = route.route().seal().function(system);

    var resp =
        handler
            .apply(
                HttpRequest.POST("/api/admin/v1/studios")
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"alias\":\"valhalla\"}"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    assertThat(resp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
