package com.larpconnect.njall.api.studios.inbox;

import static org.apache.pekko.actor.typed.javadsl.AskPattern.ask;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.larpconnect.njall.api.studios.common.StudioErrorResponse;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;

/** Default Pekko HTTP implementation of {@link InboxRoute}. */
final class DefaultInboxRoute extends AllDirectives implements InboxRoute {

  private static final String HEADER_CONTENT_DIGEST = "Content-Digest";
  private static final String HEADER_TRACEPARENT = "traceparent";

  private final Logger logger = LoggerFactory.getLogger(DefaultInboxRoute.class);
  private final ActorRef<InboxCommand> inboxActor;
  private final ContentDigestVerifier digestVerifier;
  private final ActorSystem<Void> system;
  private final ObjectMapper objectMapper;
  private final Duration askTimeout;

  @Inject
  DefaultInboxRoute(
      ActorRef<InboxCommand> inboxActor,
      ContentDigestVerifier digestVerifier,
      ActorSystem<Void> system,
      ObjectMapper objectMapper) {
    this(inboxActor, digestVerifier, system, objectMapper, Duration.ofSeconds(20));
  }

  DefaultInboxRoute(
      ActorRef<InboxCommand> inboxActor,
      ContentDigestVerifier digestVerifier,
      ActorSystem<Void> system,
      ObjectMapper objectMapper,
      Duration askTimeout) {
    this.inboxActor = inboxActor;
    this.digestVerifier = digestVerifier;
    this.system = system;
    this.objectMapper = objectMapper;
    this.askTimeout = askTimeout;
  }

  @Override
  public Route inboxSubroutes(StudioLookup lookup, String studioIdParam, String actorIdentifier) {
    return concat(
        path("inbox", () -> post(() -> handlePostSingle(lookup, studioIdParam, actorIdentifier))),
        path(
            "inbox:message",
            () -> post(() -> handlePostSingle(lookup, studioIdParam, actorIdentifier))),
        path(
            "inbox:batchMessage",
            () -> post(() -> handlePostBatch(lookup, studioIdParam, actorIdentifier))));
  }

  private Route handlePostSingle(
      StudioLookup lookup, String studioIdParam, String actorIdentifier) {
    return optionalHeaderValueByName(
        HEADER_CONTENT_DIGEST,
        digestOpt ->
            optionalHeaderValueByName(
                HEADER_TRACEPARENT,
                traceparentOpt ->
                    extractStrictEntity(
                        askTimeout,
                        strict ->
                            processSingleEntity(
                                lookup,
                                studioIdParam,
                                actorIdentifier,
                                strict.getData().toArray(),
                                digestOpt,
                                traceparentOpt))));
  }

  private Route processSingleEntity(
      StudioLookup lookup,
      String studioIdParam,
      String actorIdentifier,
      byte[] bytes,
      Optional<String> digestOpt,
      Optional<String> traceparentOpt) {
    var digestError = digestVerifier.verify(bytes, digestOpt);
    if (digestError.isPresent()) {
      return completeBadRequest(digestError.orElseThrow());
    }

    JsonNode activity;
    try {
      activity = objectMapper.readTree(bytes);
    } catch (IOException | RuntimeException ex) {
      return completeBadRequest("Invalid JSON request body");
    }
    if (activity == null || activity.isMissingNode()) {
      return completeBadRequest("Invalid JSON request body");
    }

    return onComplete(
        () -> askDeliverActivity(lookup, studioIdParam, actorIdentifier, activity, traceparentOpt),
        this::mapSingleResponse);
  }

  private Route handlePostBatch(StudioLookup lookup, String studioIdParam, String actorIdentifier) {
    return optionalHeaderValueByName(
        HEADER_CONTENT_DIGEST,
        digestOpt ->
            optionalHeaderValueByName(
                HEADER_TRACEPARENT,
                traceparentOpt ->
                    extractStrictEntity(
                        askTimeout,
                        strict ->
                            processBatchEntity(
                                lookup,
                                studioIdParam,
                                actorIdentifier,
                                strict.getData().toArray(),
                                digestOpt,
                                traceparentOpt))));
  }

