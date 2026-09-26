package com.larpconnect.njall.api.studios;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Duration;
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
import org.apache.pekko.japi.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class StudiosRouteTest {

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID studioId = UUID.randomUUID();

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

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 200 OK when found")
  void getStudio_success() throws Exception {
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

    var route = new StudiosRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/studios/valiant/v1/studio");
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 404 when actor replies with NotFound")
  void getStudio_actorRepliesNotFound() throws Exception {
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

    var route = new StudiosRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/studios/valiant/v1/studio");
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 500 when actor replies with Failure")
  void getStudio_actorRepliesFailure() throws Exception {
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

    var route = new StudiosRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/studios/valiant/v1/studio");
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/studios/{alias}/v1/studio returns 500 when actor times out")
  void getStudio_actorTimesOut() throws Exception {
    ActorRef<StudioCommand> actor =
        system.systemActorOf(
            Behaviors.empty(), "studioActorTimeout" + UUID.randomUUID(), Props.empty());

    var route = new StudiosRoute(actor, system, objectMapper, Duration.ofMillis(50));
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/studios/valiant/v1/studio");
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
