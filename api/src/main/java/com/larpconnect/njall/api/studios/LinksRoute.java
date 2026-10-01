package com.larpconnect.njall.api.studios;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.larpconnect.njall.api.http.RouteProvider;
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

/** HTTP route handling tenanted external studio link management. */
public final class LinksRoute extends AllDirectives implements RouteProvider {

  private final Logger logger = LoggerFactory.getLogger(LinksRoute.class);

  private final StudioLookupCache studioLookupCache;
  private final ActorRef<LinkCommand> linkActor;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  LinksRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<LinkCommand> linkActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(studioLookupCache, linkActor, system, objectMapper, Duration.ofSeconds(20));
  }

  LinksRoute(
      StudioLookupCache studioLookupCache,
      ActorRef<LinkCommand> linkActor,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.studioLookupCache = studioLookupCache;
    this.linkActor = linkActor;
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
                        PathMatchers.separateOnSlashes("v1/links"),
                        () -> handleLinks(studioIdParam))));
  }

  private Route handleLinks(String studioIdParam) {
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
        pathEndOrSingleSlash(() -> post(() -> handlePost(lookup))),
        path(PathMatchers.segment(), linkIdParam -> handleLinkItem(lookup, linkIdParam)));
  }

  private Route handlePost(StudioLookup lookup) {
    return entity(
        Jackson.unmarshaller(objectMapper, CreateLinkRequest.class),
        request ->
            onComplete(
                () -> askCreateLink(lookup.tenantId(), request),
                res -> mapResponse(res, StatusCodes.CREATED)));
  }

  private Route handleLinkItem(StudioLookup lookup, String linkIdParam) {
    return LinkValidation.tryParseUuid(linkIdParam)
        .map(linkId -> buildItemRoutes(lookup.tenantId(), linkId))
        .orElseGet(() -> completeNotFound("Link not found: " + linkIdParam));
  }

  private Route buildItemRoutes(UUID tenantId, UUID linkId) {
    return concat(
        get(
            () ->
                onComplete(
                    () -> askGetLink(tenantId, linkId), res -> mapResponse(res, StatusCodes.OK))),
        patch(
            () ->
                parameterOptional(
                    "update_mask",
                    mask ->
                        entity(
                            Jackson.unmarshaller(objectMapper, UpdateLinkRequest.class),
                            req ->
                                onComplete(
                                    () -> askPatchLink(tenantId, linkId, req, mask),
                                    res -> mapResponse(res, StatusCodes.OK))))),
        delete(
            () ->
                onComplete(
                    () -> askDeleteLink(tenantId, linkId),
                    res -> mapResponse(res, StatusCodes.NO_CONTENT))));
  }

  private CompletionStage<LinkActorResponse> askCreateLink(
      UUID tenantId, CreateLinkRequest request) {
    return ask(
        linkActor,
        replyTo -> new LinkCommand.CreateLink(tenantId, request, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<LinkActorResponse> askGetLink(UUID tenantId, UUID linkId) {
    return ask(
        linkActor,
        replyTo -> new LinkCommand.GetLink(tenantId, linkId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<LinkActorResponse> askPatchLink(
      UUID tenantId, UUID linkId, UpdateLinkRequest request, Optional<String> updateMask) {
    return ask(
        linkActor,
        replyTo -> new LinkCommand.PatchLink(tenantId, linkId, request, updateMask, replyTo),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<LinkActorResponse> askDeleteLink(UUID tenantId, UUID linkId) {
    return ask(
        linkActor,
        replyTo -> new LinkCommand.DeleteLink(tenantId, linkId, replyTo),
        askTimeout,
        system.scheduler());
  }

  private Route mapResponse(Try<LinkActorResponse> responseTry, StatusCode successStatus) {
    if (responseTry.isFailure()) {
      return handleActorFailure(responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case LinkActorResponse.Success s ->
          complete(successStatus, s.link(), Jackson.marshaller(objectMapper));
      case LinkActorResponse.Deleted _ -> complete(StatusCodes.NO_CONTENT);
      case LinkActorResponse.BadRequest br ->
          complete(
              StatusCodes.BAD_REQUEST,
              createErrorResponse(StatusCodes.BAD_REQUEST, br.message()),
              Jackson.marshaller(objectMapper));
      case LinkActorResponse.NotFound nf ->
          complete(
              StatusCodes.NOT_FOUND,
              createErrorResponse(StatusCodes.NOT_FOUND, nf.message()),
              Jackson.marshaller(objectMapper));
      case LinkActorResponse.Failure f ->
          complete(
              StatusCodes.INTERNAL_SERVER_ERROR,
              createErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, f.message()),
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
    logger.error("Link actor request failed", error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        createErrorResponse(StatusCodes.INTERNAL_SERVER_ERROR, "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private StudioErrorResponse createErrorResponse(StatusCode status, String message) {
    return new StudioErrorResponse(status.intValue(), message);
  }
}
