package com.larpconnect.njall.api.studios.actors;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.domain.StudioLookup;
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

/** HTTP route handling tenanted studio actors (agents). */
public final class AgentRoute extends AllDirectives implements RouteProvider {

  private static final String ADD_ENDPOINT_SUFFIX = ":addEndpoint";
  private static final String REMOVE_ENDPOINT_SUFFIX = ":removeEndpoint";

  private final Logger logger = LoggerFactory.getLogger(AgentRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<AgentCommand> agentActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  AgentRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<AgentCommand> agentActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioLookupCache, agentActor, system, objectMapper, Duration.ofSeconds(20));
  }

  AgentRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<AgentCommand> agentActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.agentActor = agentActor;
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
                        PathMatchers.separateOnSlashes("v1/actors"),
                        () -> handleActors(studioIdParam))));
  }

  private Route handleActors(String studioIdParam) {
    return studioLookupCache
        .findByIdOrAlias(studioIdParam)
        .filter(this::isActive)
        .map(lookup -> buildTenantedRoutes(lookup, studioIdParam))
        .orElseGet(() -> completeNotFound("Studio not found: " + studioIdParam));
  }

  private boolean isActive(StudioLookup lookup) {
    return !lookup.isDeleted();
  }

  private Route buildTenantedRoutes(StudioLookup lookup, String studioIdParam) {
    return concat(
        pathEndOrSingleSlash(
            () ->
                concat(
                    post(() -> handlePostActor(lookup.tenantId(), studioIdParam)),
                    get(() -> completeNotFound("Listing actors is not supported")))),
        pathPrefix(
            PathMatchers.segment(),
            segment -> handleActorBranch(lookup.tenantId(), studioIdParam, segment)));
  }

  private Route handleActorBranch(UUID tenantId, String studioIdParam, String segment) {
    if (segment.endsWith(ADD_ENDPOINT_SUFFIX)) {
      var idStr = segment.substring(0, segment.length() - ADD_ENDPOINT_SUFFIX.length());
      return AgentValidation.tryParseUuid(idStr)
          .map(actorId -> post(() -> handleAddEndpoint(tenantId, actorId)))
          .orElseGet(() -> completeNotFound("Actor not found: " + segment));
    }
    if (segment.endsWith(REMOVE_ENDPOINT_SUFFIX)) {
      var idStr = segment.substring(0, segment.length() - REMOVE_ENDPOINT_SUFFIX.length());
      return AgentValidation.tryParseUuid(idStr)
          .map(actorId -> post(() -> handleRemoveEndpoint(tenantId, actorId)))
          .orElseGet(() -> completeNotFound("Actor not found: " + segment));
    }
    return AgentValidation.tryParseUuid(segment)
        .map(actorId -> buildActorSubroutes(tenantId, studioIdParam, actorId))
        .orElseGet(() -> completeNotFound("Actor not found: " + segment));
  }

  private Route buildActorSubroutes(UUID tenantId, String studioIdParam, UUID actorId) {
    return concat(
        pathEndOrSingleSlash(
            () ->
                concat(
                    get(() -> handleGetActor(tenantId, studioIdParam, actorId)),
                    patch(() -> handlePatchActor(tenantId, studioIdParam, actorId)),
                    delete(() -> handleDeleteActor(tenantId, actorId)))),
        path(ADD_ENDPOINT_SUFFIX, () -> post(() -> handleAddEndpoint(tenantId, actorId))),
        path(REMOVE_ENDPOINT_SUFFIX, () -> post(() -> handleRemoveEndpoint(tenantId, actorId))),
        path(
            "inbox",
            () ->
                concat(
                    get(() -> handleGetInbox(tenantId, actorId)),
                    post(() -> handlePostInbox(tenantId, actorId)))),
        path(
            "outbox",
            () ->
                concat(
                    get(() -> handleGetOutbox(tenantId, actorId)),
                    post(() -> handlePostOutbox(tenantId, actorId)))));
  }

  private Route handlePostActor(UUID tenantId, String studioIdParam) {
    return entity(
        Jackson.unmarshaller(objectMapper, CreateActorRequest.class),
        request ->
            onComplete(
                () -> askCreateActor(tenantId, studioIdParam, request),
                res -> mapActorResponse(res, StatusCodes.CREATED)));
  }

  private Route handleGetActor(UUID tenantId, String studioIdParam, UUID actorId) {
    return onComplete(
        () -> askGetActor(tenantId, studioIdParam, actorId),
        res -> mapActorResponse(res, StatusCodes.OK));
  }

  private Route handlePatchActor(UUID tenantId, String studioIdParam, UUID actorId) {
    return parameterOptional(
        "update_mask",
        mask ->
            entity(
                Jackson.unmarshaller(objectMapper, UpdateActorRequest.class),
                req ->
                    onComplete(
                        () -> askPatchActor(tenantId, studioIdParam, actorId, req, mask),
                        res -> mapActorResponse(res, StatusCodes.OK))));
  }

  private Route handleDeleteActor(UUID tenantId, UUID actorId) {
    return onComplete(
        () -> askDeleteActor(tenantId, actorId),
        res -> mapActorResponse(res, StatusCodes.NO_CONTENT));
  }

  private Route handleAddEndpoint(UUID tenantId, UUID actorId) {
    return entity(
        Jackson.unmarshaller(objectMapper, AddEndpointRequest.class),
        request ->
            onComplete(
                () -> askAddEndpoint(tenantId, actorId, request),
                res -> mapActorResponse(res, StatusCodes.NO_CONTENT)));
  }

  private Route handleRemoveEndpoint(UUID tenantId, UUID actorId) {
    return entity(
        Jackson.unmarshaller(objectMapper, RemoveEndpointRequest.class),
        request ->
            onComplete(
                () -> askRemoveEndpoint(tenantId, actorId, request),
                res -> mapActorResponse(res, StatusCodes.NO_CONTENT)));
  }

  private Route handleGetInbox(UUID tenantId, UUID actorId) {
    return onComplete(
        () -> askGetInbox(tenantId, actorId), res -> mapActorResponse(res, StatusCodes.OK));
  }

  private Route handlePostInbox(UUID tenantId, UUID actorId) {
    return entity(
        Jackson.unmarshaller(objectMapper, JsonNode.class),
        payload ->
            onComplete(
                () -> askPostInbox(tenantId, actorId, payload),
                res -> mapActorResponse(res, StatusCodes.CREATED)));
  }

  private Route handleGetOutbox(UUID tenantId, UUID actorId) {
    return onComplete(
        () -> askGetOutbox(tenantId, actorId), res -> mapActorResponse(res, StatusCodes.OK));
  }

  private Route handlePostOutbox(UUID tenantId, UUID actorId) {
    return entity(
        Jackson.unmarshaller(objectMapper, JsonNode.class),
        payload ->
            onComplete(
                () -> askPostOutbox(tenantId, actorId, payload),
                res -> mapActorResponse(res, StatusCodes.CREATED)));
  }

  private CompletionStage<AgentActorResponse> askCreateActor(
      UUID tenantId, String studioIdentifier, CreateActorRequest request) {
    return ask(
        agentActor,
        replyTo -> new AgentCommand.CreateAgent(tenantId, studioIdentifier, request, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AgentActorResponse> askGetActor(
      UUID tenantId, String studioIdentifier, UUID actorId) {
    return ask(
        agentActor,
        replyTo -> new AgentCommand.GetAgent(tenantId, studioIdentifier, actorId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AgentActorResponse> askPatchActor(
      UUID tenantId,
      String studioIdentifier,
      UUID actorId,
      UpdateActorRequest request,
      Optional<String> updateMask) {
    return ask(
        agentActor,
        replyTo ->
            new AgentCommand.PatchAgent(
                tenantId, studioIdentifier, actorId, request, updateMask, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AgentActorResponse> askDeleteActor(UUID tenantId, UUID actorId) {
    return ask(
        agentActor,
        replyTo -> new AgentCommand.DeleteAgent(tenantId, actorId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AgentActorResponse> askAddEndpoint(
      UUID tenantId, UUID actorId, AddEndpointRequest request) {
    return ask(
        agentActor,
        replyTo ->
            new AgentCommand.MutateEndpoint(
                tenantId,
                actorId,
                AgentCommand.EndpointAction.ADD,
                request.name(),
                Optional.of(request.endpoint()),
                replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AgentActorResponse> askRemoveEndpoint(
      UUID tenantId, UUID actorId, RemoveEndpointRequest request) {
    return ask(
        agentActor,
        replyTo ->
            new AgentCommand.MutateEndpoint(
                tenantId,
                actorId,
                AgentCommand.EndpointAction.REMOVE,
                request.name(),
                Optional.empty(),
                replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AgentActorResponse> askGetInbox(UUID tenantId, UUID actorId) {
    return ask(
        agentActor,
        replyTo ->
            new AgentCommand.AccessCollection(
                tenantId,
                actorId,
                AgentCommand.CollectionTarget.INBOX,
                AgentCommand.CollectionAction.GET,
                Optional.empty(),
                replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AgentActorResponse> askGetOutbox(UUID tenantId, UUID actorId) {
    return ask(
        agentActor,
        replyTo ->
            new AgentCommand.AccessCollection(
                tenantId,
                actorId,
                AgentCommand.CollectionTarget.OUTBOX,
                AgentCommand.CollectionAction.GET,
                Optional.empty(),
                replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AgentActorResponse> askPostInbox(
      UUID tenantId, UUID actorId, JsonNode payload) {
    return ask(
        agentActor,
        replyTo ->
            new AgentCommand.AccessCollection(
                tenantId,
                actorId,
                AgentCommand.CollectionTarget.INBOX,
                AgentCommand.CollectionAction.POST,
                Optional.of(payload),
                replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<AgentActorResponse> askPostOutbox(
      UUID tenantId, UUID actorId, JsonNode payload) {
    return ask(
        agentActor,
        replyTo ->
            new AgentCommand.AccessCollection(
                tenantId,
                actorId,
                AgentCommand.CollectionTarget.OUTBOX,
                AgentCommand.CollectionAction.POST,
                Optional.of(payload),
                replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapActorResponse(Try<AgentActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      return handleActorFailure("Actor", responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case AgentActorResponse.Success s -> {
        if (s.payload().isEmpty()) {
          yield complete(successStatus);
        }
        var obj = s.payload().orElseThrow();
        yield complete(successStatus, obj, Jackson.marshaller(objectMapper));
      }
      case AgentActorResponse.Failure f -> {
        var status = StatusCodes.get(f.status());
        yield complete(
            status, createErrorResponse(status, f.message()), Jackson.marshaller(objectMapper));
      }
    };
  }

  private Route completeNotFound(String message) {
    return complete(
        StatusCodes.NOT_FOUND,
        createErrorResponse(StatusCodes.NOT_FOUND, message),
        Jackson.marshaller(objectMapper));
  }

  private Route handleActorFailure(String entityType, Throwable error) {
    logger.error("{} actor request failed", entityType, error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        createErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private StudioErrorResponse createErrorResponse(StatusCode status, String message) {
    return new StudioErrorResponse(status.intValue(), message);
  }
}
