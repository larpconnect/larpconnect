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

/** HTTP route handling studio management operations. */
public final class StudioAdminRoute extends AllDirectives {

  private final Logger logger = LoggerFactory.getLogger(StudioAdminRoute.class);

  private final ActorRef<StudioAdminCommand> studioAdminActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  public StudioAdminRoute(
      ActorRef<StudioAdminCommand> studioAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioAdminActor, system, objectMapper, Duration.ofSeconds(20));
  }

  /**
   * Package-private constructor allowing custom ask timeouts during unit testing.
   *
   * @param studioAdminActor actor handling studio administration commands
   * @param system typed actor system
   * @param objectMapper Jackson object mapper
   * @param askTimeout timeout duration for ask operations
   */
  StudioAdminRoute(
      ActorRef<StudioAdminCommand> studioAdminActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioAdminActor = requireNonNull(studioAdminActor, "studioAdminActor cannot be null");
    this.system = requireNonNull(system, "system cannot be null");
    this.objectMapper = requireNonNull(objectMapper, "objectMapper cannot be null");
    this.askTimeout = requireNonNull(askTimeout, "askTimeout cannot be null");
  }

  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/admin/v1/studios"),
        () ->
            concat(
                pathEndOrSingleSlash(
                    () ->
                        concat(
                            get(
                                () ->
                                    parameterOptional(
                                        "include_deleted",
                                        val ->
                                            handleListStudios(
                                                val.map(Boolean::parseBoolean).orElse(false)))),
                            post(
                                () ->
                                    entity(
                                        Jackson.unmarshaller(
                                            objectMapper, CreateStudioRequest.class),
                                        this::handleCreateStudio)))),
                path(
                    PathMatchers.segment(),
                    id ->
                        get(
                            () ->
                                parameterOptional(
                                    "include_deleted",
                                    val ->
                                        handleGetStudio(
                                            id, val.map(Boolean::parseBoolean).orElse(false)))))));
  }

  private Route handleCreateStudio(CreateStudioRequest request) {
    return onComplete(
        () -> askCreateStudio(request.alias()),
        responseTry -> mapResponseToRoute(responseTry, true));
  }

  private CompletionStage<StudioAdminResponse> askCreateStudio(String alias) {
    return ask(
        studioAdminActor,
        replyTo -> new StudioAdminCommand.CreateStudio(alias, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route handleListStudios(boolean includeDeleted) {
    return onComplete(
        () -> askListStudios(includeDeleted),
        responseTry -> mapResponseToRoute(responseTry, false));
  }

  private CompletionStage<StudioAdminResponse> askListStudios(boolean includeDeleted) {
    return ask(
        studioAdminActor,
        replyTo -> new StudioAdminCommand.ListStudios(includeDeleted, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route handleGetStudio(String identifier, boolean includeDeleted) {
    return onComplete(
        () -> askGetStudio(identifier, includeDeleted),
        responseTry -> mapResponseToRoute(responseTry, false));
  }

  private CompletionStage<StudioAdminResponse> askGetStudio(
      String identifier, boolean includeDeleted) {
    var maybeUuid = AdminValidation.tryParseUuid(identifier);
    return ask(
        studioAdminActor,
        replyTo ->
            maybeUuid
                .map(
                    uuid ->
                        (StudioAdminCommand)
                            new StudioAdminCommand.GetStudioById(uuid, includeDeleted, replyTo))
                .orElseGet(
                    () ->
                        new StudioAdminCommand.GetStudioByAlias(
                            identifier, includeDeleted, replyTo)),
        askTimeout,
        system.scheduler());
  }

  private Route mapResponseToRoute(
      Try<StudioAdminResponse> responseTry, boolean isCreateOperation) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return mapSuccessResponse(responseTry.get(), isCreateOperation);
  }

  private Route handleActorFailure(Throwable error) {
    logger.error("Studio admin actor request failed", error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        AdminErrorResponse.of(500, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private Route mapSuccessResponse(StudioAdminResponse response, boolean isCreateOperation) {
    return switch (response) {
      case StudioAdminResponse.StudioSingle single ->
          complete(
              resolveSuccessStatus(isCreateOperation),
              single.studio(),
              Jackson.marshaller(objectMapper));
      case StudioAdminResponse.StudioList list ->
          completeOK(list.studios(), Jackson.marshaller(objectMapper));
      default -> mapErrorResponse(response);
    };
  }

  private Route mapErrorResponse(StudioAdminResponse response) {
    return switch (response) {
      case StudioAdminResponse.NotFound nf ->
          complete(
              StatusCodes.NOT_FOUND,
              AdminErrorResponse.of(404, nf.message()),
              Jackson.marshaller(objectMapper));
      case StudioAdminResponse.Conflict c ->
          complete(
              StatusCodes.CONFLICT,
              AdminErrorResponse.of(409, c.message()),
              Jackson.marshaller(objectMapper));
      case StudioAdminResponse.BadRequest br ->
          complete(
              StatusCodes.BAD_REQUEST,
              AdminErrorResponse.of(400, br.message()),
              Jackson.marshaller(objectMapper));
      case StudioAdminResponse.Failure f ->
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

  private static StatusCode resolveSuccessStatus(boolean isCreateOperation) {
    return isCreateOperation ? StatusCodes.CREATED : StatusCodes.OK;
  }
}
