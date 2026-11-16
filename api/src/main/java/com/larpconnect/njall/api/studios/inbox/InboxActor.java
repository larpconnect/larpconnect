package com.larpconnect.njall.api.studios.inbox;

import static com.larpconnect.njall.api.studios.inbox.DefaultInboxActorFactory.createEnvelope;
import static com.larpconnect.njall.api.studios.inbox.DefaultInboxActorFactory.createItemError;
import static com.larpconnect.njall.api.studios.inbox.DefaultInboxActorFactory.createRecord;
import static com.larpconnect.njall.api.studios.inbox.DefaultInboxActorFactory.createSingleEnvelope;
import static com.larpconnect.njall.api.studios.inbox.DefaultInboxActorFactory.extractErrorMessage;
import static com.larpconnect.njall.api.studios.inbox.DefaultInboxActorFactory.replyBatchBadRequest;
import static com.larpconnect.njall.api.studios.inbox.DefaultInboxActorFactory.replyFailure;
import static com.larpconnect.njall.api.studios.inbox.DefaultInboxActorFactory.resolveFinalBatchResponse;
import static com.larpconnect.njall.api.studios.inbox.DefaultInboxActorFactory.toPublishError;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.dao.studios.actors.AgentDAO;
import com.larpconnect.njall.data.dao.studios.inbox.InboxActivityDAO;
import com.larpconnect.njall.queue.publisher.InboxActivityPublisher;
import com.larpconnect.njall.queue.publisher.InboxQueueEnvelope;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Receive;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Pekko Typed actor executing inbox validation, routing, deduplication, and AMQP queueing. */
final class InboxActor extends AbstractBehavior<InboxCommand> {

  private static final int MAX_BATCH_REQUESTS = 100;
  private static final Duration DEDUPLICATION_TTL = Duration.ofDays(8);
  private static final String NO_LOCAL_RECIPIENTS =
      "Activity contains no local recipients matching studio";
  private static final String NOT_ROUTABLE =
      "Candidate recipients are not routable under receiving actor";
  private static final InboxActorResponse SUCCESS_RESPONSE = new InboxActorResponse.Success();

  private final Logger logger = LoggerFactory.getLogger(InboxActor.class);
  private final AgentDAO agentDao;
  private final FeatherPubActivityValidator validator;
  private final LocalActorRecipientExtractor extractor;
  private final InboxActivityDAO inboxActivityDao;
  private final InboxDeduplicationCache deduplicationCache;
  private final InboxActivityPublisher publisher;
  private final Clock clock;

  InboxActor(
      ActorContext<InboxCommand> ctx,
      AgentDAO agentDao,
      FeatherPubActivityValidator validator,
      LocalActorRecipientExtractor extractor,
      InboxActivityDAO inboxDao,
      InboxDeduplicationCache dedupCache,
      InboxActivityPublisher publisher,
      Clock clock) {
    super(ctx);
    this.agentDao = agentDao;
    this.validator = validator;
    this.extractor = extractor;
    this.inboxActivityDao = inboxDao;
    this.deduplicationCache = dedupCache;
    this.publisher = publisher;
    this.clock = clock;
  }

  @Override
  public Receive<InboxCommand> createReceive() {
    return newReceiveBuilder()
        .onMessage(InboxCommand.DeliverActivity.class, this::onDeliverActivity)
        .onMessage(InboxCommand.DeliverBatch.class, this::onDeliverBatch)
        .onMessage(CompleteDelivery.class, this::onCompleteDelivery)
        .build();
  }

  private Behavior<InboxCommand> onDeliverActivity(InboxCommand.DeliverActivity cmd) {
    try {
      resolveActorId(cmd.tenantId(), cmd.actorIdentifier(), cmd.replyTo())
          .ifPresent(actorId -> evaluateAndPublishSingle(cmd, actorId));
    } catch (RuntimeException ex) {
      logger.error("Unexpected error in deliver activity: {}", ex.getMessage(), ex);
      replyFailure(cmd.replyTo(), 500, "Internal error processing activity");
    }
    return this;
  }