  private Route processBatchEntity(
      StudioLookup lookup,
      String studioIdParam,
      String actorIdentifier,
      byte[] bytes,
      Optional<String> digestOpt,
      Optional<String> traceparentOpt) {
    var digestError = digestVerifier.verify(bytes, digestOpt);
    if (digestError.isPresent()) {
      return completeBatchBadRequest(digestError.orElseThrow());
    }

    BatchInboxActivitiesRequest request;
    try {
      JsonNode root = objectMapper.readTree(bytes);
      var wrapperError = validateBatchWrapper(root);
      if (wrapperError.isPresent()) {
        return completeBatchBadRequest(wrapperError.orElseThrow());
      }
      request =
          objectMapper
              .readerFor(BatchInboxActivitiesRequest.class)
              .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .readValue(root);
    } catch (IOException | RuntimeException ex) {
      return completeBatchBadRequest("Invalid batch JSON request body");
    }

    return onComplete(
        () -> askDeliverBatch(lookup, studioIdParam, actorIdentifier, request, traceparentOpt),
        this::mapBatchResponse);
  }

  private static Optional<String> validateBatchWrapper(@Nullable JsonNode root) {
    if (root == null || root.isMissingNode() || !root.isObject()) {
      return Optional.of("Invalid batch JSON request body: expected JSON object");
    }
    if (root.has("@context")) {
      return Optional.of("Batch request wrapper must not contain '@context'");
    }
    @Nullable JsonNode requestsNode = root.get("requests");
    if (requestsNode == null || !requestsNode.isArray()) {
      return Optional.of("Invalid batch JSON request body: 'requests' array is required");
    }
    return Optional.empty();
  }

  private CompletionStage<InboxActorResponse> askDeliverActivity(
      StudioLookup lookup,
      String studioIdParam,
      String actorIdentifier,
      JsonNode activity,
      Optional<String> traceparent) {
    var lookupOpt = Optional.of(lookup);
    return ask(
        inboxActor,
        replyTo ->
            createDeliverActivityCommand(
                lookup, studioIdParam, actorIdentifier, activity, traceparent, replyTo, lookupOpt),
        askTimeout,
        system.scheduler());
  }

  private CompletionStage<InboxActorResponse> askDeliverBatch(
      StudioLookup lookup,
      String studioIdParam,
      String actorIdentifier,
      BatchInboxActivitiesRequest request,
      Optional<String> traceparent) {
    var lookupOpt = Optional.of(lookup);
    return ask(
        inboxActor,
        replyTo ->
            createDeliverBatchCommand(
                lookup, studioIdParam, actorIdentifier, request, traceparent, replyTo, lookupOpt),
        askTimeout,
        system.scheduler());
  }

