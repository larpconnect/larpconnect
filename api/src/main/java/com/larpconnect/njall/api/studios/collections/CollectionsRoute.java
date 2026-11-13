package com.larpconnect.njall.api.studios.collections;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.mu.util.Substring;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
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

/** HTTP route handling subordinate collection operations for a generating studio actor. */
public final class CollectionsRoute extends AllDirectives {

  private static final String ADD_ITEM_SUFFIX = ":addItem";
  private static final String REMOVE_ITEM_SUFFIX = ":removeItem";

  private final Logger logger = LoggerFactory.getLogger(CollectionsRoute.class);

  private final ActorRef<CollectionCommand> collectionActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  CollectionsRoute(
      ActorRef<CollectionCommand> collectionActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(collectionActor, system, objectMapper, Duration.ofSeconds(20));
  }

  CollectionsRoute(
      ActorRef<CollectionCommand> collectionActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.collectionActor = collectionActor;
    this.system = system;
    this.objectMapper = objectMapper;
    this.askTimeout = askTimeout;
  }

  public Route collectionsSubroutes(UUID tenantId, String studioIdParam, UUID actorId) {
    return concat(
        pathEndOrSingleSlash(
            () ->
                concat(
                    post(() -> handlePostCollection(tenantId, studioIdParam, actorId)),
                    get(() -> completeNotFound("Listing collections is not supported")))),
        pathPrefix(
            PathMatchers.segment(),
            collectionIdParam ->
                handleCollectionBranch(tenantId, studioIdParam, actorId, collectionIdParam)));
  }

  private Route handleCollectionBranch(
      UUID tenantId, String studioIdParam, UUID actorId, String segment) {
    if (segment.endsWith(ADD_ITEM_SUFFIX)) {
      var idStr = Substring.suffix(ADD_ITEM_SUFFIX).removeFrom(segment);
      return CollectionValidation.tryParseUuid(idStr)
          .map(id -> post(() -> handleAddItem(tenantId, actorId, id)))
          .orElseGet(() -> completeNotFound("Collection not found: " + segment));
    }
    if (segment.endsWith(REMOVE_ITEM_SUFFIX)) {
      var idStr = Substring.suffix(REMOVE_ITEM_SUFFIX).removeFrom(segment);
      return CollectionValidation.tryParseUuid(idStr)
          .map(id -> post(() -> handleRemoveItem(tenantId, actorId, id)))
          .orElseGet(() -> completeNotFound("Collection not found: " + segment));
    }
    return CollectionValidation.tryParseUuid(segment)
        .map(
            collectionId ->
                buildCollectionSubroutes(tenantId, studioIdParam, actorId, collectionId))
        .orElseGet(() -> completeNotFound("Collection not found: " + segment));
  }

  private Route buildCollectionSubroutes(
      UUID tenantId, String studioIdParam, UUID actorId, UUID collectionId) {
    return pathEndOrSingleSlash(
        () ->
            concat(
                get(() -> handleGetCollection(tenantId, studioIdParam, actorId, collectionId)),
                patch(() -> handlePatchCollection(tenantId, studioIdParam, actorId, collectionId)),
                delete(() -> handleDeleteCollection(tenantId, actorId, collectionId))));
  }

  private Route handlePostCollection(UUID tenantId, String studioIdParam, UUID actorId) {
    return entity(
        Jackson.unmarshaller(objectMapper, CreateCollectionRequest.class),
        request ->
            onComplete(
                () -> askCreateCollection(tenantId, studioIdParam, actorId, request),
                res -> mapCollectionResponse(res, StatusCodes.CREATED)));
  }

  private Route handleGetCollection(
      UUID tenantId, String studioIdParam, UUID actorId, UUID collectionId) {
    return parameterOptional(
        "form",
        form ->
            onComplete(
                () -> askGetCollection(tenantId, studioIdParam, actorId, collectionId, form),
                res -> mapCollectionResponse(res, StatusCodes.OK)));
  }

  private Route handlePatchCollection(
      UUID tenantId, String studioIdParam, UUID actorId, UUID collectionId) {
    return parameterOptional(
        "update_mask",
        mask ->
            entity(
                Jackson.unmarshaller(objectMapper, UpdateCollectionRequest.class),
                req ->
                    onComplete(
                        () ->
                            askPatchCollection(
                                tenantId, studioIdParam, actorId, collectionId, req, mask),
                        res -> mapCollectionResponse(res, StatusCodes.OK))));
  }