  private Optional<UUID> resolveActorId(
      UUID tenantId, String actorIdentifier, ActorRef<InboxActorResponse> replyTo) {
    var actorOpt = agentDao.findByIdentifier(tenantId, actorIdentifier);
    if (actorOpt.isEmpty()) {
      replyFailure(replyTo, 404, "Actor not found: " + actorIdentifier);
    }
    return actorOpt.map(actor -> actor.id());
  }

  private void evaluateAndPublishSingle(InboxCommand.DeliverActivity cmd, UUID actorId) {
    JsonNode act = cmd.activity();
    var err = validator.validate(act);
    if (err.isPresent()) {
      replyFailure(cmd.replyTo(), 400, err.orElseThrow());
      return;
    }
    var cands = extractor.extract(act, cmd.studioIdParam(), cmd.studioId(), cmd.lookup());
    if (cands.isEmpty()) {
      replyFailure(cmd.replyTo(), 400, NO_LOCAL_RECIPIENTS);
      return;
    }
    var routable = inboxActivityDao.resolveRoutableRecipients(cmd.tenantId(), actorId, cands);
    if (routable.isEmpty()) {
      replyFailure(cmd.replyTo(), 400, NOT_ROUTABLE);
      return;
    }
    publishSingleIfUnseen(cmd, actorId, routable);
  }

  private void publishSingleIfUnseen(
      InboxCommand.DeliverActivity cmd, UUID actorId, ImmutableSet<UUID> targets) {
    JsonNode act = cmd.activity();
    Instant now = clock.instant();
    var unseen =
        deduplicationCache.filterUnseen(cmd.tenantId(), act.path("id").asText(), targets, now);
    if (unseen.isEmpty()) {
      cmd.replyTo().tell(SUCCESS_RESPONSE);
      return;
    }
    var env = createSingleEnvelope(cmd, actorId, unseen, now);
    getContext()
        .pipeToSelf(publisher.publish(env), (_, ex) -> buildSingleDone(cmd, unseen, now, ex));
  }

  private CompleteDelivery buildSingleDone(
      InboxCommand.DeliverActivity cmd,
      ImmutableSet<UUID> unseen,
      Instant now,
      @Nullable Throwable error) {
    return complete(
        singleDone(cmd, unseen, now, toPublishError(logger, "activity to AMQP", error)));
  }

  private Behavior<InboxCommand> onDeliverBatch(InboxCommand.DeliverBatch cmd) {
    try {
      resolveActorId(cmd.tenantId(), cmd.actorIdentifier(), cmd.replyTo())
          .ifPresent(actorId -> processBatchRequest(cmd, actorId));
    } catch (RuntimeException ex) {
      logger.error("Unexpected error processing batch inbox request: {}", ex.getMessage(), ex);
      replyFailure(cmd.replyTo(), 503, "Error processing batch: " + extractErrorMessage(ex));
    }
    return this;
  }

  private void processBatchRequest(InboxCommand.DeliverBatch cmd, UUID actorId) {
    var requests = cmd.request().requests();
    if (requests.isEmpty() || requests.size() > MAX_BATCH_REQUESTS) {
      String msg =
          requests.isEmpty()
              ? "requests array must not be empty"
              : "requests array exceeds maximum allowed length of " + MAX_BATCH_REQUESTS;
      replyBatchBadRequest(cmd.replyTo(), msg, ImmutableMap.of());
      return;
    }
    var routable =
        resolveRoutableBatchItems(cmd.tenantId(), actorId, collectValidBatchItems(cmd, requests));
    if (routable.items().isEmpty()) {
      resolveFinalBatchResponse(cmd, 0, routable.errors());
      return;
    }
    var plan = buildPublishPlan(cmd, actorId, routable, clock.instant());
    if (plan.candidates().isEmpty()) {
      resolveFinalBatchResponse(cmd, plan.preDedupCount(), plan.errors());
      return;
    }
    dispatchBatchPublish(plan);
  }