  private Route mapSingleResponse(Try<InboxActorResponse> responseTry) {
    if (responseTry.isFailure()) {
      return handleInternalFailure(responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case InboxActorResponse.Success _ ->
          complete(HttpResponse.create().withStatus(StatusCodes.ACCEPTED));
      case InboxActorResponse.Failure f -> mapFailureResponse(f);
      case InboxActorResponse.BatchSuccess _ ->
          handleInternalFailure(
              createUnexpectedResponseException(
                  "Unexpected BatchSuccess response for single inbox delivery"));
    };
  }

  private Route mapBatchResponse(Try<InboxActorResponse> responseTry) {
    if (responseTry.isFailure()) {
      return handleInternalFailure(responseTry.failed().get());
    }
    return switch (responseTry.get()) {
      case InboxActorResponse.BatchSuccess s ->
          complete(StatusCodes.ACCEPTED, s.response(), Jackson.marshaller(objectMapper));
      case InboxActorResponse.Failure f when f.batchError().isPresent() ->
          complete(
              StatusCodes.get(f.statusCode()),
              f.batchError().orElseThrow(),
              Jackson.marshaller(objectMapper));
      case InboxActorResponse.Failure f when f.statusCode() == 400 ->
          completeBatchBadRequest(f.message());
      case InboxActorResponse.Failure f -> mapFailureResponse(f);
      case InboxActorResponse.Success _ ->
          handleInternalFailure(
              createUnexpectedResponseException(
                  "Unexpected Success response for batch inbox delivery"));
    };
  }

  private Route mapFailureResponse(InboxActorResponse.Failure failure) {
    return switch (failure.statusCode()) {
      case 400 -> completeBadRequest(failure.message());
      case 404 -> completeNotFound(failure.message());
      case 503 -> completeServiceUnavailable(failure.message());
      default ->
          complete(
              StatusCodes.get(failure.statusCode()),
              createStudioError(failure.statusCode(), failure.message()),
              Jackson.marshaller(objectMapper));
    };
  }

  private Route completeNotFound(String message) {
    return complete(
        StatusCodes.NOT_FOUND,
        createStudioError(StatusCodes.NOT_FOUND.intValue(), message),
        Jackson.marshaller(objectMapper));
  }

  private Route completeBadRequest(String message) {
    return complete(
        StatusCodes.BAD_REQUEST,
        createStudioError(StatusCodes.BAD_REQUEST.intValue(), message),
        Jackson.marshaller(objectMapper));
  }

  private Route completeBatchBadRequest(String message) {
    return complete(
        StatusCodes.BAD_REQUEST,
        createBatchError(StatusCodes.BAD_REQUEST.intValue(), message),
        Jackson.marshaller(objectMapper));
  }

  private Route completeServiceUnavailable(String message) {
    return complete(
        StatusCodes.SERVICE_UNAVAILABLE,
        createStudioError(StatusCodes.SERVICE_UNAVAILABLE.intValue(), message),
        Jackson.marshaller(objectMapper));
  }

  private Route handleInternalFailure(Throwable error) {
    logger.error("Inbox route ask failure", error);
    return complete(
        StatusCodes.INTERNAL_SERVER_ERROR,
        createStudioError(StatusCodes.INTERNAL_SERVER_ERROR.intValue(), "Internal server error"),
        Jackson.marshaller(objectMapper));
  }

  private static InboxCommand.DeliverActivity createDeliverActivityCommand(
      StudioLookup lookup,
      String studioIdParam,
      String actorIdentifier,
      JsonNode activity,
      Optional<String> traceparent,
      ActorRef<InboxActorResponse> replyTo,
      Optional<StudioLookup> lookupOpt) {
    return new InboxCommand.DeliverActivity(
        lookup.tenantId(),
        lookup.studioId(),
        studioIdParam,
        actorIdentifier,
        activity,
        traceparent,
        replyTo,
        lookupOpt);
  }

  private static InboxCommand.DeliverBatch createDeliverBatchCommand(
      StudioLookup lookup,
      String studioIdParam,
      String actorIdentifier,
      BatchInboxActivitiesRequest request,
      Optional<String> traceparent,
      ActorRef<InboxActorResponse> replyTo,
      Optional<StudioLookup> lookupOpt) {
    return new InboxCommand.DeliverBatch(
        lookup.tenantId(),
        lookup.studioId(),
        studioIdParam,
        actorIdentifier,
        request,
        traceparent,
        replyTo,
        lookupOpt);
  }

  private static StudioErrorResponse createStudioError(int code, String message) {
    return new StudioErrorResponse(code, message);
  }

  private static BatchInboxErrorResponse createBatchError(int code, String message) {
    return new BatchInboxErrorResponse(code, message, ImmutableMap.of());
  }

  private static IllegalStateException createUnexpectedResponseException(String message) {
    return new IllegalStateException(message);
  }
}
