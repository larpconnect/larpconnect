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
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

/** HTTP route handling user management and role assignment operations. */
public final class UserAdminRoute extends AllDirectives {

  private final Logger logger = LoggerFactory.getLogger(UserAdminRoute.class);
  private static final String ADD_ROLE_SUFFIX = ":addRole";
  private static final String REMOVE_ROLE_SUFFIX = ":removeRole";

  private final ActorRef<UserAdminCommand> userAdminActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  public UserAdminRoute(
      ActorRef<UserAdminCommand> userAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(userAdminActor, system, objectMapper, Duration.ofSeconds(20));
  }

  public UserAdminRoute(
      ActorRef<UserAdminCommand> userAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.userAdminActor = requireNonNull(userAdminActor, "userAdminActor cannot be null");
    this.system = requireNonNull(system, "system cannot be null");
    this.objectMapper = requireNonNull(objectMapper, "objectMapper cannot be null");
    this.askTimeout = requireNonNull(askTimeout, "askTimeout cannot be null");
  }

  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/admin/v1/users"),
        () ->
            concat(
                pathEndOrSingleSlash(
                    () ->
                        concat(
                            get(this::handleListUsers),
                            post(
                                () ->
                                    entity(
                                        Jackson.unmarshaller(objectMapper, CreateUserRequest.class),
                                        this::handleCreateUser)))),
                path(PathMatchers.segment(), this::handleUserSegment),
                pathPrefix(
                    PathMatchers.segment(),
                    id ->
                        concat(
                            path(
                                ADD_ROLE_SUFFIX,
                                () ->
                                    post(
                                        () ->
                                            entity(
                                                Jackson.unmarshaller(
                                                    objectMapper, RoleAssignmentRequest.class),
                                                req -> handleAddRole(id, req)))),
                            path(
                                REMOVE_ROLE_SUFFIX,
                                () ->
                                    post(
                                        () ->
                                            entity(
                                                Jackson.unmarshaller(
                                                    objectMapper, RoleAssignmentRequest.class),
                                                req -> handleRemoveRole(id, req))))))));
  }

  private Route handleUserSegment(String segment) {
    if (segment.endsWith(ADD_ROLE_SUFFIX)) {
      var id = segment.substring(0, segment.length() - ADD_ROLE_SUFFIX.length());
      return post(
          () ->
              entity(
                  Jackson.unmarshaller(objectMapper, RoleAssignmentRequest.class),
                  req -> handleAddRole(id, req)));
    }
    if (segment.endsWith(REMOVE_ROLE_SUFFIX)) {
      var id = segment.substring(0, segment.length() - REMOVE_ROLE_SUFFIX.length());
      return post(
          () ->
              entity(
                  Jackson.unmarshaller(objectMapper, RoleAssignmentRequest.class),
                  req -> handleRemoveRole(id, req)));
    }
    return get(() -> handleGetUser(segment));
  }

  private Route handleCreateUser(CreateUserRequest request) {
    return onComplete(
        () -> askCreateUser(request), responseTry -> mapResponseToRoute(responseTry, true));
  }

  private CompletionStage<UserAdminResponse> askCreateUser(CreateUserRequest request) {
    return ask(
        userAdminActor,
        replyTo ->
            new UserAdminCommand.CreateUser(
                request.username(), request.status(), request.roles(), replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route handleListUsers() {
    return onComplete(this::askListUsers, responseTry -> mapResponseToRoute(responseTry, false));
  }

  private CompletionStage<UserAdminResponse> askListUsers() {
    return ask(userAdminActor, UserAdminCommand.ListUsers::new, askTimeout, system.scheduler());
  }

  private Route handleGetUser(String identifier) {
    return onComplete(
        () -> askGetUser(identifier), responseTry -> mapResponseToRoute(responseTry, false));
  }

  private CompletionStage<UserAdminResponse> askGetUser(String identifier) {
    var maybeUuid = AdminValidation.tryParseUuid(identifier);
    return ask(
        userAdminActor,
        replyTo ->
            maybeUuid
                .map(uuid -> (UserAdminCommand) new UserAdminCommand.GetUserById(uuid, replyTo))
                .orElseGet(() -> new UserAdminCommand.GetUserByUsername(identifier, replyTo)),
        askTimeout,
        system.scheduler());
  }

  private Route handleAddRole(String userIdentifier, RoleAssignmentRequest req) {
    return onComplete(
        () -> askAddRole(userIdentifier, req),
        responseTry -> mapResponseToRoute(responseTry, false));
  }

  private CompletionStage<UserAdminResponse> askAddRole(
      String userIdentifier, RoleAssignmentRequest req) {
    return ask(
        userAdminActor,
        replyTo ->
            new UserAdminCommand.AddRole(userIdentifier, req.roleId(), req.roleName(), replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route handleRemoveRole(String userIdentifier, RoleAssignmentRequest req) {
    return onComplete(
        () -> askRemoveRole(userIdentifier, req),
        responseTry -> mapResponseToRoute(responseTry, false));
  }

  private CompletionStage<UserAdminResponse> askRemoveRole(
      String userIdentifier, RoleAssignmentRequest req) {
    return ask(
        userAdminActor,
        replyTo ->
            new UserAdminCommand.RemoveRole(userIdentifier, req.roleId(), req.roleName(), replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapResponseToRoute(Try<UserAdminResponse> responseTry, boolean isCreateOperation) {
    if (responseTry.isFailure()) {
      logger.error("User admin actor request failed", responseTry.failed().get());
      return complete(
          StatusCodes.INTERNAL_SERVER_ERROR,
          AdminErrorResponse.of(500, "Internal server error"),
          Jackson.marshaller(objectMapper));
    }
    return switch (responseTry.get()) {
      case UserAdminResponse.UserSingle single ->
          complete(
              isCreateOperation ? StatusCodes.CREATED : StatusCodes.OK,
              single.user(),
              Jackson.marshaller(objectMapper));
      case UserAdminResponse.UserList list ->
          completeOK(list.users(), Jackson.marshaller(objectMapper));
      case UserAdminResponse.NotFound nf ->
          complete(
              StatusCodes.NOT_FOUND,
              AdminErrorResponse.of(404, nf.message()),
              Jackson.marshaller(objectMapper));
      case UserAdminResponse.Conflict c ->
          complete(
              StatusCodes.CONFLICT,
              AdminErrorResponse.of(409, c.message()),
              Jackson.marshaller(objectMapper));
      case UserAdminResponse.BadRequest br ->
          complete(
              StatusCodes.BAD_REQUEST,
              AdminErrorResponse.of(400, br.message()),
              Jackson.marshaller(objectMapper));
      case UserAdminResponse.Failure f ->
          complete(
              StatusCodes.INTERNAL_SERVER_ERROR,
              AdminErrorResponse.of(500, f.message()),
              Jackson.marshaller(objectMapper));
    };
  }
}
