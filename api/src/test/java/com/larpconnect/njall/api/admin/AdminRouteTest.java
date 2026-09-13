package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AdminRouteTest {

  private static ActorSystem<Void> system;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "admin-route-test");
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("GET /api/admin/v1/health returns 200 OK with empty body when healthy")
  void route_healthyResponse_returns200Ok() throws Exception {
    ActorRef<HealthCheckCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof HealthCheckCommand.CheckHealth cmd) {
                    cmd.replyTo().tell(HealthCheckResponse.healthy());
                  }
                  return Behaviors.same();
                }),
            "healthyActor",
            Props.empty());

    var route = new DefaultAdminRoute(actor, system);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/health"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    var body =
        response
            .entity()
            .toStrict(Duration.ofSeconds(1).toMillis(), system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    assertThat(body.getData().utf8String()).isEmpty();
  }

  @Test
  @DisplayName("GET /api/admin/v1/health returns 500 when unhealthy")
  void route_unhealthyResponse_returns500Error() throws Exception {
    ActorRef<HealthCheckCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof HealthCheckCommand.CheckHealth cmd) {
                    cmd.replyTo().tell(new HealthCheckResponse.Unhealthy("component degraded"));
                  }
                  return Behaviors.same();
                }),
            "unhealthyActor",
            Props.empty());

    var route = new DefaultAdminRoute(actor, system);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/health"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    var body =
        response
            .entity()
            .toStrict(Duration.ofSeconds(1).toMillis(), system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
    assertThat(body.getData().utf8String()).isEmpty();
  }

  @Test
  @DisplayName("GET /api/admin/v1/health returns 500 on ask timeout")
  void route_timeout_returns500Error() throws Exception {
    ActorRef<HealthCheckCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(msg -> Behaviors.same()), "silentActor", Props.empty());

    var route = new DefaultAdminRoute(actor, system);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/health"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    var body =
        response
            .entity()
            .toStrict(Duration.ofSeconds(1).toMillis(), system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
    assertThat(body.getData().utf8String()).isEmpty();
  }

  @Test
  @DisplayName("POST /api/admin/v1/health is rejected with 405 Method Not Allowed")
  void route_postMethod_returnsMethodNotAllowed() throws Exception {
    ActorRef<HealthCheckCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof HealthCheckCommand.CheckHealth cmd) {
                    cmd.replyTo().tell(HealthCheckResponse.healthy());
                  }
                  return Behaviors.same();
                }),
            "dummyActor",
            Props.empty());

    var route = new DefaultAdminRoute(actor, system);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.POST("/api/admin/v1/health"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.METHOD_NOT_ALLOWED);
  }

  @Test
  @DisplayName("DefaultAdminRoute constructor throws NullPointerException for null dependencies")
  void constructor_nullArguments_throwsNullPointerException() {
    assertThatNullPointerException()
        .isThrownBy(() -> new DefaultAdminRoute(null, system))
        .withMessage("healthCheckActor must not be null");

    ActorRef<HealthCheckCommand> actor =
        system.systemActorOf(Behaviors.empty(), "nullCheckActor", Props.empty());
    assertThatNullPointerException()
        .isThrownBy(() -> new DefaultAdminRoute(actor, null))
        .withMessage("system must not be null");
  }
}
