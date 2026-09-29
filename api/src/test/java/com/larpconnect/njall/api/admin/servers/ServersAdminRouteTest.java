package com.larpconnect.njall.api.admin.servers;

import static org.assertj.core.api.Assertions.assertThat;

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

final class ServersAdminRouteTest {

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "servers-admin-route-test");
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

  private static Server sampleServer() {
    var contact =
        new ServerContact(
            UUID.randomUUID(), RoleType.ADMIN, ContactType.EMAIL, "admin@larpconnect.com", 0);
    return new Server(
        UUID.randomUUID(),
        "Test Server",
        "larpconnect.com",
        Instant.parse("2026-01-01T00:00:00Z"),
        ImmutableList.of(contact));
  }

  @Test
  @DisplayName("GET /api/admin/v1/servers returns 200 OK with camelCase JSON array")
  void route_listServersSuccess_returns200OkWithJson() throws Exception {
    var server = sampleServer();
    ActorRef<ServerAdminCommand> serverActor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case ServerAdminCommand.ListServers cmd ->
                        cmd.replyTo().tell(ServerAdminResponse.success(ImmutableList.of(server)));
                  }
                  return Behaviors.same();
                }),
            "serverSuccessActor" + UUID.randomUUID(),
            Props.empty());

    var route = ServersAdminRoute.create(serverActor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeRequest(handler, HttpRequest.GET("/api/admin/v1/servers"));
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
    ActorRef<ServerAdminCommand> serverActor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case ServerAdminCommand.ListServers cmd ->
                        cmd.replyTo().tell(ServerAdminResponse.failure("Query error"));
                  }
                  return Behaviors.same();
                }),
            "serverFailureActor" + UUID.randomUUID(),
            Props.empty());

    var route = ServersAdminRoute.create(serverActor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeRequest(handler, HttpRequest.GET("/api/admin/v1/servers"));
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("POST /api/admin/v1/servers is rejected with 405 Method Not Allowed")
  void route_postServers_returnsMethodNotAllowed() throws Exception {
    ActorRef<ServerAdminCommand> serverActor =
        system.systemActorOf(
            Behaviors.receiveMessage(msg -> Behaviors.same()),
            "dummyServerActor" + UUID.randomUUID(),
            Props.empty());

    var route = ServersAdminRoute.create(serverActor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeRequest(handler, HttpRequest.POST("/api/admin/v1/servers"));
    assertThat(response.status()).isEqualTo(StatusCodes.METHOD_NOT_ALLOWED);
  }
}
