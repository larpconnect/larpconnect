package com.larpconnect.njall.api.studios.tags;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
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

/** HTTP route handling tenanted studio hashtag management. */
public final class TagsRoute extends AllDirectives implements RouteProvider {

  private final Logger logger = LoggerFactory.getLogger(TagsRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<TagCommand> tagActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  TagsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<TagCommand> tagActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioLookupCache, tagActor, system, objectMapper, Duration.ofSeconds(20));
  }

  TagsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<TagCommand> tagActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.tagActor = tagActor;
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
                        "v1",
                        () ->
                            concat(
                                // Support Google AIP-233 custom method path (tags:batchCreate)
                                path(
                                    "tags:batchCreate",
                                    () -> post(() -> handleBatchCreate(studioIdParam))),
                                pathPrefix(
                                    "tags",
                                    () ->
                                        concat(
                                            // Support subpath alias for batch creation
                                            // (/tags/:batchCreate)
                                            path(
                                                ":batchCreate",
                                                () -> post(() -> handleBatchCreate(studioIdParam))),
                                            handleTags(studioIdParam)))))));
  }

  private Route handleTags(String studioIdParam) {
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
    var prefix = "/api/studios/" + studioIdParam + "/v1/tags";
    return concat(
        pathEndOrSingleSlash(
            () ->
                concat(
                    get(() -> handleListTags(lookup)), post(() -> handlePostTag(lookup, prefix)))),
        path(PathMatchers.segment(), tagIdParam -> handleTagItem(lookup, tagIdParam)));
  }

  private Route handleListTags(StudioLookup lookup) {
    return onComplete(
        () -> askListTags(lookup.tenantId()), res -> mapResponse(res, StatusCodes.OK));
  }

  private Route handlePostTag(StudioLookup lookup, String prefix) {
    return entity(
        Jackson.unmarshaller(objectMapper, CreateTagRequest.class),
        request ->
            onComplete(
                () -> askCreateTag(lookup.tenantId(), prefix, request), this::mapCreateResponse));
  }

  private Route handleBatchCreate(String studioIdParam) {
    return studioLookupCache
        .findByIdOrAlias(studioIdParam)
        .filter(this::isActive)
        .map(lookup -> executeBatchCreate(lookup, studioIdParam))
        .orElseGet(() -> completeNotFound("Studio not found: " + studioIdParam));
  }

  private Route executeBatchCreate(StudioLookup lookup, String studioIdParam) {
    var prefix = "/api/studios/" + studioIdParam + "/v1/tags";
    return entity(
        Jackson.unmarshaller(objectMapper, BatchCreateTagsRequest.class),
        request ->
            onComplete(
                () -> askBatchCreate(lookup.tenantId(), prefix, request),
                this::mapBatchCreateResponse));
  }

  private Route mapBatchCreateResponse(Try<TagActorResponse> responseTry) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case TagActorResponse.Items items ->
          complete(
              StatusCodes.OK,
              new BatchCreateTagsResponse(items.tags()),
              Jackson.marshaller(objectMapper));
      case TagActorResponse.Failure err ->
          complete(
              StatusCodes.get(err.status()),
              createErrorResponse(StatusCodes.get(err.status()), err.message()),
              Jackson.marshaller(objectMapper));
      case TagActorResponse.Success s ->
          complete(StatusCodes.OK, s.tag(), Jackson.marshaller(objectMapper));
      case TagActorResponse.Deleted _ -> complete(StatusCodes.NO_CONTENT);
    };
  }

  private Route handleTagItem(StudioLookup lookup, String tagIdParam) {
    return concat(
        get(
            () ->
                onComplete(
                    () -> askGetTag(lookup.tenantId(), tagIdParam),
                    res -> mapResponse(res, StatusCodes.OK))),
        patch(
            () ->
                parameterOptional(
                    "update_mask",
                    mask ->
                        entity(
                            Jackson.unmarshaller(objectMapper, UpdateTagRequest.class),
                            req ->
                                onComplete(
                                    () -> askPatchTag(lookup.tenantId(), tagIdParam, req, mask),
                                    res -> mapResponse(res, StatusCodes.OK))))),
        delete(
            () ->
                onComplete(
                    () -> askDeleteTag(lookup.tenantId(), tagIdParam),
                    res -> mapResponse(res, StatusCodes.NO_CONTENT))));
  }

  private CompletionStage<TagActorResponse> askCreateTag(
      UUID tenantId, String prefix, CreateTagRequest request) {
    return ask(
        tagActor,
        replyTo ->
            new TagCommand.CreateTags(tenantId, prefix, ImmutableList.of(request), false, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<TagActorResponse> askBatchCreate(
      UUID tenantId, String prefix, BatchCreateTagsRequest request) {
    return ask(
        tagActor,
        replyTo -> new TagCommand.CreateTags(tenantId, prefix, request.requests(), true, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<TagActorResponse> askGetTag(UUID tenantId, String tagIdParam) {
    return ask(
        tagActor,
        replyTo -> new TagCommand.QueryTag(tenantId, Optional.of(tagIdParam), replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<TagActorResponse> askListTags(UUID tenantId) {
    return ask(
        tagActor,
        replyTo -> new TagCommand.QueryTag(tenantId, Optional.empty(), replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<TagActorResponse> askPatchTag(
      UUID tenantId, String tagIdParam, UpdateTagRequest request, Optional<String> updateMask) {
    return ask(
        tagActor,
        replyTo -> new TagCommand.PatchTag(tenantId, tagIdParam, request, updateMask, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<TagActorResponse> askDeleteTag(UUID tenantId, String tagIdParam) {
    return ask(
        tagActor,
        replyTo -> new TagCommand.DeleteTag(tenantId, tagIdParam, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapCreateResponse(Try<TagActorResponse> responseTry) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    if (responseTry.get() instanceof TagActorResponse.Success s) {
      var status = s.created() ? StatusCodes.CREATED : StatusCodes.OK;
      return complete(status, s.tag(), Jackson.marshaller(objectMapper));
    }
    return mapResponse(responseTry, StatusCodes.OK);
  }

  private Route mapResponse(Try<TagActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case TagActorResponse.Success s ->
          complete(successStatus, s.tag(), Jackson.marshaller(objectMapper));
      case TagActorResponse.Items items ->
          complete(successStatus, items.tags(), Jackson.marshaller(objectMapper));
      case TagActorResponse.Deleted _ -> complete(StatusCodes.NO_CONTENT);
      case TagActorResponse.Failure err ->
          complete(
              StatusCodes.get(err.status()),
              createErrorResponse(StatusCodes.get(err.status()), err.message()),
              Jackson.marshaller(objectMapper));
    };
  }

  private Route completeNotFound(String message) {
    return complete(
        StatusCodes.NOT_FOUND,
        createErrorResponse(StatusCodes.NOT_FOUND, message),
        Jackson.marshaller(objectMapper));
  }

  private Route handleActorFailure(Throwable error) {
    logger.error("Tag actor request failed", error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        createErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private StudioErrorResponse createErrorResponse(StatusCode status, String message) {
    return new StudioErrorResponse(status.intValue(), message);
  }
}
