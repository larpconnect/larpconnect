package com.larpconnect.njall.api.admin.studioroles;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.api.admin.common.AdminErrorResponse;
import com.larpconnect.njall.api.admin.common.AdminValidation;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

/** HTTP route handling default studio role administrative operations. */
public final class StudioRoleAdminRoute extends AllDirectives {

  private final Logger logger = LoggerFactory.getLogger(StudioRoleAdminRoute.class);

  private final ActorRef<StudioRoleAdminCommand> studioRoleAdminActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  StudioRoleAdminRoute(
      ActorRef<StudioRoleAdminCommand> studioRoleAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioRoleAdminActor, system, objectMapper, Duration.ofSeconds(20));
  }

  StudioRoleAdminRoute(
      ActorRef<StudioRoleAdminCommand> studioRoleAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioRoleAdminActor = studioRoleAdminActor;
    this.system = system;
    this.objectMapper = objectMapper;
    this.askTimeout = askTimeout;
  }

  /**
   * Pure factory method constructing a {@link StudioRoleAdminRoute} instance.
   *
   * @param studioRoleAdminActor The studio role admin actor reference.
   * @param system The ActorSystem.
   * @param objectMapper The ObjectMapper for JSON marshalling.
   * @return A configured route instance.
   */
  public static StudioRoleAdminRoute create(
      ActorRef<StudioRoleAdminCommand> studioRoleAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    return new StudioRoleAdminRoute(studioRoleAdminActor, system, objectMapper);
  }

  /**
   * Pure factory method constructing a {@link StudioRoleAdminRoute} instance with a custom ask
   * timeout.
   *
   * @param studioRoleAdminActor The studio role admin actor reference.
   * @param system The ActorSystem.
   * @param objectMapper The ObjectMapper for JSON marshalling.
   * @param askTimeout The custom ask timeout duration.
   * @return A configured route instance.
   */
  public static StudioRoleAdminRoute create(
      ActorRef<StudioRoleAdminCommand> studioRoleAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    return new StudioRoleAdminRoute(studioRoleAdminActor, system, objectMapper, askTimeout);
  }

  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/admin/v1/studio-roles"),
        () ->
            concat(
                pathEndOrSingleSlash(
                    () ->
                        concat(
                            get(this::handleListRoles),
                            post(
                                () ->
                                    entity(
                                        Jackson.unmarshaller(
                                            objectMapper, CreateStudioRoleRequest.class),
                                        this::handleCreateRole)))),
                path(
                    PathMatchers.segment(),
                    idStr -> {
                      var maybeUuid = AdminValidation.tryParseUuid(idStr);
                      if (maybeUuid.isEmpty()) {
                        return complete(
                            StatusCodes.NOT_FOUND,
                            new AdminErrorResponse(404, "Invalid role UUID: " + idStr),
                            Jackson.marshaller(objectMapper));
                      }
                      var roleId = maybeUuid.orElseThrow();
                      return concat(
                          get(() -> handleGetRole(roleId)),
                          patch(
                              () ->
                                  parameterOptional(
                                      "update_mask",
                                      mask ->
                                          entity(
                                              Jackson.unmarshaller(
                                                  objectMapper, UpdateStudioRoleRequest.class),
                                              req -> handleUpdateRole(roleId, mask, req)))));
                    })));
  }

  private Route handleCreateRole(CreateStudioRoleRequest request) {
    return onComplete(() -> askCreateRole(request.name()), this::mapCreateResponse);
  }

  private CompletionStage<StudioRoleAdminResponse> askCreateRole(String name) {
    return ask(
        studioRoleAdminActor,
        replyTo -> new StudioRoleAdminCommand.CreateRole(name, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route handleListRoles() {
    return onComplete(this::askListRoles, this::mapOkResponse);
  }

  private CompletionStage<StudioRoleAdminResponse> askListRoles() {
    return ask(
        studioRoleAdminActor,
        StudioRoleAdminCommand.ListRoles::new,
        askTimeout,
        system.scheduler());
  }

  private Route handleGetRole(UUID roleId) {
    return onComplete(() -> askGetRole(roleId), this::mapOkResponse);
  }

  private CompletionStage<StudioRoleAdminResponse> askGetRole(UUID roleId) {
    return ask(
        studioRoleAdminActor,
        replyTo -> new StudioRoleAdminCommand.GetRoleById(roleId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route handleUpdateRole(
      UUID roleId, Optional<String> updateMask, UpdateStudioRoleRequest request) {
    return onComplete(() -> askUpdateRole(roleId, request.name(), updateMask), this::mapOkResponse);
  }

  private CompletionStage<StudioRoleAdminResponse> askUpdateRole(
      UUID roleId, String name, Optional<String> updateMask) {
    return ask(
        studioRoleAdminActor,
        replyTo -> new StudioRoleAdminCommand.UpdateRole(roleId, name, updateMask, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapCreateResponse(Try<StudioRoleAdminResponse> responseTry) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return mapSuccessResponse(responseTry.get(), StatusCodes.CREATED);
  }

  private Route mapOkResponse(Try<StudioRoleAdminResponse> responseTry) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return mapSuccessResponse(responseTry.get(), StatusCodes.OK);
  }

  private Route handleActorFailure(Throwable error) {
    logger.error("Studio role admin actor request failed", error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        new AdminErrorResponse(500, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private Route mapSuccessResponse(StudioRoleAdminResponse response, StatusCode successStatus) {
    return switch (response) {
      case StudioRoleAdminResponse.RoleSingle single ->
          complete(successStatus, single.role(), Jackson.marshaller(objectMapper));
      case StudioRoleAdminResponse.RoleList list ->
          completeOK(list.roles(), Jackson.marshaller(objectMapper));
      case StudioRoleAdminResponse.NotFound nf ->
          complete(
              StatusCodes.NOT_FOUND,
              new AdminErrorResponse(404, nf.message()),
              Jackson.marshaller(objectMapper));
      case StudioRoleAdminResponse.Conflict c ->
          complete(
              StatusCodes.CONFLICT,
              new AdminErrorResponse(409, c.message()),
              Jackson.marshaller(objectMapper));
      case StudioRoleAdminResponse.BadRequest br ->
          complete(
              StatusCodes.BAD_REQUEST,
              new AdminErrorResponse(400, br.message()),
              Jackson.marshaller(objectMapper));
      case StudioRoleAdminResponse.Failure f ->
          complete(
              StatusCodes.INTERNAL_SERVER_ERROR,
              new AdminErrorResponse(500, f.message()),
              Jackson.marshaller(objectMapper));
    };
  }
}