  private Route handleDeleteCollection(UUID tenantId, UUID actorId, UUID collectionId) {
    return onComplete(
        () -> askDeleteCollection(tenantId, actorId, collectionId),
        res -> mapCollectionResponse(res, StatusCodes.NO_CONTENT));
  }

  private Route handleAddItem(UUID tenantId, UUID actorId, UUID collectionId) {
    return entity(
        Jackson.unmarshaller(objectMapper, AddItemRequest.class),
        req -> processAddItem(tenantId, actorId, collectionId, req));
  }

  private Route processAddItem(UUID tenantId, UUID actorId, UUID collectionId, AddItemRequest req) {
    var validationError = CollectionValidation.validateAddItem(req);
    if (validationError.isPresent()) {
      return complete(
          StatusCodes.BAD_REQUEST,
          createErrorResponse(StatusCodes.BAD_REQUEST, validationError.orElseThrow()),
          Jackson.marshaller(objectMapper));
    }
    return onComplete(
        () -> askAddItem(tenantId, actorId, collectionId, req),
        res -> mapCollectionResponse(res, StatusCodes.NO_CONTENT));
  }

  private Route handleRemoveItem(UUID tenantId, UUID actorId, UUID collectionId) {
    return entity(
        Jackson.unmarshaller(objectMapper, RemoveItemRequest.class),
        req ->
            onComplete(
                () -> askRemoveItem(tenantId, actorId, collectionId, req),
                res -> mapCollectionResponse(res, StatusCodes.NO_CONTENT)));
  }

  private CompletionStage<CollectionActorResponse> askCreateCollection(
      UUID tenantId, String studioIdentifier, UUID actorId, CreateCollectionRequest request) {
    return ask(
        collectionActor,
        replyTo ->
            new CollectionCommand.CreateCollection(
                tenantId, studioIdentifier, actorId, request, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<CollectionActorResponse> askGetCollection(
      UUID tenantId,
      String studioIdentifier,
      UUID actorId,
      UUID collectionId,
      Optional<String> form) {
    return ask(
        collectionActor,
        replyTo ->
            new CollectionCommand.GetCollection(
                tenantId, studioIdentifier, actorId, collectionId, form, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<CollectionActorResponse> askPatchCollection(
      UUID tenantId,
      String studioIdentifier,
      UUID actorId,
      UUID collectionId,
      UpdateCollectionRequest request,
      Optional<String> updateMask) {
    return ask(
        collectionActor,
        replyTo ->
            new CollectionCommand.PatchCollection(
                tenantId, studioIdentifier, actorId, collectionId, request, updateMask, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<CollectionActorResponse> askDeleteCollection(
      UUID tenantId, UUID actorId, UUID collectionId) {
    return ask(
        collectionActor,
        replyTo -> new CollectionCommand.DeleteCollection(tenantId, actorId, collectionId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<CollectionActorResponse> askAddItem(
      UUID tenantId, UUID actorId, UUID collectionId, AddItemRequest request) {
    return ask(
        collectionActor,
        replyTo ->
            new CollectionCommand.MutateItem(
                tenantId,
                actorId,
                collectionId,
                CollectionCommand.ItemAction.ADD,
                request.refersToId(),
                request.before(),
                request.index(),
                replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<CollectionActorResponse> askRemoveItem(
      UUID tenantId, UUID actorId, UUID collectionId, RemoveItemRequest request) {
    return ask(
        collectionActor,
        replyTo ->
            new CollectionCommand.MutateItem(
                tenantId,
                actorId,
                collectionId,
                CollectionCommand.ItemAction.REMOVE,
                request.refersToId(),
                Optional.empty(),
                Optional.empty(),
                replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapCollectionResponse(
      Try<CollectionActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      return handleActorFailure("Collection", responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case CollectionActorResponse.Success s ->
          complete(successStatus, s.collection(), Jackson.marshaller(objectMapper));
      case CollectionActorResponse.NoContent _ -> complete(StatusCodes.NO_CONTENT);
      case CollectionActorResponse.Failure f -> {
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
