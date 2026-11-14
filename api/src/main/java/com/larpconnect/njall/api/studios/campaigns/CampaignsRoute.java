package com.larpconnect.njall.api.studios.campaigns;

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

/** HTTP route handling tenanted studio campaigns. */
public final class CampaignsRoute extends AllDirectives implements RouteProvider {

  private static final ImmutableList<String> CREATE_REQUIRED_FIELDS =
      ImmutableList.of("generator", "title");
  private static final ImmutableList<String> PATCH_REQUIRED_FIELDS = ImmutableList.of("title");
  private static final ImmutableList<String> OPTIONAL_TEXT_FIELDS =
      ImmutableList.of("summary", "system");

  private final Logger logger = LoggerFactory.getLogger(CampaignsRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<CampaignCommand> campaignActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  CampaignsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<CampaignCommand> campaignActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioLookupCache, campaignActor, system, objectMapper, Duration.ofSeconds(20));
  }

  CampaignsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<CampaignCommand> campaignActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.campaignActor = campaignActor;
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
                        PathMatchers.separateOnSlashes("v1/campaigns"),
                        () -> handleCampaigns(studioIdParam))));
  }

  private Route handleCampaigns(String studioIdParam) {
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
                            get(() -> handleListCampaigns(lookup, studioIdParam)),
                            post(() -> handlePostCampaign(lookup, studioIdParam)))),
                pathPrefix(
                    PathMatchers.segment(),
                    campaignIdParam ->
                        handleCampaignBranch(lookup, studioIdParam, campaignIdParam))));
  }

  private RejectionHandler malformedRequestRejectionHandler() {
    return RejectionHandler.newBuilder()
        .handle(
            MalformedRequestContentRejection.class,
            rejection -> completeError(StatusCodes.BAD_REQUEST, rejection.message()))
        .build();
  }

  private Route handlePostCampaign(StudioLookup lookup, String studioIdParam) {
    return handleJsonBody(
        CreateCampaignRequest.class,
        CREATE_REQUIRED_FIELDS,
        false,
        request ->
            onComplete(
                () -> askCreateCampaign(lookup, studioIdParam, request),
                res -> mapCampaignResponse(res, StatusCodes.CREATED)));
  }

  private Route handleListCampaigns(StudioLookup lookup, String studioIdParam) {
    return parameterOptional(
        "systemId",
        systemId ->
            onComplete(
                () -> askListCampaigns(lookup, studioIdParam, systemId),
                res -> mapCampaignResponse(res, StatusCodes.OK)));
  }

  private Route handleCampaignBranch(
      StudioLookup lookup, String studioIdParam, String campaignIdParam) {
    return CampaignValidation.tryParseUuid(campaignIdParam)
        .map(campaignId -> buildCampaignSubroutes(lookup, studioIdParam, campaignId))
        .orElseGet(
            () -> completeError(StatusCodes.NOT_FOUND, "Campaign not found: " + campaignIdParam));
  }

  private Route buildCampaignSubroutes(StudioLookup lookup, String studioIdParam, UUID campaignId) {
    return pathEndOrSingleSlash(
        () ->
            concat(
                get(() -> handleGetCampaign(lookup.tenantId(), studioIdParam, campaignId)),
                patch(() -> handlePatchCampaign(lookup, studioIdParam, campaignId)),
                delete(() -> handleDeleteCampaign(lookup.tenantId(), campaignId))));
  }

  private Route handleGetCampaign(UUID tenantId, String studioIdParam, UUID campaignId) {
    return onComplete(
        () -> askGetCampaign(tenantId, studioIdParam, campaignId),
        res -> mapCampaignResponse(res, StatusCodes.OK));
  }

  private Route handlePatchCampaign(StudioLookup lookup, String studioIdParam, UUID campaignId) {
    return parameterOptional(
        "update_mask",
        mask ->
            handleJsonBody(
                UpdateCampaignRequest.class,
                PATCH_REQUIRED_FIELDS,
                UpdateMaskSupport.parseUpdateMask(mask).isPresent(),
                req ->
                    onComplete(
                        () -> askPatchCampaign(lookup, studioIdParam, campaignId, req, mask),
                        res -> mapCampaignResponse(res, StatusCodes.OK))));
  }

  private Route handleDeleteCampaign(UUID tenantId, UUID campaignId) {
    return onComplete(
        () -> askDeleteCampaign(tenantId, campaignId),
        res -> mapCampaignResponse(res, StatusCodes.NO_CONTENT));
  }

  private CompletionStage<CampaignActorResponse> askCreateCampaign(
      StudioLookup lookup, String studioIdentifier, CreateCampaignRequest request) {
    return ask(
        campaignActor,
        replyTo ->
            new CampaignCommand.CreateCampaign(
                lookup.tenantId(), studioIdentifier, request, replyTo, Optional.of(lookup)),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<CampaignActorResponse> askListCampaigns(
      StudioLookup lookup, String studioIdentifier, Optional<String> systemId) {
    return ask(
        campaignActor,
        replyTo ->
            new CampaignCommand.ListCampaigns(
                lookup.tenantId(), studioIdentifier, systemId, replyTo, Optional.of(lookup)),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<CampaignActorResponse> askGetCampaign(
      UUID tenantId, String studioIdentifier, UUID campaignId) {
    return ask(
        campaignActor,
        replyTo -> new CampaignCommand.GetCampaign(tenantId, studioIdentifier, campaignId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<CampaignActorResponse> askPatchCampaign(
      StudioLookup lookup,
      String studioIdentifier,
      UUID campaignId,
      UpdateCampaignRequest request,
      Optional<String> updateMask) {
    return ask(
        campaignActor,
        replyTo ->
            new CampaignCommand.PatchCampaign(
                lookup.tenantId(),
                studioIdentifier,
                campaignId,
                request,
                updateMask,
                replyTo,
                Optional.of(lookup)),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<CampaignActorResponse> askDeleteCampaign(UUID tenantId, UUID campaignId) {
    return ask(
        campaignActor,
        replyTo -> new CampaignCommand.DeleteCampaign(tenantId, campaignId, replyTo),
        askTimeout,
        system.scheduler());
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
        return Optional.of("Campaign " + field + " must be a string");
      }
    }
    for (var field : OPTIONAL_TEXT_FIELDS) {
      if (hasNonTextualOptionalField(body, field)) {
        return Optional.of("Campaign " + field + " must be a string or null");
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

  private Route mapCampaignResponse(
      Try<CampaignActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      return handleActorFailure("Campaign", responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case CampaignActorResponse.Success s ->
          complete(successStatus, s.campaign(), Jackson.marshaller(objectMapper));
      case CampaignActorResponse.Items i ->
          complete(StatusCodes.OK, i.campaigns(), Jackson.marshaller(objectMapper));
      case CampaignActorResponse.Deleted _ -> complete(StatusCodes.NO_CONTENT);
      case CampaignActorResponse.Failure f ->
          completeError(StatusCodes.get(f.status()), f.message());
    };
  }

  private Route completeError(StatusCode status, String message) {
    return complete(status, createErrorResponse(status, message), Jackson.marshaller(objectMapper));
  }

  private Route handleActorFailure(String entityType, Throwable error) {
    logger.error("{} actor request failed", entityType, error);
    return completeError(StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  private StudioErrorResponse createErrorResponse(StatusCode status, String message) {
    return new StudioErrorResponse(status.intValue(), message);
  }
}
