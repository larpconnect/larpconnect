package com.larpconnect.njall.api.studios.systems;

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

/** HTTP route handling tenanted studio game systems. */
public final class SystemsRoute extends AllDirectives implements RouteProvider {

  private static final ImmutableList<String> CREATE_REQUIRED_FIELDS =
      ImmutableList.of("generator", "name");
  private static final ImmutableList<String> PATCH_REQUIRED_FIELDS = ImmutableList.of("name");
  private static final ImmutableList<String> OPTIONAL_TEXT_FIELDS = ImmutableList.of("summary");

  private final Logger logger = LoggerFactory.getLogger(SystemsRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<GameSystemCommand> gameSystemActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  SystemsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<GameSystemCommand> gameSystemActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioLookupCache, gameSystemActor, system, objectMapper, Duration.ofSeconds(20));
  }

  SystemsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<GameSystemCommand> gameSystemActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.gameSystemActor = gameSystemActor;
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
                        PathMatchers.separateOnSlashes("v1/systems"),
                        () -> handleSystems(studioIdParam))));
  }

  private Route handleSystems(String studioIdParam) {
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
                            get(() -> handleListSystems(lookup.tenantId(), studioIdParam)),
                            post(() -> handlePostSystem(lookup, studioIdParam)))),
                pathPrefix(
                    PathMatchers.segment(),
                    systemIdParam ->
                        handleSystemBranch(lookup.tenantId(), studioIdParam, systemIdParam))));
  }

  private RejectionHandler malformedRequestRejectionHandler() {
    return RejectionHandler.newBuilder()
        .handle(
            MalformedRequestContentRejection.class,
            rejection -> completeError(StatusCodes.BAD_REQUEST, rejection.message()))
        .build();
  }

  private Route handleListSystems(UUID tenantId, String studioIdParam) {
    return onComplete(
        () -> askListSystems(tenantId, studioIdParam),
        res -> mapSystemResponse(res, StatusCodes.OK));
  }

  private Route handlePostSystem(StudioLookup lookup, String studioIdParam) {
    return handleJsonBody(
        CreateGameSystemRequest.class,
        CREATE_REQUIRED_FIELDS,
        OPTIONAL_TEXT_FIELDS,
        false,
        request ->
            onComplete(
                () -> askCreateSystem(lookup, studioIdParam, request),
                res -> mapSystemResponse(res, StatusCodes.CREATED)));
  }

  private Route handleSystemBranch(UUID tenantId, String studioIdParam, String systemIdParam) {
    return GameSystemValidation.tryParseUuid(systemIdParam)
        .map(systemId -> buildSystemSubroutes(tenantId, studioIdParam, systemId))
        .orElseGet(
            () -> completeError(StatusCodes.NOT_FOUND, "Game system not found: " + systemIdParam));
  }

  private Route buildSystemSubroutes(UUID tenantId, String studioIdParam, UUID systemId) {
    return pathEndOrSingleSlash(
        () ->
            concat(
                get(() -> handleGetSystem(tenantId, studioIdParam, systemId)),
                patch(() -> handlePatchSystem(tenantId, studioIdParam, systemId)),
                delete(() -> handleDeleteSystem(tenantId, systemId))));
  }

  private Route handleGetSystem(UUID tenantId, String studioIdParam, UUID systemId) {
    return onComplete(
        () -> askGetSystem(tenantId, studioIdParam, systemId),
        res -> mapSystemResponse(res, StatusCodes.OK));
  }

  private Route handlePatchSystem(UUID tenantId, String studioIdParam, UUID systemId) {
    return parameterOptional(
        "update_mask",
        mask ->
            handleJsonBody(
                UpdateGameSystemRequest.class,
                PATCH_REQUIRED_FIELDS,
                OPTIONAL_TEXT_FIELDS,
                UpdateMaskSupport.parseUpdateMask(mask).isPresent(),
                req ->
                    onComplete(
                        () -> askPatchSystem(tenantId, studioIdParam, systemId, req, mask),
                        res -> mapSystemResponse(res, StatusCodes.OK))));
  }

  private Route handleDeleteSystem(UUID tenantId, UUID systemId) {
    return onComplete(
        () -> askDeleteSystem(tenantId, systemId),
        res -> mapSystemResponse(res, StatusCodes.NO_CONTENT));
  }

  private <T> Route handleJsonBody(
      Class<T> targetType,
      ImmutableList<String> requiredFields,
      ImmutableList<String> optionalFields,
      boolean allowNullRequiredFields,
      Function<T, Route> onParsed) {
    return entity(
        Jackson.unmarshaller(objectMapper, JsonNode.class),
        body ->
            decodeAndRun(
                body,
                targetType,
                requiredFields,
                optionalFields,
                allowNullRequiredFields,
                onParsed));
  }

  private <T> Route decodeAndRun(
      JsonNode body,
      Class<T> targetType,
      ImmutableList<String> requiredFields,
      ImmutableList<String> optionalFields,
      boolean allowNullRequiredFields,
      Function<T, Route> onParsed) {
    var jsonError = validateJsonBody(body, requiredFields, optionalFields, allowNullRequiredFields);
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

  private Optional<String> validateJsonBody(
      JsonNode body,
      ImmutableList<String> requiredFields,
      ImmutableList<String> optionalFields,
      boolean allowNullRequiredFields) {
    if (!body.isObject()) {
      return Optional.of("Request body must be a JSON object");
    }
    for (var field : requiredFields) {
      if (hasInvalidRequiredField(body, field, allowNullRequiredFields)) {
        return Optional.of(field + " must be a string");
      }
    }
    for (var field : optionalFields) {
      if (hasNonTextualOptionalField(body, field)) {
        return Optional.of(field + " must be a string or null");
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

  private CompletionStage<GameSystemActorResponse> askCreateSystem(
      StudioLookup lookup, String studioIdentifier, CreateGameSystemRequest request) {
    return ask(
        gameSystemActor,
        replyTo ->
            new GameSystemCommand.CreateGameSystem(
                lookup.tenantId(), studioIdentifier, request, replyTo, Optional.of(lookup)),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<GameSystemActorResponse> askListSystems(
      UUID tenantId, String studioIdentifier) {
    return ask(
        gameSystemActor,
        replyTo -> new GameSystemCommand.ListGameSystems(tenantId, studioIdentifier, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<GameSystemActorResponse> askGetSystem(
      UUID tenantId, String studioIdentifier, UUID systemId) {
    return ask(
        gameSystemActor,
        replyTo ->
            new GameSystemCommand.GetGameSystem(tenantId, studioIdentifier, systemId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<GameSystemActorResponse> askPatchSystem(
      UUID tenantId,
      String studioIdentifier,
      UUID systemId,
      UpdateGameSystemRequest request,
      Optional<String> updateMask) {
    return ask(
        gameSystemActor,
        replyTo ->
            new GameSystemCommand.PatchGameSystem(
                tenantId, studioIdentifier, systemId, request, updateMask, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<GameSystemActorResponse> askDeleteSystem(UUID tenantId, UUID systemId) {
    return ask(
        gameSystemActor,
        replyTo -> new GameSystemCommand.DeleteGameSystem(tenantId, systemId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapSystemResponse(
      Try<GameSystemActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case GameSystemActorResponse.Success s ->
          complete(successStatus, s.system(), Jackson.marshaller(objectMapper));
      case GameSystemActorResponse.Items i ->
          complete(StatusCodes.OK, i.systems(), Jackson.marshaller(objectMapper));
      case GameSystemActorResponse.Deleted _ -> complete(StatusCodes.NO_CONTENT);
      case GameSystemActorResponse.Failure f ->
          completeError(StatusCodes.get(f.status()), f.message());
    };
  }

  private Route completeError(StatusCode status, String message) {
    return complete(status, createErrorResponse(status, message), Jackson.marshaller(objectMapper));
  }

  private Route handleActorFailure(Throwable error) {
    logger.error("GameSystem actor request failed", error);
    return completeError(StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  private StudioErrorResponse createErrorResponse(StatusCode status, String message) {
    return new StudioErrorResponse(status.intValue(), message);
  }
}