  private BatchPass<String> collectValidBatchItems(
      InboxCommand.DeliverBatch cmd, ImmutableList<JsonNode> requests) {
    var items = ImmutableList.<ItemTargets<String>>builder();
    var errors = ImmutableSortedMap.<Integer, BatchInboxItemError>naturalOrder();
    for (int i = 0; i < requests.size(); i++) {
      JsonNode item = requests.get(i);
      var err = validator.validate(item);
      if (err.isPresent()) {
        errors.put(i, createItemError(400, err.orElseThrow()));
        continue;
      }
      var cands = extractor.extract(item, cmd.studioIdParam(), cmd.studioId(), cmd.lookup());
      if (cands.isEmpty()) {
        errors.put(i, createItemError(400, NO_LOCAL_RECIPIENTS));
      } else {
        items.add(itemTargets(i, item.path("id").asText(), cands));
      }
    }
    return batchPass(items.build(), errors.build());
  }

  private BatchPass<UUID> resolveRoutableBatchItems(
      UUID tenantId, UUID actorId, BatchPass<String> validPass) {
    if (validPass.items().isEmpty()) {
      return batchPass(ImmutableList.of(), validPass.errors());
    }
    var all =
        validPass.items().stream()
            .flatMap(v -> v.targets().stream())
            .collect(ImmutableSet.toImmutableSet());
    var routableMap = inboxActivityDao.resolveRoutableRecipientMap(tenantId, actorId, all);
    var routable = ImmutableList.<ItemTargets<UUID>>builder();
    var errs =
        ImmutableSortedMap.<Integer, BatchInboxItemError>naturalOrder().putAll(validPass.errors());
    for (var v : validPass.items()) {
      var targets =
          v.targets().stream()
              .map(id -> routableMap.get(id.toLowerCase(Locale.ROOT)))
              .filter(Objects::nonNull)
              .collect(ImmutableSet.toImmutableSet());
      if (targets.isEmpty()) {
        errs.put(v.index(), createItemError(400, NOT_ROUTABLE));
      } else {
        routable.add(itemTargets(v.index(), v.activityId(), targets));
      }
    }
    return batchPass(routable.build(), errs.build());
  }

  private PublishPlan buildPublishPlan(
      InboxCommand.DeliverBatch cmd, UUID actorId, BatchPass<UUID> routable, Instant now) {
    var req = ImmutableSetMultimap.<String, UUID>builder();
    routable.items().forEach(r -> req.putAll(r.activityId(), r.targets()));
    var unseenMap = deduplicationCache.filterUnseenBatch(cmd.tenantId(), req.build(), now);
    var owners = mapFirstTargetOwners(routable.items(), unseenMap);
    int preDedup = 0;
    var dependent = ImmutableList.<ItemTargets<UUID>>builder();
    var candidates = ImmutableList.<BatchCandidate>builder();
    for (var r : routable.items()) {
      var reqUnseen =
          r.targets().stream()
              .filter(unseenMap.get(r.activityId())::contains)
              .collect(ImmutableSet.toImmutableSet());
      if (reqUnseen.isEmpty()) {
        preDedup++;
        continue;
      }
      dependent.add(itemTargets(r.index(), r.activityId(), reqUnseen));
      var toPub =
          reqUnseen.stream()
              .filter(t -> Objects.equals(owners.get(targetKey(r.activityId(), t)), r.index()))
              .collect(ImmutableList.toImmutableList());
      if (!toPub.isEmpty()) {
        var env =
            createEnvelope(
                cmd.tenantId(),
                cmd.studioId(),
                actorId,
                toPub,
                now,
                cmd.request().requests().get(r.index()),
                cmd.traceparent());
        candidates.add(candidate(r.index(), r.activityId(), env));
      }
    }
    return publishPlan(
        cmd, now, preDedup, routable.errors(), dependent.build(), candidates.build());
  }

  private static ImmutableMap<TargetKey, Integer> mapFirstTargetOwners(
      ImmutableList<ItemTargets<UUID>> validItems, ImmutableSetMultimap<String, UUID> unseenMap) {
    return validItems.stream()
        .flatMap(
            r ->
                r.targets().stream()
                    .filter(unseenMap.get(r.activityId())::contains)
                    .map(t -> Map.entry(targetKey(r.activityId(), t), r.index())))
        .collect(
            ImmutableMap.toImmutableMap(
                Map.Entry::getKey, Map.Entry::getValue, (first, _) -> first));
  }

