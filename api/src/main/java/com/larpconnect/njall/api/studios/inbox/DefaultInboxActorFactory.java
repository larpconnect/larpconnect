package com.larpconnect.njall.api.studios.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.actors.AgentDAO;
import com.larpconnect.njall.data.dao.studios.inbox.InboxActivityDAO;
import com.larpconnect.njall.data.domain.inbox.InboxActivityRecord;
import com.larpconnect.njall.queue.publisher.InboxActivityPublisher;
import com.larpconnect.njall.queue.publisher.InboxQueueEnvelope;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/** Default Guice-injected factory producing {@link InboxActor} behaviors and protocol payloads. */
final class DefaultInboxActorFactory implements InboxActorFactory {

  private final AgentDAO agentDao;
  private final FeatherPubActivityValidator validator;
  private final LocalActorRecipientExtractor extractor;
  private final InboxActivityDAO inboxActivityDao;
  private final InboxDeduplicationCache deduplicationCache;
  private final InboxActivityPublisher publisher;
  private final Clock clock;

  @Inject
  DefaultInboxActorFactory(
      AgentDAO agentDao,
      FeatherPubActivityValidator validator,
      LocalActorRecipientExtractor extractor,
      InboxActivityDAO inboxActivityDao,
      InboxDeduplicationCache deduplicationCache,
      InboxActivityPublisher publisher,
      Clock clock) {
    this.agentDao = agentDao;
    this.validator = validator;
    this.extractor = extractor;
    this.inboxActivityDao = inboxActivityDao;
    this.deduplicationCache = deduplicationCache;
    this.publisher = publisher;
    this.clock = clock;
  }

  @Override
  public Behavior<InboxCommand> create() {
    return Behaviors.setup(this::createActor);
  }

  private InboxActor createActor(ActorContext<InboxCommand> context) {
    return new InboxActor(
        context,
        agentDao,
        validator,
        extractor,
        inboxActivityDao,
        deduplicationCache,
        publisher,
        clock);
  }

  static InboxQueueEnvelope createSingleEnvelope(
      InboxCommand.DeliverActivity cmd, UUID actorId, ImmutableSet<UUID> unseen, Instant now) {
    return createEnvelope(
        cmd.tenantId(),
        cmd.studioId(),
        actorId,
        ImmutableList.copyOf(unseen),
        now,
        cmd.activity(),
        cmd.traceparent());
  }

  static InboxQueueEnvelope createEnvelope(
      UUID tenantId,
      UUID studioId,
      UUID actorId,
      ImmutableList<UUID> targets,
      Instant now,
      JsonNode activity,
      Optional<String> traceparent) {
    return new InboxQueueEnvelope(tenantId, studioId, actorId, targets, now, activity, traceparent);
  }

  static InboxActivityRecord createRecord(
      UUID tenantId, UUID targetActorId, String activityId, Instant receivedOn, Instant expiresOn) {
    return new InboxActivityRecord(tenantId, targetActorId, activityId, receivedOn, expiresOn);
  }

  static BatchInboxItemError createItemError(int code, String message) {
    return new BatchInboxItemError(code, message);
  }

  static BatchInboxActivitiesResponse createBatchResponse(
      ImmutableMap<String, BatchInboxItemError> failedRequests) {
    return new BatchInboxActivitiesResponse(failedRequests);
  }

  static InboxActorResponse.BatchSuccess createBatchSuccess(BatchInboxActivitiesResponse response) {
    return new InboxActorResponse.BatchSuccess(response);
  }

  static InboxActorResponse.Failure createFailure(int code, String message) {
    return new InboxActorResponse.Failure(code, message);
  }

  static BatchInboxErrorResponse createBatchErrorResponse(
      String message, ImmutableMap<String, BatchInboxItemError> failedRequests) {
    return new BatchInboxErrorResponse(400, message, failedRequests);
  }

  static InboxActorResponse.Failure createBatchFailure(
      String message, Optional<BatchInboxErrorResponse> batchError) {
    return new InboxActorResponse.Failure(400, message, batchError);
  }

  static void resolveFinalBatchResponse(
      InboxCommand.DeliverBatch cmd,
      int okCount,
      ImmutableMap<Integer, BatchInboxItemError> failed) {
    var sorted =
        failed.entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue));
    if (okCount > 0) {
      cmd.replyTo().tell(createBatchSuccess(createBatchResponse(sorted)));
    } else if (failed.values().stream().anyMatch(e -> e.code() >= 500)) {
      replyFailure(
          cmd.replyTo(), 503, "All batch publish attempts failed due to server or queue errors");
    } else {
      replyBatchBadRequest(cmd.replyTo(), "All batch items failed validation or routing", sorted);
    }
  }

  static void replyFailure(ActorRef<InboxActorResponse> replyTo, int code, String msg) {
    replyTo.tell(createFailure(code, msg));
  }

  static void replyBatchBadRequest(
      ActorRef<InboxActorResponse> replyTo,
      String msg,
      ImmutableMap<String, BatchInboxItemError> errs) {
    replyTo.tell(createBatchFailure(msg, Optional.of(createBatchErrorResponse(msg, errs))));
  }

  static Optional<String> toPublishError(
      Logger logger, String targetLabel, @Nullable Throwable error) {
    if (error == null) {
      return Optional.empty();
    }
    String msg = extractErrorMessage(error);
    logger.error("Failed to publish {}: {}", targetLabel, msg, error);
    return Optional.of("Queue broker publish failed: " + msg);
  }

  static String extractErrorMessage(@Nullable Throwable error) {
    Throwable c =
        error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    return c == null
        ? "Unknown error"
        : (c.getMessage() != null ? c.getMessage() : c.getClass().getSimpleName());
  }
}
