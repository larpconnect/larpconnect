package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.ContactType;
import com.larpconnect.njall.data.domain.RoleType;
import com.larpconnect.njall.data.domain.Server;
import com.larpconnect.njall.data.domain.ServerContact;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AdminRouteTest {

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "admin-route-test");
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

  private static ActorRef<HealthCheckCommand> dummyHealthActor() {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              if (msg instanceof HealthCheckCommand.CheckHealth cmd) {
                cmd.replyTo().tell(HealthCheckResponse.healthy());
              }
              return Behaviors.same();
            }),
        "dummyHealthActor" + UUID.randomUUID(),
        Props.empty());
  }

  private static ActorRef<ServerAdminCommand> dummyServerActor() {
    return system.systemActorOf(
        Behaviors.receiveMessage(
            msg -> {
              if (msg instanceof ServerAdminCommand.ListServers cmd) {
                cmd.replyTo().tell(ServerAdminResponse.success(ImmutableList.of()));
              }
              return Behaviors.same();
            }),
        "dummyServerActor" + UUID.randomUUID(),
        Props.empty());
  }

  private static Server sampleServer() {
    var contact =
        ServerContact.of(
            UUID.randomUUID(), RoleType.ADMIN, ContactType.EMAIL, "admin@larpconnect.com", 0);
    return Server.of(
        UUID.randomUUID(),
        "Test Server",
        "larpconnect.com",
        Instant.parse("2026-01-01T00:00:00Z"),
        ImmutableList.of(contact));
  }

  @Test
  @DisplayName("GET /api/admin/v1/health returns 200 OK with empty body when healthy")
  void route_healthyResponse_returns200Ok() throws Exception {
    var healthActor =
        system.systemActorOf(
            Behaviors.<HealthCheckCommand>receiveMessage(
                msg -> {
                  if (msg instanceof HealthCheckCommand.CheckHealth cmd) {
                    cmd.replyTo().tell(HealthCheckResponse.healthy());
                  }
                  return Behaviors.same();
                }),
            "healthyActor" + UUID.randomUUID(),
            Props.empty());

    var route = new DefaultAdminRoute(healthActor, dummyServerActor(), system, objectMapper);
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
    var healthActor =
        system.systemActorOf(
            Behaviors.<HealthCheckCommand>receiveMessage(
                msg -> {
                  if (msg instanceof HealthCheckCommand.CheckHealth cmd) {
                    cmd.replyTo().tell(new HealthCheckResponse.Unhealthy("component degraded"));
                  }
                  return Behaviors.same();
                }),
            "unhealthyActor" + UUID.randomUUID(),
            Props.empty());

    var route = new DefaultAdminRoute(healthActor, dummyServerActor(), system, objectMapper);
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
  void route_healthTimeout_returns500Error() throws Exception {
    var silentHealthActor =
        system.systemActorOf(
            Behaviors.<HealthCheckCommand>receiveMessage(msg -> Behaviors.same()),
            "silentHealthActor" + UUID.randomUUID(),
            Props.empty());

    var route = new DefaultAdminRoute(silentHealthActor, dummyServerActor(), system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/health"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/admin/v1/servers returns 200 OK with camelCase JSON array")
  void route_listServersSuccess_returns200OkWithJson() throws Exception {
    var server = sampleServer();
    var serverActor =
        system.systemActorOf(
            Behaviors.<ServerAdminCommand>receiveMessage(
                msg -> {
                  if (msg instanceof ServerAdminCommand.ListServers cmd) {
                    cmd.replyTo().tell(ServerAdminResponse.success(ImmutableList.of(server)));
                  }
                  return Behaviors.same();
                }),
            "serverSuccessActor" + UUID.randomUUID(),
            Props.empty());

    var route = new DefaultAdminRoute(dummyHealthActor(), serverActor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/servers"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    var body =
        response
            .entity()
            .toStrict(Duration.ofSeconds(1).toMillis(), system)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
    assertThat(response.entity().getContentType()).isEqualTo(ContentTypes.APPLICATION_JSON);
    var json = body.getData().utf8String();
    assertThat(json).contains("primaryDomain");
    assertThat(json).contains("roleType");
    assertThat(json).contains("contactType");
    assertThat(json).contains("larpconnect.com");
  }

  @Test
  @DisplayName("GET /api/admin/v1/servers returns 500 on actor failure")
  void route_listServersFailure_returns500Error() throws Exception {
    var serverActor =
        system.systemActorOf(
            Behaviors.<ServerAdminCommand>receiveMessage(
                msg -> {
                  if (msg instanceof ServerAdminCommand.ListServers cmd) {
                    cmd.replyTo().tell(ServerAdminResponse.failure("Query error"));
                  }
                  return Behaviors.same();
                }),
            "serverFailureActor" + UUID.randomUUID(),
            Props.empty());

    var route = new DefaultAdminRoute(dummyHealthActor(), serverActor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/servers"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/admin/v1/servers returns 500 on ask timeout")
  void route_listServersTimeout_returns500Error() throws Exception {
    var silentServerActor =
        system.systemActorOf(
            Behaviors.<ServerAdminCommand>receiveMessage(msg -> Behaviors.same()),
            "silentServerActor" + UUID.randomUUID(),
            Props.empty());

    var route = new DefaultAdminRoute(dummyHealthActor(), silentServerActor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/servers"))
            .toCompletableFuture()
            .get(7, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("POST /api/admin/v1/servers is rejected with 405 Method Not Allowed")
  void route_postServers_returnsMethodNotAllowed() throws Exception {
    var route = new DefaultAdminRoute(dummyHealthActor(), dummyServerActor(), system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.POST("/api/admin/v1/servers"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.METHOD_NOT_ALLOWED);
  }

  @Test
  @DisplayName("DefaultAdminRoute constructor throws NullPointerException for null dependencies")
  void constructor_nullArguments_throwsNullPointerException() {
    var health = dummyHealthActor();
    var server = dummyServerActor();

    assertThatNullPointerException()
        .isThrownBy(() -> new DefaultAdminRoute(null, server, system, objectMapper))
        .withMessage("healthCheckActor must not be null");

    assertThatNullPointerException()
        .isThrownBy(() -> new DefaultAdminRoute(health, null, system, objectMapper))
        .withMessage("serverAdminActor must not be null");

    assertThatNullPointerException()
        .isThrownBy(() -> new DefaultAdminRoute(health, server, null, objectMapper))
        .withMessage("system must not be null");

    assertThatNullPointerException()
        .isThrownBy(() -> new DefaultAdminRoute(health, server, system, null))
        .withMessage("objectMapper must not be null");
  }
}