  private void dispatchBatchPublish(PublishPlan plan) {
    var futures = plan.candidates().stream().map(this::publishCandidate).toList();
    var all =
        CompletableFuture.allOf(futures.toArray(CompletableFuture<?>[]::new))
            .thenApply(_ -> futures.stream().map(CompletableFuture::join).toList());
    getContext().pipeToSelf(all, (res, ex) -> buildBatchDone(plan, res, ex));
  }

  private CompletableFuture<Optional<String>> publishCandidate(BatchCandidate candidate) {
    return publisher
        .publish(candidate.envelope())
        .handle((_, ex) -> toPublishError(logger, "batch item " + candidate.index(), ex));
  }

  private static CompleteDelivery buildBatchDone(
      PublishPlan plan, @Nullable List<Optional<String>> res, @Nullable Throwable err) {
    var ok = ImmutableList.<BatchCandidate>builder();
    var errs = ImmutableMap.<TargetKey, String>builder();
    String fallback = "Queue broker publish failed: " + extractErrorMessage(err);
    for (int i = 0; i < plan.candidates().size(); i++) {
      var c = plan.candidates().get(i);
      var errOpt = res != null ? res.get(i) : Optional.of(fallback);
      if (errOpt.isPresent()) {
        String msg = errOpt.orElseThrow();
        c.envelope().targetActorIds().forEach(t -> errs.put(targetKey(c.activityId(), t), msg));
      } else {
        ok.add(c);
      }
    }
    return complete(batchDone(plan, ok.build(), errs.buildKeepingLast()));
  }

  private Behavior<InboxCommand> onCompleteDelivery(CompleteDelivery cmd) {
    switch (cmd.completion()) {
      case DeliveryCompletion.Single single -> completeSingleDelivery(single);
      case DeliveryCompletion.Batch batch -> completeBatchDelivery(batch);
    }
    return this;
  }

  private void completeSingleDelivery(DeliveryCompletion.Single s) {
    if (s.err().isPresent()) {
      replyFailure(s.cmd().replyTo(), 503, s.err().orElseThrow());
      return;
    }
    try {
      String id = s.cmd().activity().path("id").asText();
      inboxActivityDao.recordAcceptedActivity(
          s.cmd().tenantId(), id, s.targets(), s.now(), s.now().plus(DEDUPLICATION_TTL));
      deduplicationCache.recordSeen(s.cmd().tenantId(), id, s.targets());
      s.cmd().replyTo().tell(SUCCESS_RESPONSE);
    } catch (RuntimeException ex) {
      String msg = extractErrorMessage(ex);
      logger.error("Failed to persist accepted activity: {}", msg, ex);
      replyFailure(s.cmd().replyTo(), 503, "Failed to persist accepted activity: " + msg);
    }
  }

  private void completeBatchDelivery(DeliveryCompletion.Batch batch) {
    var plan = batch.plan();
    Optional<String> dbErr =
        persistAcceptedCandidates(plan.cmd().tenantId(), plan.now(), batch.ok());
    var errors =
        ImmutableSortedMap.<Integer, BatchInboxItemError>naturalOrder().putAll(plan.errors());
    int okCount = plan.preDedupCount();
    for (var dep : plan.dependent()) {
      var missingErr = findMissingTargetError(dep, dbErr, batch.errs());
      if (missingErr.isEmpty()) {
        okCount++;
      } else {
        errors.put(dep.index(), createItemError(503, missingErr.orElseThrow()));
      }
    }
    resolveFinalBatchResponse(plan.cmd(), okCount, errors.build());
  }

