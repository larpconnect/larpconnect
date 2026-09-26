package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.domain.DefaultStudioRole;
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

final class StudioRoleAdminRouteTest {

  private static ActorSystem<Void> system;
  private static ObjectMapper objectMapper;

  private final UUID roleId = UUID.randomUUID();

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "studio-role-route-test");
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

  private DefaultStudioRole sampleRole() {
    return new DefaultStudioRole(roleId, "ORGANIZER");
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

  private static HttpResponse executePatch(
      Function<HttpRequest, CompletionStage<HttpResponse>> handler, String path, String json)
      throws Exception {
    return handler
        .apply(HttpRequest.PATCH(path).withEntity(ContentTypes.APPLICATION_JSON, json))
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("GET /api/admin/v1/studio-roles returns 200 OK with list")
  void getRoles_returns200() throws Exception {
    var role = sampleRole();
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.ListRoles cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.list(ImmutableList.of(role)));
                  }
                  return Behaviors.same();
                }),
            "studioRoleListActor" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/admin/v1/studio-roles");
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("POST /api/admin/v1/studio-roles returns 201 Created on success")
  void postRole_returns201() throws Exception {
    var role = sampleRole();
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.CreateRole cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.single(role));
                  }
                  return Behaviors.same();
                }),
            "studioRoleCreateActor" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executePost(handler, "/api/admin/v1/studio-roles", "{\"name\": \"ORGANIZER\"}");
    assertThat(response.status()).isEqualTo(StatusCodes.CREATED);
  }

  @Test
  @DisplayName("GET /api/admin/v1/studio-roles/{id} returns 200 OK when found")
  void getRoleById_found() throws Exception {
    var role = sampleRole();
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.GetRoleById cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.single(role));
                  }
                  return Behaviors.same();
                }),
            "studioRoleGetActor" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/admin/v1/studio-roles/" + roleId);
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("GET /api/admin/v1/studio-roles/{id} with invalid UUID returns 404")
  void getRoleById_invalidUuid_returns404() throws Exception {
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.empty(), "studioRoleDummy" + UUID.randomUUID(), Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/admin/v1/studio-roles/not-a-uuid");
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("PATCH /api/admin/v1/studio-roles/{id} returns 200 OK on update")
  void patchRole_returns200() throws Exception {
    var updatedRole = new DefaultStudioRole(roleId, "LEAD_ORGANIZER");
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.UpdateRole cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.single(updatedRole));
                  }
                  return Behaviors.same();
                }),
            "studioRolePatchActor" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executePatch(
            handler,
            "/api/admin/v1/studio-roles/" + roleId + "?update_mask=name",
            "{\"name\": \"LEAD_ORGANIZER\"}");
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName(
      "POST /api/admin/v1/studio-roles returns 400 Bad Request when actor returns BadRequest")
  void postRole_returns400() throws Exception {
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.CreateRole cmd) {
                    cmd.replyTo()
                        .tell(StudioRoleAdminResponse.badRequest("Role name cannot be blank"));
                  }
                  return Behaviors.same();
                }),
            "studioRoleCreate400" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executePost(handler, "/api/admin/v1/studio-roles", "{\"name\": \"   \"}");
    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }

  @Test
  @DisplayName("POST /api/admin/v1/studio-roles returns 409 Conflict when actor returns Conflict")
  void postRole_returns409() throws Exception {
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.CreateRole cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.conflict("Role already exists"));
                  }
                  return Behaviors.same();
                }),
            "studioRoleCreate409" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executePost(handler, "/api/admin/v1/studio-roles", "{\"name\": \"ORGANIZER\"}");
    assertThat(response.status()).isEqualTo(StatusCodes.CONFLICT);
  }

  @Test
  @DisplayName("POST /api/admin/v1/studio-roles returns 500 when actor returns Failure")
  void postRole_returns500() throws Exception {
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.CreateRole cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.failure("DB failure"));
                  }
                  return Behaviors.same();
                }),
            "studioRoleCreate500" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executePost(handler, "/api/admin/v1/studio-roles", "{\"name\": \"ORGANIZER\"}");
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("POST /api/admin/v1/studio-roles returns 500 when actor fails/times out")
  void postRole_actorFails_returns500() throws Exception {
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.empty(), "studioRoleCreateTimeout" + UUID.randomUUID(), Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper, Duration.ofMillis(50));
    var handler = route.route().seal().function(system);

    var response = executePost(handler, "/api/admin/v1/studio-roles", "{\"name\": \"ORGANIZER\"}");
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("GET /api/admin/v1/studio-roles/{id} returns 404 when actor returns NotFound")
  void getRoleById_notFound() throws Exception {
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.GetRoleById cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.notFound("Role not found"));
                  }
                  return Behaviors.same();
                }),
            "studioRoleGet404" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/admin/v1/studio-roles/" + roleId);
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("GET /api/admin/v1/studio-roles returns 500 when actor fails/times out")
  void getRoles_actorFails_returns500() throws Exception {
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.empty(), "studioRoleListTimeout" + UUID.randomUUID(), Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper, Duration.ofMillis(50));
    var handler = route.route().seal().function(system);

    var response = executeGet(handler, "/api/admin/v1/studio-roles");
    assertThat(response.status()).isEqualTo(StatusCodes.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("PATCH /api/admin/v1/studio-roles/{id} without update_mask updates successfully")
  void patchRole_withoutMask_returns200() throws Exception {
    var updatedRole = new DefaultStudioRole(roleId, "ADMIN");
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.UpdateRole cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.single(updatedRole));
                  }
                  return Behaviors.same();
                }),
            "studioRolePatchNoMask" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executePatch(handler, "/api/admin/v1/studio-roles/" + roleId, "{\"name\": \"ADMIN\"}");
    assertThat(response.status()).isEqualTo(StatusCodes.OK);
  }

  @Test
  @DisplayName("PATCH /api/admin/v1/studio-roles/{id} returns 404 when role not found")
  void patchRole_notFound_returns404() throws Exception {
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.UpdateRole cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.notFound("Role not found"));
                  }
                  return Behaviors.same();
                }),
            "studioRolePatch404" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executePatch(handler, "/api/admin/v1/studio-roles/" + roleId, "{\"name\": \"ADMIN\"}");
    assertThat(response.status()).isEqualTo(StatusCodes.NOT_FOUND);
  }

  @Test
  @DisplayName("PATCH /api/admin/v1/studio-roles/{id} returns 409 when conflict")
  void patchRole_conflict_returns409() throws Exception {
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.UpdateRole cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.conflict("Name taken"));
                  }
                  return Behaviors.same();
                }),
            "studioRolePatch409" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executePatch(handler, "/api/admin/v1/studio-roles/" + roleId, "{\"name\": \"ADMIN\"}");
    assertThat(response.status()).isEqualTo(StatusCodes.CONFLICT);
  }

  @Test
  @DisplayName("PATCH /api/admin/v1/studio-roles/{id} returns 400 when bad request")
  void patchRole_badRequest_returns400() throws Exception {
    ActorRef<StudioRoleAdminCommand> actor =
        system.systemActorOf(
            Behaviors.receiveMessage(
                msg -> {
                  if (msg instanceof StudioRoleAdminCommand.UpdateRole cmd) {
                    cmd.replyTo().tell(StudioRoleAdminResponse.badRequest("Name blank"));
                  }
                  return Behaviors.same();
                }),
            "studioRolePatch400" + UUID.randomUUID(),
            Props.empty());

    var route = new StudioRoleAdminRoute(actor, system, objectMapper);
    var handler = route.route().seal().function(system);

    var response =
        executePatch(handler, "/api/admin/v1/studio-roles/" + roleId, "{\"name\": \"   \"}");
    assertThat(response.status()).isEqualTo(StatusCodes.BAD_REQUEST);
  }
}
