package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.AdminUser;
import com.larpconnect.njall.data.domain.AdminUserStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

final class UserAdminRouteTest {

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID userId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "user-route-test");
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

  private AdminUser sampleUser() {
    return new AdminUser(userId, "admin_user", AdminUserStatus.ACTIVE, now, now, List.of());
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
  @DisplayName("GET /api/admin/v1/users returns 200 OK with list")
  void getUsers_returns200() throws Exception {
    var user = sampleUser();
    ActorRef<UserAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case UserAdminCommand.ListUsers cmd ->
                        cmd.replyTo().tell(UserAdminResponse.list(ImmutableList.of(user)));
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "userListActor" + UUID.randomUUID(),
            Props.empty());

    var route = new UserAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/users"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("POST /api/admin/v1/users returns 201 Created on success")
  void postUsers_returns201() throws Exception {
    var user = sampleUser();
    ActorRef<UserAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case UserAdminCommand.CreateUser cmd ->
                        cmd.replyTo().tell(UserAdminResponse.single(user));
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "userCreateActor" + UUID.randomUUID(),
            Props.empty());

    var route = new UserAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(
                HttpRequest.POST("/api/admin/v1/users")
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"username\":\"admin_user\"}"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("GET /api/admin/v1/users/{id} returns 200 when found and 404 when absent")
  void getUserById_foundAndNotFound() throws Exception {
    var user = sampleUser();
    ActorRef<UserAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case UserAdminCommand.GetUserByUsername cmd -> {
                      if ("admin_user".equals(cmd.username())) {
                        cmd.replyTo().tell(UserAdminResponse.single(user));
                      } else {
                        cmd.replyTo().tell(UserAdminResponse.notFound("Not found"));
                      }
                    }
                    case UserAdminCommand.GetUserById cmd -> {
                      if (userId.equals(cmd.userId())) {
                        cmd.replyTo().tell(UserAdminResponse.single(user));
                      } else {
                        cmd.replyTo().tell(UserAdminResponse.notFound("Not found"));
                      }
                    }
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "userGetActor" + UUID.randomUUID(),
            Props.empty());

    var route = new UserAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var responseName = executeGet(handler, "/api/admin/v1/users/admin_user");
    assertThat(responseName.status()).isEqualTo(StatusCodes.OK);

    var responseUuid = executeGet(handler, "/api/admin/v1/users/" + userId);
    assertThat(responseUuid.status()).isEqualTo(StatusCodes.OK);

    var responseMissing = executeGet(handler, "/api/admin/v1/users/unknown");
    assertThat(responseMissing.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("POST /api/admin/v1/users/{id}:addRole assigns role and returns 200")
  void postAddRole_returns200() throws Exception {
    var user = sampleUser();
    ActorRef<UserAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case UserAdminCommand.AddRole cmd ->
                        cmd.replyTo().tell(UserAdminResponse.single(user));
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "userAddRoleActor" + UUID.randomUUID(),
            Props.empty());

    var route = new UserAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(
                HttpRequest.POST("/api/admin/v1/users/admin_user:addRole")
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"roleName\":\"security_admin\"}"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);

    var responseSlash =
        handler
            .apply(
                HttpRequest.POST("/api/admin/v1/users/admin_user/:addRole")
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"roleName\":\"security_admin\"}"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(responseSlash.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("POST /api/admin/v1/users/{id}:removeRole removes role and returns 200")
  void postRemoveRole_returns200() throws Exception {
    var user = sampleUser();
    ActorRef<UserAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case UserAdminCommand.RemoveRole cmd ->
                        cmd.replyTo().tell(UserAdminResponse.single(user));
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "userRemoveRoleActor" + UUID.randomUUID(),
            Props.empty());

    var route = new UserAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(
                HttpRequest.POST("/api/admin/v1/users/admin_user:removeRole")
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"roleName\":\"security_admin\"}"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);

    var responseSlash =
        handler
            .apply(
                HttpRequest.POST("/api/admin/v1/users/admin_user/:removeRole")
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"roleName\":\"security_admin\"}"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(responseSlash.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("POST /api/admin/v1/users maps BadRequest, Conflict, and Failure correctly")
  void postUsers_errorMappings() throws Exception {
    ActorRef<UserAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case UserAdminCommand.CreateUser cmd -> {
                      if ("bad".equals(cmd.username())) {
                        cmd.replyTo().tell(UserAdminResponse.badRequest("Bad format"));
                      } else if ("conflict".equals(cmd.username())) {
                        cmd.replyTo().tell(UserAdminResponse.conflict("Already exists"));
                      } else if ("failure".equals(cmd.username())) {
                        cmd.replyTo().tell(UserAdminResponse.failure("Boom"));
                      }
                    }
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "userErrorsActor" + UUID.randomUUID(),
            Props.empty());

    var route = new UserAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var badResp = executePost(handler, "/api/admin/v1/users", "{\"username\":\"bad\"}");
    assertThat(badResp.status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var conflictResp = executePost(handler, "/api/admin/v1/users", "{\"username\":\"conflict\"}");
    assertThat(conflictResp.status()).isEqualTo(StatusCodes.CONFLICT);

    var failResp = executePost(handler, "/api/admin/v1/users", "{\"username\":\"failure\"}");
    assertThat(failResp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/admin/v1/users returns 500 on ask failure or timeout")
  void getUsers_timeoutReturns500() throws Exception {
    ActorRef<UserAdminCommand> silentActor =
        system.systemActorOf(Behaviors.empty(), "silentUser" + UUID.randomUUID(), Props.empty());

    var route = new UserAdminRoute(silentActor, system, objectMapper, Duration.ofMillis(300));
    var handler = route.route().seal().function(system);

    var resp =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/users"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    assertThat(resp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("POST /api/admin/v1/users returns 500 on ask failure or timeout")
  void postUsers_timeoutReturns500() throws Exception {
    ActorRef<UserAdminCommand> silentActor =
        system.systemActorOf(
            Behaviors.empty(), "silentUserPost" + UUID.randomUUID(), Props.empty());

    var route = new UserAdminRoute(silentActor, system, objectMapper, Duration.ofMillis(300));
    var handler = route.route().seal().function(system);

    var resp =
        handler
            .apply(
                HttpRequest.POST("/api/admin/v1/users")
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"username\":\"admin_user\"}"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    assertThat(resp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
