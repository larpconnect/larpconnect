package com.larpconnect.njall.api.studios.individuals;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

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

/** HTTP route handling tenanted studio individuals. */
public final class IndividualsRoute extends AllDirectives implements RouteProvider {

  private final Logger logger = LoggerFactory.getLogger(IndividualsRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<IndividualCommand> individualActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  IndividualsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<IndividualCommand> individualActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioLookupCache, individualActor, system, objectMapper, Duration.ofSeconds(20));
  }

  IndividualsRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<IndividualCommand> individualActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.individualActor = individualActor;
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
                        PathMatchers.separateOnSlashes("v1/individuals"),
                        () -> handleIndividuals(studioIdParam))));
  }

  private Route handleIndividuals(String studioIdParam) {
    return studioLookupCache
        .findByIdOrAlias(studioIdParam)
        .filter(this::isActive)
        .map(this::buildTenantedRoutes)
        .orElseGet(() -> completeNotFound("Studio not found: " + studioIdParam));
  }

  private boolean isActive(StudioLookup lookup) {
    return !lookup.isDeleted();
  }

  private Route buildTenantedRoutes(StudioLookup lookup) {
    return concat(
        pathEndOrSingleSlash(
            () ->
                concat(
                    post(() -> handlePostIndividual(lookup.tenantId())),
                    get(() -> completeNotFound("Listing individuals is not supported")))),
        pathPrefix(
            PathMatchers.segment(),
            individualIdParam -> handleIndividualBranch(lookup.tenantId(), individualIdParam)));
  }

  private Route handlePostIndividual(UUID tenantId) {
    return entity(
        Jackson.unmarshaller(objectMapper, CreateIndividualRequest.class),
        request ->
            onComplete(
                () -> askCreateIndividual(tenantId, request),
                res -> mapIndividualResponse(res, StatusCodes.CREATED)));
  }

  private Route handleIndividualBranch(UUID tenantId, String individualIdParam) {
    return IndividualValidation.tryParseUuid(individualIdParam)
        .map(individualId -> buildIndividualSubroutes(tenantId, individualId))
        .orElseGet(() -> completeNotFound("Individual not found: " + individualIdParam));
  }

  private Route buildIndividualSubroutes(UUID tenantId, UUID individualId) {
    return pathEndOrSingleSlash(
        () ->
            concat(
                get(() -> handleGetIndividual(tenantId, individualId)),
                patch(() -> handlePatchIndividual(tenantId, individualId)),
                delete(() -> handleDeleteIndividual(tenantId, individualId))));
  }

  private Route handleGetIndividual(UUID tenantId, UUID individualId) {
    return onComplete(
        () -> askGetIndividual(tenantId, individualId),
        res -> mapIndividualResponse(res, StatusCodes.OK));
  }

  private Route handlePatchIndividual(UUID tenantId, UUID individualId) {
    return parameterOptional(
        "update_mask",
        mask ->
            entity(
                Jackson.unmarshaller(objectMapper, UpdateIndividualRequest.class),
                req ->
                    onComplete(
                        () -> askPatchIndividual(tenantId, individualId, req, mask),
                        res -> mapIndividualResponse(res, StatusCodes.OK))));
  }

  private Route handleDeleteIndividual(UUID tenantId, UUID individualId) {
    return onComplete(
        () -> askDeleteIndividual(tenantId, individualId),
        res -> mapIndividualResponse(res, StatusCodes.NO_CONTENT));
  }

  private CompletionStage<IndividualActorResponse> askCreateIndividual(
      UUID tenantId, CreateIndividualRequest request) {
    return ask(
        individualActor,
        replyTo -> new IndividualCommand.CreateIndividual(tenantId, request, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<IndividualActorResponse> askGetIndividual(
      UUID tenantId, UUID individualId) {
    return ask(
        individualActor,
        replyTo -> new IndividualCommand.GetIndividual(tenantId, individualId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<IndividualActorResponse> askPatchIndividual(
      UUID tenantId,
      UUID individualId,
      UpdateIndividualRequest request,
      Optional<String> updateMask) {
    return ask(
        individualActor,
        replyTo ->
            new IndividualCommand.PatchIndividual(
                tenantId, individualId, request, updateMask, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<IndividualActorResponse> askDeleteIndividual(
      UUID tenantId, UUID individualId) {
    return ask(
        individualActor,
        replyTo -> new IndividualCommand.DeleteIndividual(tenantId, individualId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapIndividualResponse(
      Try<IndividualActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      return handleActorFailure("Individual", responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case IndividualActorResponse.Success s ->
          complete(successStatus, s.individual(), Jackson.marshaller(objectMapper));
      case IndividualActorResponse.Deleted _ -> complete(StatusCodes.NO_CONTENT);
      case IndividualActorResponse.Failure f -> {
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
