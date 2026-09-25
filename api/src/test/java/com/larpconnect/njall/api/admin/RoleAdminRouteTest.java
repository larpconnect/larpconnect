package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.AdminRole;
import java.time.Duration;
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

final class RoleAdminRouteTest {

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID roleId = UUID.randomUUID();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "role-route-test");
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

  private AdminRole sampleRole() {
    return new AdminRole(roleId, "security_admin");
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
  @DisplayName("GET /api/admin/v1/roles returns 200 OK with list")
  void getRoles_returns200() throws Exception {
    var role = sampleRole();
    ActorRef<RoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case RoleAdminCommand.ListRoles cmd ->
                        cmd.replyTo().tell(RoleAdminResponse.list(ImmutableList.of(role)));
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "roleListActor" + UUID.randomUUID(),
            Props.empty());

    var route = new RoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/roles"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("POST /api/admin/v1/roles returns 201 Created on success")
  void postRoles_returns201() throws Exception {
    var role = sampleRole();
    ActorRef<RoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case RoleAdminCommand.CreateRole cmd ->
                        cmd.replyTo().tell(RoleAdminResponse.single(role));
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "roleCreateActor" + UUID.randomUUID(),
            Props.empty());

    var route = new RoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        handler
            .apply(
                HttpRequest.POST("/api/admin/v1/roles")
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"roleName\":\"security_admin\"}"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);

    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("GET /api/admin/v1/roles/{id} returns 200 when found and 404 when absent")
  void getRoleById_foundAndNotFound() throws Exception {
    var role = sampleRole();
    ActorRef<RoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case RoleAdminCommand.GetRoleByName cmd -> {
                      if ("security_admin".equals(cmd.roleName())) {
                        cmd.replyTo().tell(RoleAdminResponse.single(role));
                      } else {
                        cmd.replyTo().tell(RoleAdminResponse.notFound("Not found"));
                      }
                    }
                    case RoleAdminCommand.GetRoleById cmd -> {
                      if (roleId.equals(cmd.roleId())) {
                        cmd.replyTo().tell(RoleAdminResponse.single(role));
                      } else {
                        cmd.replyTo().tell(RoleAdminResponse.notFound("Not found"));
                      }
                    }
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "roleGetActor" + UUID.randomUUID(),
            Props.empty());

    var route = new RoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var responseName = executeGet(handler, "/api/admin/v1/roles/security_admin");
    assertThat(responseName.status()).isEqualTo(StatusCodes.OK);

    var responseUuid = executeGet(handler, "/api/admin/v1/roles/" + roleId);
    assertThat(responseUuid.status()).isEqualTo(StatusCodes.OK);

    var responseMissing = executeGet(handler, "/api/admin/v1/roles/unknown");
    assertThat(responseMissing.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("POST /api/admin/v1/roles maps BadRequest, Conflict, and Failure correctly")
  void postRoles_errorMappings() throws Exception {
    ActorRef<RoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  switch (msg) {
                    case RoleAdminCommand.CreateRole cmd -> {
                      if ("bad".equals(cmd.roleName())) {
                        cmd.replyTo().tell(RoleAdminResponse.badRequest("Bad format"));
                      } else if ("conflict".equals(cmd.roleName())) {
                        cmd.replyTo().tell(RoleAdminResponse.conflict("Already exists"));
                      } else if ("failure".equals(cmd.roleName())) {
                        cmd.replyTo().tell(RoleAdminResponse.failure("Boom"));
                      }
                    }
                    default -> {}
                  }
                  return Behaviors.same();
                }),
            "roleErrorsActor" + UUID.randomUUID(),
            Props.empty());

    var route = new RoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var badResp = executePost(handler, "/api/admin/v1/roles", "{\"roleName\":\"bad\"}");
    assertThat(badResp.status()).isEqualTo(StatusCodes.BAD_REQUEST);

    var conflictResp = executePost(handler, "/api/admin/v1/roles", "{\"roleName\":\"conflict\"}");
    assertThat(conflictResp.status()).isEqualTo(StatusCodes.CONFLICT);

    var failResp = executePost(handler, "/api/admin/v1/roles", "{\"roleName\":\"failure\"}");
    assertThat(failResp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/admin/v1/roles returns 500 on ask failure or timeout")
  void getRoles_timeoutReturns500() throws Exception {
    ActorRef<RoleAdminCommand> silentActor =
        system.systemActorOf(Behaviors.empty(), "silentRole" + UUID.randomUUID(), Props.empty());

    var route = new RoleAdminRoute(silentActor, system, objectMapper, Duration.ofMillis(300));
    var handler = route.route().seal().function(system);

    var resp =
        handler
            .apply(HttpRequest.GET("/api/admin/v1/roles"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    assertThat(resp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("POST /api/admin/v1/roles returns 500 on ask failure or timeout")
  void postRoles_timeoutReturns500() throws Exception {
    ActorRef<RoleAdminCommand> silentActor =
        system.systemActorOf(
            Behaviors.empty(), "silentRolePost" + UUID.randomUUID(), Props.empty());

    var route = new RoleAdminRoute(silentActor, system, objectMapper, Duration.ofMillis(300));
    var handler = route.route().seal().function(system);

    var resp =
        handler
            .apply(
                HttpRequest.POST("/api/admin/v1/roles")
                    .withEntity(ContentTypes.APPLICATION_JSON, "{\"roleName\":\"security_admin\"}"))
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
    assertThat(resp.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }
}
