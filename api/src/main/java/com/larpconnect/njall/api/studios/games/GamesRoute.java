package com.larpconnect.njall.api.studios.games;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
import com.larpconnect.njall.api.studios.common.UpdateMaskSupport;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.MalformedRequestContentRejection;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.RejectionHandler;
import org.apache.pekko.http.javadsl.server.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

/** HTTP route handling tenanted studio game management. */
public final class GamesRoute extends AllDirectives implements RouteProvider {

  private static final ImmutableList<String> CREATE_REQUIRED_FIELDS =
      ImmutableList.of("generator", "system", "title");
  private static final ImmutableList<String> PATCH_REQUIRED_FIELDS =
      ImmutableList.of("system", "title");
  private static final ImmutableList<String> OPTIONAL_TEXT_FIELDS =
      ImmutableList.of("campaign", "summary", "locationId", "startTime", "endTime");

  private final Logger logger = LoggerFactory.getLogger(GamesRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<GameCommand> gameActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  GamesRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<GameCommand> gameActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioLookupCache, gameActor, system, objectMapper, Duration.ofSeconds(20));
  }

  GamesRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<GameCommand> gameActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.gameActor = gameActor;
    this.system = system;
    this.objectMapper = objectMapper;
    this.askTimeout = askTimeout;
  }

  @Override
  public Route route() {
    return pathPrefix(
        PathMatchers.separateOnSlashes("api/studios"),
        () ->
            pathPrefix(
                PathMatchers.segment(),
                studioIdParam ->
                    pathPrefix(
                        PathMatchers.separateOnSlashes("v1/games"),
                        () -> handleGames(studioIdParam))));
  }

  private Route handleGames(String studioIdParam) {
    return studioLookupCache
        .findByIdOrAlias(studioIdParam)
        .filter(this::isActive)
        .map(lookup -> buildTenantedRoutes(lookup, studioIdParam))
        .orElseGet(
            () -> completeError(StatusCodes.NOT_FOUND, "Studio not found: " + studioIdParam));
  }

  private boolean isActive(StudioLookup lookup) {
    return !lookup.isDeleted();
  }

  private Route buildTenantedRoutes(StudioLookup lookup, String studioIdParam) {
    return handleRejections(
        malformedRequestRejectionHandler(),
        () ->
            concat(
                pathEndOrSingleSlash(
                    () ->
                        concat(
                            post(() -> handlePost(lookup, studioIdParam)),
                            get(() -> handleList(lookup, studioIdParam)))),
                pathPrefix(
                    PathMatchers.segment(),
                    gameIdParam -> handleGameItem(lookup, studioIdParam, gameIdParam))));
  }

  private RejectionHandler malformedRequestRejectionHandler() {
    return RejectionHandler.newBuilder()
        .handle(
            MalformedRequestContentRejection.class,
            rejection -> completeError(StatusCodes.BAD_REQUEST, rejection.message()))
        .build();
  }

  private Route handleGameItem(StudioLookup lookup, String studioIdParam, String gameIdParam) {
    return GameValidation.tryParseUuid(gameIdParam)
        .map(gameId -> buildItemRoutes(lookup, studioIdParam, gameId))
        .orElseGet(() -> completeError(StatusCodes.NOT_FOUND, "Game not found: " + gameIdParam));
  }

  private Route buildItemRoutes(StudioLookup lookup, String studioIdParam, UUID gameId) {
    return pathEndOrSingleSlash(
        () ->
            concat(
                get(() -> handleGet(lookup, studioIdParam, gameId)),
                patch(() -> handlePatch(lookup, studioIdParam, gameId)),
                delete(() -> handleDelete(lookup, gameId))));
  }

  private Route handlePost(StudioLookup lookup, String studioIdParam) {
    return handleJsonBody(
        CreateGameRequest.class,
        CREATE_REQUIRED_FIELDS,
        false,
        request ->
            onComplete(
                () -> askCreateGame(lookup, studioIdParam, request),
                res -> mapResponse(res, StatusCodes.CREATED)));
  }

  private Route handleList(StudioLookup lookup, String studioIdParam) {
    return parameterOptional(
        "systemId",
        systemId ->
            parameterOptional(
                "campaignId",
                campaignId ->
                    onComplete(
                        () -> askListGames(lookup, studioIdParam, systemId, campaignId),
                        res -> mapResponse(res, StatusCodes.OK))));
  }

  private Route handleGet(StudioLookup lookup, String studioIdParam, UUID gameId) {
    return onComplete(
        () -> askGetGame(lookup.tenantId(), studioIdParam, gameId),
        res -> mapResponse(res, StatusCodes.OK));
  }

  private Route handlePatch(StudioLookup lookup, String studioIdParam, UUID gameId) {
    return parameterOptional(
        "update_mask",
        updateMask ->
            handleJsonBody(
                UpdateGameRequest.class,
                PATCH_REQUIRED_FIELDS,
                UpdateMaskSupport.parseUpdateMask(updateMask).isPresent(),
                request ->
                    onComplete(
                        () -> askPatchGame(lookup, studioIdParam, gameId, request, updateMask),
                        res -> mapResponse(res, StatusCodes.OK))));
  }

  private Route handleDelete(StudioLookup lookup, UUID gameId) {
    return onComplete(
        () -> askDeleteGame(lookup.tenantId(), gameId),
        res -> mapResponse(res, StatusCodes.NO_CONTENT));
  }

  private <T> Route handleJsonBody(
      Class<T> targetType,
      ImmutableList<String> requiredFields,
      boolean allowNullRequiredFields,
      Function<T, Route> onParsed) {
    return entity(
        Jackson.unmarshaller(objectMapper, JsonNode.class),
        body -> decodeAndRun(body, targetType, requiredFields, allowNullRequiredFields, onParsed));
  }

  private <T> Route decodeAndRun(
      JsonNode body,
      Class<T> targetType,
      ImmutableList<String> requiredFields,
      boolean allowNullRequiredFields,
      Function<T, Route> onParsed) {
    var jsonError = validateJsonBody(body, requiredFields, allowNullRequiredFields);
    if (jsonError.isPresent()) {
      return completeError(StatusCodes.BAD_REQUEST, jsonError.orElseThrow());
    }
    try {
      return onParsed.apply(objectMapper.treeToValue(body, targetType));
    } catch (JsonProcessingException e) {
      var detail = Optional.ofNullable(e.getOriginalMessage()).orElse("Malformed request payload");
      return completeError(StatusCodes.BAD_REQUEST, "Invalid request payload: " + detail);
    }
  }

  private static Optional<String> validateJsonBody(
      JsonNode body, ImmutableList<String> requiredFields, boolean allowNullRequiredFields) {
    if (!body.isObject()) {
      return Optional.of("Request body must be a JSON object");
    }
    for (var field : requiredFields) {
      if (hasInvalidRequiredField(body, field, allowNullRequiredFields)) {
        return Optional.of("Game " + field + " must be a string");
      }
    }
    for (var field : OPTIONAL_TEXT_FIELDS) {
      if (hasNonTextualOptionalField(body, field)) {
        return Optional.of("Game " + field + " must be a string or null");
      }
    }
    return Optional.empty();
  }

  private static boolean hasInvalidRequiredField(
      JsonNode body, String fieldName, boolean allowNullRequiredFields) {
    if (!body.has(fieldName)) {
      return false;
    }
    var node = body.get(fieldName);
    return allowNullRequiredFields ? (!node.isNull() && !node.isTextual()) : !node.isTextual();
  }

  private static boolean hasNonTextualOptionalField(JsonNode body, String fieldName) {
    return body.has(fieldName) && !body.get(fieldName).isNull() && !body.get(fieldName).isTextual();
  }

  private CompletionStage<GameActorResponse> askCreateGame(
      StudioLookup lookup, String studioIdentifier, CreateGameRequest request) {
    return ask(
        gameActor,
        replyTo ->
            new GameCommand.CreateGame(
                lookup.tenantId(), studioIdentifier, Optional.of(lookup), request, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<GameActorResponse> askGetGame(
      UUID tenantId, String studioIdentifier, UUID gameId) {
    return ask(
        gameActor,
        replyTo -> new GameCommand.GetGame(tenantId, studioIdentifier, gameId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<GameActorResponse> askListGames(
      StudioLookup lookup,
      String studioIdentifier,
      Optional<String> systemId,
      Optional<String> campaignId) {
    return ask(
        gameActor,
        replyTo ->
            new GameCommand.ListGames(
                lookup.tenantId(),
                studioIdentifier,
                Optional.of(lookup),
                systemId,
                campaignId,
                replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<GameActorResponse> askPatchGame(
      StudioLookup lookup,
      String studioIdentifier,
      UUID gameId,
      UpdateGameRequest request,
      Optional<String> updateMask) {
    return ask(
        gameActor,
        replyTo ->
            new GameCommand.PatchGame(
                lookup.tenantId(),
                studioIdentifier,
                Optional.of(lookup),
                gameId,
                request,
                updateMask,
                replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<GameActorResponse> askDeleteGame(UUID tenantId, UUID gameId) {
    return ask(
        gameActor,
        replyTo -> new GameCommand.DeleteGame(tenantId, gameId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapResponse(Try<GameActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      logger.error("Failed waiting for GameActor response", responseTry.failed().get());
      return completeError(StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error");
    }
    return mapSuccess(responseTry.get(), successStatus);
  }

  private Route mapSuccess(GameActorResponse response, StatusCode successStatus) {
    return switch (response) {
      case GameActorResponse.Success s ->
          complete(successStatus, s.game(), Jackson.marshaller(objectMapper));
      case GameActorResponse.Items items ->
          complete(StatusCodes.OK, items.games(), Jackson.marshaller(objectMapper));
      case GameActorResponse.Deleted _ -> complete(StatusCodes.NO_CONTENT);
      case GameActorResponse.Failure f -> completeError(StatusCodes.get(f.status()), f.message());
    };
  }

  private Route completeError(StatusCode status, String message) {
    return complete(status, createErrorResponse(status, message), Jackson.marshaller(objectMapper));
  }

  private StudioErrorResponse createErrorResponse(StatusCode status, String message) {
    return new StudioErrorResponse(status.intValue(), message);
  }
}
