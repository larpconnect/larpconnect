package com.larpconnect.njall.api.admin.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.common.telemetry.ApiCall;
import com.larpconnect.njall.common.telemetry.TraceContext;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.model.headers.RawHeader;
import org.apache.pekko.japi.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HealthAdminRouteTest {

  private static ActorSystem<Void> system;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "health-admin-route-test");
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  private static HttpResponse executeGet(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, HttpRequest request)
      throws Exception {
    return handler.apply(request).toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("GET /api/admin/v1/health returns 200 OK when healthy")
  void route_healthy_returns200() throws Exception {
    ActorRef<ApiCall<HealthCheckCommand>> healthActor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg.call()) {
                    case HealthCheckCommand.CheckHealth cmd ->
                        cmd.replyTo().tell(HealthCheckResponse.healthy());
                  }
                  return Behaviors.same();
                }),
            "healthyActor" + UUID.randomUUID(),
            Props.empty());

    var healthRoute = new HealthAdminRoute(healthActor, system);
    var handler = healthRoute.route().seal().function(system);

    var response = executeGet(handler, HttpRequest.GET("/api/admin/v1/health"));
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/admin/v1/health returns 500 Internal Server Error when unhealthy")
  void route_unhealthy_returns500() throws Exception {
    ActorRef<ApiCall<HealthCheckCommand>> healthActor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg.call()) {
                    case HealthCheckCommand.CheckHealth cmd ->
                        cmd.replyTo().tell(HealthCheckResponse.unhealthy("Disk full"));
                  }
                  return Behaviors.same();
                }),
            "unhealthyActor" + UUID.randomUUID(),
            Props.empty());

    var healthRoute = new HealthAdminRoute(healthActor, system);
    var handler = healthRoute.route().seal().function(system);

    var response = executeGet(handler, HttpRequest.GET("/api/admin/v1/health"));
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/admin/v1/health parses traceparent header")
  void route_withTraceparent_passesTraceContext() throws Exception {
    var receivedContext = new AtomicReference<TraceContext>();
    ActorRef<ApiCall<HealthCheckCommand>> healthActor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  msg.context().ifPresent(receivedContext::set);
                  switch (msg.call()) {
                    case HealthCheckCommand.CheckHealth cmd ->
                        cmd.replyTo().tell(HealthCheckResponse.healthy());
                  }
                  return Behaviors.same();
                }),
            "tracingHealthActor" + UUID.randomUUID(),
            Props.empty());

    var healthRoute = new HealthAdminRoute(healthActor, system);
    var handler = healthRoute.route().seal().function(system);

    var response =
        executeGet(
            handler,
            HttpRequest.GET("/api/admin/v1/health")
                .addHeader(
                    RawHeader.create(
                        "traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01")));

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    assertThat(receivedContext.get()).isNotNull();
    assertThat(receivedContext.get().traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    assertThat(receivedContext.get().spanId()).isEqualTo("00f067aa0ba902b7");
  }
}