  private Optional<String> persistAcceptedCandidates(
      UUID tenantId, Instant now, ImmutableList<BatchCandidate> accepted) {
    if (accepted.isEmpty()) {
      return Optional.empty();
    }
    Instant exp = now.plus(DEDUPLICATION_TTL);
    var records =
        accepted.stream()
            .flatMap(
                c ->
                    c.envelope().targetActorIds().stream()
                        .map(t -> createRecord(tenantId, t, c.activityId(), now, exp)))
            .collect(ImmutableList.toImmutableList());
    try {
      inboxActivityDao.recordAcceptedActivities(tenantId, records);
      accepted.forEach(
          c ->
              deduplicationCache.recordSeen(
                  tenantId, c.activityId(), ImmutableSet.copyOf(c.envelope().targetActorIds())));
      return Optional.empty();
    } catch (RuntimeException ex) {
      String msg = extractErrorMessage(ex);
      logger.error("Failed to persist accepted batch items: {}", msg, ex);
      return Optional.of("Failed to persist accepted activity: " + msg);
    }
  }

  private static Optional<String> findMissingTargetError(
      ItemTargets<UUID> dep, Optional<String> dbErr, ImmutableMap<TargetKey, String> pubErrors) {
    return dep.targets().stream()
        .map(t -> pubErrors.get(targetKey(dep.activityId(), t)))
        .filter(Objects::nonNull)
        .findFirst()
        .or(() -> dbErr);
  }

  private static CompleteDelivery complete(DeliveryCompletion completion) {
    return new CompleteDelivery(completion);
  }

  private static DeliveryCompletion.Single singleDone(
      InboxCommand.DeliverActivity cmd,
      ImmutableSet<UUID> targets,
      Instant now,
      Optional<String> err) {
    return new DeliveryCompletion.Single(cmd, targets, now, err);
  }

  private static DeliveryCompletion.Batch batchDone(
      PublishPlan plan, ImmutableList<BatchCandidate> ok, ImmutableMap<TargetKey, String> errs) {
    return new DeliveryCompletion.Batch(plan, ok, errs);
  }

  private static <T> ItemTargets<T> itemTargets(
      int index, String activityId, ImmutableSet<T> targets) {
    return new ItemTargets<>(index, activityId, targets);
  }

  private static <T> BatchPass<T> batchPass(
      ImmutableList<ItemTargets<T>> items, ImmutableMap<Integer, BatchInboxItemError> errors) {
    return new BatchPass<>(items, errors);
  }

  private static TargetKey targetKey(String activityId, UUID targetActorId) {
    return new TargetKey(activityId, targetActorId);
  }

  private static BatchCandidate candidate(int index, String activityId, InboxQueueEnvelope env) {
    return new BatchCandidate(index, activityId, env);
  }

  private static PublishPlan publishPlan(
      InboxCommand.DeliverBatch cmd,
      Instant now,
      int preDedup,
      ImmutableMap<Integer, BatchInboxItemError> errs,
      ImmutableList<ItemTargets<UUID>> dependent,
      ImmutableList<BatchCandidate> candidates) {
    return new PublishPlan(cmd, now, preDedup, errs, dependent, candidates);
  }

  @Immutable
  record CompleteDelivery(DeliveryCompletion completion) implements InboxCommand {}

  private sealed interface DeliveryCompletion {
    @Immutable
    record Single(
        InboxCommand.DeliverActivity cmd,
        ImmutableSet<UUID> targets,
        Instant now,
        Optional<String> err)
        implements DeliveryCompletion {}

    @Immutable
    record Batch(
        PublishPlan plan, ImmutableList<BatchCandidate> ok, ImmutableMap<TargetKey, String> errs)
        implements DeliveryCompletion {}
  }

  @Immutable
  private record TargetKey(String activityId, UUID targetActorId) {}

  @Immutable(containerOf = "T")
  private record ItemTargets<T>(int index, String activityId, ImmutableSet<T> targets) {}

  @Immutable(containerOf = "T")
  private record BatchPass<T>(
      ImmutableList<ItemTargets<T>> items, ImmutableMap<Integer, BatchInboxItemError> errors) {}

  @Immutable
  private record BatchCandidate(int index, String activityId, InboxQueueEnvelope envelope) {}

  @Immutable
  private record PublishPlan(
      InboxCommand.DeliverBatch cmd,
      Instant now,
      int preDedupCount,
      ImmutableMap<Integer, BatchInboxItemError> errors,
      ImmutableList<ItemTargets<UUID>> dependent,
      ImmutableList<BatchCandidate> candidates) {}
}
