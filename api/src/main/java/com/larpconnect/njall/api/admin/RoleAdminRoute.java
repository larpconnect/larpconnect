package com.larpconnect.njall.api.admin;

import static java.util.Objects.requireNonNull;
import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.time.Duration;
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

/** HTTP route handling role management operations. */
public final class RoleAdminRoute extends AllDirectives {

  private final Logger logger = LoggerFactory.getLogger(RoleAdminRoute.class);

  private final ActorRef<RoleAdminCommand> roleAdminActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  RoleAdminRoute(
      ActorRef<RoleAdminCommand> roleAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(roleAdminActor, system, objectMapper, Duration.ofSeconds(20));
  }

  /**
   * Package-private constructor allowing custom ask timeouts during unit testing.
   *
   * @param roleAdminActor actor handling role administration commands
   * @param system typed actor system
   * @param objectMapper Jackson object mapper
   * @param askTimeout timeout duration for ask operations
   */
  RoleAdminRoute(
      ActorRef<RoleAdminCommand> roleAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.roleAdminActor = requireNonNull(roleAdminActor, "roleAdminActor cannot be null");
    this.system = requireNonNull(system, "system cannot be null");
    this.objectMapper = requireNonNull(objectMapper, "objectMapper cannot be null");
    this.askTimeout = requireNonNull(askTimeout, "askTimeout cannot be null");
  }

  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/admin/v1/roles"),
        () ->
            concat(
                pathEndOrSingleSlash(
                    () ->
                        concat(
                            get(this::handleListRoles),
                            post(
                                () ->
                                    entity(
                                        Jackson.unmarshaller(objectMapper, CreateRoleRequest.class),
                                        this::handleCreateRole)))),
                path(PathMatchers.segment(), id -> get(() -> handleGetRole(id)))));
  }

  private Route handleCreateRole(CreateRoleRequest request) {
    return onComplete(() -> askCreateRole(request.roleName()), this::mapCreateResponse);
  }

  private CompletionStage<RoleAdminResponse> askCreateRole(String roleName) {
    return ask(
        roleAdminActor,
        replyTo -> new RoleAdminCommand.CreateRole(roleName, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route handleListRoles() {
    return onComplete(this::askListRoles, this::mapOkResponse);
  }

  private CompletionStage<RoleAdminResponse> askListRoles() {
    return ask(roleAdminActor, RoleAdminCommand.ListRoles::new, askTimeout, system.scheduler());
  }

  private Route handleGetRole(String identifier) {
    return onComplete(() -> askGetRole(identifier), this::mapOkResponse);
  }

  private CompletionStage<RoleAdminResponse> askGetRole(String identifier) {
    var maybeUuid = AdminValidation.tryParseUuid(identifier);
    return ask(
        roleAdminActor,
        replyTo ->
            maybeUuid
                .map(uuid -> (RoleAdminCommand) new RoleAdminCommand.GetRoleById(uuid, replyTo))
                .orElseGet(() -> new RoleAdminCommand.GetRoleByName(identifier, replyTo)),
        askTimeout,
        system.scheduler());
  }

  private Route mapCreateResponse(Try<RoleAdminResponse> responseTry) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return mapSuccessResponse(responseTry.get(), StatusCodes.CREATED);
  }

  private Route mapOkResponse(Try<RoleAdminResponse> responseTry) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return mapSuccessResponse(responseTry.get(), StatusCodes.OK);
  }

  private Route handleActorFailure(Throwable error) {
    logger.error("Role admin actor request failed", error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        AdminErrorResponse.of(500, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private Route mapSuccessResponse(RoleAdminResponse response, StatusCode successStatus) {
    return switch (response) {
      case RoleAdminResponse.RoleSingle single ->
          complete(successStatus, single.role(), Jackson.marshaller(objectMapper));
      case RoleAdminResponse.RoleList list ->
          completeOK(list.roles(), Jackson.marshaller(objectMapper));
      default -> mapErrorResponse(response);
    };
  }

  private Route mapErrorResponse(RoleAdminResponse response) {
    return switch (response) {
      case RoleAdminResponse.NotFound nf ->
          complete(
              StatusCodes.NOT_FOUND,
              AdminErrorResponse.of(404, nf.message()),
              Jackson.marshaller(objectMapper));
      case RoleAdminResponse.Conflict c ->
          complete(
              StatusCodes.CONFLICT,
              AdminErrorResponse.of(409, c.message()),
              Jackson.marshaller(objectMapper));
      case RoleAdminResponse.BadRequest br ->
          complete(
              StatusCodes.BAD_REQUEST,
              AdminErrorResponse.of(400, br.message()),
              Jackson.marshaller(objectMapper));
      case RoleAdminResponse.Failure f ->
          complete(
              StatusCodes.INTERNAL_SERVER_ERROR,
              AdminErrorResponse.of(500, f.message()),
              Jackson.marshaller(objectMapper));
      default ->
          complete(
              StatusCodes.INTERNAL_SERVER_ERROR,
              AdminErrorResponse.of(500, "Unknown error"),
              Jackson.marshaller(objectMapper));
    };
  }
}
