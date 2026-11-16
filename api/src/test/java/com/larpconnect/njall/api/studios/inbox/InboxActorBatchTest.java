package com.larpconnect.njall.api.studios.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.larpconnect.njall.common.config.ServerConfig;
import com.larpconnect.njall.data.dao.studios.actors.AgentDAO;
import com.larpconnect.njall.data.dao.studios.inbox.InboxActivityDAO;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.Agent;
import com.larpconnect.njall.queue.publisher.InboxActivityPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.IntStream;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class InboxActorBatchTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-11-14T12:00:00Z");

  private ObjectMapper mapper;
  private AgentDAO agentDao;
  private FeatherPubActivityValidator validator;
  private LocalActorRecipientExtractor extractor;
  private InboxActivityDAO inboxActivityDao;
  private InboxDeduplicationCache cache;
  private InboxActivityPublisher publisher;
  private Clock clock;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID receivingActorId = UUID.randomUUID();
  private final UUID targetActorId = UUID.randomUUID();
  private final StudioLookup lookup =
      new StudioLookup(tenantId, studioId, "valiant", FIXED_NOW, FIXED_NOW, Optional.empty());

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    agentDao = mock(AgentDAO.class);
    validator = new DefaultFeatherPubActivityValidator(clock);
    extractor = new DefaultLocalActorRecipientExtractor(new ServerConfig("127.0.0.1", 0));
    inboxActivityDao = mock(InboxActivityDAO.class);
    cache = mock(InboxDeduplicationCache.class);
    publisher = mock(InboxActivityPublisher.class);

    when(agentDao.findByIdentifier(tenantId, receivingActorId.toString()))
        .thenReturn(Optional.of(sampleAgent()));
  }

  private Agent sampleAgent() {
    return new Agent(
        receivingActorId,
        receivingActorId,
        "valkyrie",
        Optional.empty(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        ImmutableMap.of(),
        FIXED_NOW,
        FIXED_NOW,
        Optional.empty());
  }

  private BehaviorTestKit<InboxCommand> createTestKit() {
    var factory =
        new DefaultInboxActorFactory(
            agentDao, validator, extractor, inboxActivityDao, cache, publisher, clock);
    return BehaviorTestKit.create(factory.create());
  }

  private InboxCommand.DeliverBatch batchCmd(
      List<JsonNode> items, ActorRef<InboxActorResponse> replyTo) {
    return new InboxCommand.DeliverBatch(
        tenantId,
        studioId,
        "valiant",
        receivingActorId.toString(),
        new BatchInboxActivitiesRequest(items),
        Optional.empty(),
        replyTo,
        Optional.of(lookup));
  }

  private static void runToCompletion(BehaviorTestKit<InboxCommand> testKit, InboxCommand cmd) {
    testKit.run(cmd);
    while (testKit.selfInbox().hasMessages()) {
      testKit.runOne();
    }
  }

  private ObjectNode createValidActivity(String activityId) {
    ObjectNode node = mapper.createObjectNode();
    ArrayNode ctx = node.putArray("@context");
    ctx.add("https://larpconnect.app/schemas/featherpub");
    node.put("id", activityId);
    node.put("type", "Create");
    node.put("actor", "https://larpconnect.app/api/studios/valiant/v1/actors/master");
    node.put("created", FIXED_NOW.toString());
    node.putArray("to").add("/api/studios/valiant/v1/actors/galahad");
    node.set("object", mapper.createObjectNode().put("name", "Quest"));
    return node;
  }

  @Test
  @DisplayName("DeliverBatch returns 404 when receiving actor does not exist")
  void deliverBatch_missingActor_returns404() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    when(agentDao.findByIdentifier(tenantId, "ghost")).thenReturn(Optional.empty());

    runToCompletion(
        testKit,
        new InboxCommand.DeliverBatch(
            tenantId,
            studioId,
            "valiant",
            "ghost",
            new BatchInboxActivitiesRequest(ImmutableList.of()),
            Optional.empty(),
            inbox.getRef(),
            Optional.empty()));

    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(404);
    verify(publisher, never()).publish(any());
  }

  @Test
  @DisplayName("DeliverBatch rejects empty requests array with 400")
  void deliverBatch_emptyRequests_returns400() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();

    runToCompletion(testKit, batchCmd(ImmutableList.of(), inbox.getRef()));

    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(400);
  }

  @Test
  @DisplayName("DeliverBatch rejects requests array exceeding 100 items with 400")
  void deliverBatch_exceedsMaxLength_returns400() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    var overLimit =
        IntStream.range(0, 101)
            .<JsonNode>mapToObj(_ -> mapper.createObjectNode())
            .collect(ImmutableList.toImmutableList());

    runToCompletion(testKit, batchCmd(overLimit, inbox.getRef()));

    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(400);
  }

  @Test
  @DisplayName("DeliverBatch deduplicates intra-batch targets into single-pass queries and publish")
  void deliverBatch_intraBatchDuplicate_executesSinglePassQueriesAndPublish() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    String actId = "https://larpconnect.app/act/single-pass";
    ObjectNode validAct = createValidActivity(actId);
    ObjectNode duplicateAct = createValidActivity(actId);

    when(inboxActivityDao.resolveRoutableRecipientMap(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableMap.of("galahad", targetActorId));
    when(cache.filterUnseenBatch(eq(tenantId), any(), any()))
        .thenReturn(ImmutableSetMultimap.of(actId, targetActorId));
    when(publisher.publish(any())).thenReturn(CompletableFuture.completedFuture(null));

    runToCompletion(testKit, batchCmd(ImmutableList.of(validAct, duplicateAct), inbox.getRef()));

    var batchRes = (InboxActorResponse.BatchSuccess) inbox.receiveMessage();
    assertThat(batchRes.response().failedRequests()).isEmpty();
    verify(inboxActivityDao, times(1))
        .resolveRoutableRecipientMap(eq(tenantId), eq(receivingActorId), any());
    verify(cache, times(1)).filterUnseenBatch(eq(tenantId), any(), any());
    verify(publisher, times(1)).publish(any());
    verify(inboxActivityDao, times(1)).recordAcceptedActivities(eq(tenantId), any());
  }

  @Test
  @DisplayName("DeliverBatch sorts multi-digit error keys numerically")
  void deliverBatch_elevenInvalidItems_sortsErrorKeysNumerically() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    var elevenInvalid =
        IntStream.range(0, 11)
            .<JsonNode>mapToObj(_ -> mapper.createObjectNode())
            .collect(ImmutableList.toImmutableList());

    runToCompletion(testKit, batchCmd(elevenInvalid, inbox.getRef()));

    var allInvalid = (InboxActorResponse.Failure) inbox.receiveMessage();
    assertThat(allInvalid.batchError().orElseThrow().failedRequests().keySet())
        .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
  }

  @Test
  @DisplayName("DeliverBatch marks newly published item as 503 when DAO batch persistence fails")
  void deliverBatch_daoFailureWithPreDedupedItem_returnsPartial202With503Entry() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    String dedupId = "https://larpconnect.app/act/dedup-ok";
    String failId = "https://larpconnect.app/act/dao-fail";

    when(inboxActivityDao.resolveRoutableRecipientMap(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableMap.of("galahad", targetActorId));
    when(cache.filterUnseenBatch(eq(tenantId), any(), any()))
        .thenReturn(ImmutableSetMultimap.of(failId, targetActorId));
    when(publisher.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    doThrow(new IllegalStateException("Transient DB error"))
        .when(inboxActivityDao)
        .recordAcceptedActivities(eq(tenantId), any());

    runToCompletion(
        testKit,
        batchCmd(
            ImmutableList.of(createValidActivity(dedupId), createValidActivity(failId)),
            inbox.getRef()));

    var partial = (InboxActorResponse.BatchSuccess) inbox.receiveMessage();
    assertThat(partial.response().failedRequests()).containsOnlyKeys("1");
    assertThat(partial.response().failedRequests().get("1").code()).isEqualTo(503);
  }

  @Test
  @DisplayName("DeliverBatch returns top-level 503 when all items fail DAO persistence")
  void deliverBatch_allItemsFailDaoPersistence_returns503() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    String failId = "https://larpconnect.app/act/dao-fail";

    when(inboxActivityDao.resolveRoutableRecipientMap(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableMap.of("galahad", targetActorId));
    when(cache.filterUnseenBatch(eq(tenantId), any(), any()))
        .thenReturn(ImmutableSetMultimap.of(failId, targetActorId));
    when(publisher.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    doThrow(new IllegalStateException("Transient DB error"))
        .when(inboxActivityDao)
        .recordAcceptedActivities(eq(tenantId), any());

    runToCompletion(
        testKit, batchCmd(ImmutableList.of(createValidActivity(failId)), inbox.getRef()));

    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(503);
  }

  @Test
  @DisplayName("DeliverBatch returns 400 when all valid items have unroutable recipients")
  void deliverBatch_allItemsUnroutable_returns400() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    ObjectNode unroutable = createValidActivity("https://larpconnect.app/act/unroutable");
    unroutable.putArray("to").add("/api/studios/valiant/v1/actors/outsider");
    when(inboxActivityDao.resolveRoutableRecipientMap(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableMap.of());

    runToCompletion(testKit, batchCmd(ImmutableList.of(unroutable), inbox.getRef()));

    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(400);
  }

  @Test
  @DisplayName("DeliverBatch succeeds with failedRequests map when pre-deduped mixed with invalid")
  void deliverBatch_preDedupedWithForeignAndUnroutable_returnsPartialSuccess() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    ObjectNode deduped = createValidActivity("https://larpconnect.app/act/deduped");
    ObjectNode foreign = createValidActivity("https://larpconnect.app/act/foreign");
    foreign.putArray("to").add("https://larpconnect.org/api/studios/other/v1/actors/someone");
    ObjectNode unroutable = createValidActivity("https://larpconnect.app/act/unroutable");
    unroutable.putArray("to").add("/api/studios/valiant/v1/actors/outsider");

    when(inboxActivityDao.resolveRoutableRecipientMap(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableMap.of("galahad", targetActorId));
    when(cache.filterUnseenBatch(eq(tenantId), any(), any())).thenReturn(ImmutableSetMultimap.of());

    runToCompletion(
        testKit, batchCmd(ImmutableList.of(deduped, foreign, unroutable), inbox.getRef()));

    var batchSuccess = (InboxActorResponse.BatchSuccess) inbox.receiveMessage();
    assertThat(batchSuccess.response().failedRequests()).containsOnlyKeys("1", "2");
  }

  @Test
  @DisplayName("DeliverBatch fails all duplicate unseen items with 503 when broker publish fails")
  void deliverBatch_duplicateUnseenBrokerFailure_marksAllDuplicatesFailed() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    String dupId = "https://larpconnect.app/act/dup-unseen";
    String preSeenId = "https://larpconnect.app/act/pre-seen";
    ObjectNode dup0 = createValidActivity(dupId);
    ObjectNode dup1 = createValidActivity(dupId);
    ObjectNode preSeen = createValidActivity(preSeenId);

    when(inboxActivityDao.resolveRoutableRecipientMap(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableMap.of("galahad", targetActorId));
    when(cache.filterUnseenBatch(eq(tenantId), any(), any()))
        .thenReturn(ImmutableSetMultimap.of(dupId, targetActorId));
    when(publisher.publish(any()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Queue down")));

    runToCompletion(testKit, batchCmd(ImmutableList.of(dup0, dup1), inbox.getRef()));
    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(503);

    runToCompletion(testKit, batchCmd(ImmutableList.of(preSeen, dup0, dup1), inbox.getRef()));
    var partial = (InboxActorResponse.BatchSuccess) inbox.receiveMessage();
    assertThat(partial.response().failedRequests()).containsOnlyKeys("1", "2");
    assertThat(partial.response().failedRequests().get("1").code()).isEqualTo(503);
    assertThat(partial.response().failedRequests().get("2").code()).isEqualTo(503);
  }

  @Test
  @DisplayName("DeliverBatch returns 503 when actor lookup throws unexpected RuntimeException")
  void deliverBatch_actorDaoThrows_returns503() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    when(agentDao.findByIdentifier(tenantId, receivingActorId.toString()))
        .thenThrow(new IllegalStateException("DB down"));

    runToCompletion(
        testKit,
        batchCmd(
            ImmutableList.of(createValidActivity("https://larpconnect.app/act/1")),
            inbox.getRef()));

    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(503);
  }

  @Test
  @DisplayName("DeliverBatch records 503 failedRequests for pending items on unexpected error")
  @SuppressWarnings("unchecked")
  void deliverBatch_unexpectedCompletionFailure_recordsFailedItems() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    String dedupId = "https://larpconnect.app/act/dedup-0";
    String pendingId = "https://larpconnect.app/act/pending-1";

    when(inboxActivityDao.resolveRoutableRecipientMap(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableMap.of("galahad", targetActorId));
    when(cache.filterUnseenBatch(eq(tenantId), any(), any()))
        .thenReturn(ImmutableSetMultimap.of(pendingId, targetActorId));

    CompletableFuture<Void> brokenStage = mock(CompletableFuture.class);
    when(brokenStage.handle(any()))
        .thenReturn(
            CompletableFuture.failedFuture(new RejectedExecutionException("Executor down")));
    when(publisher.publish(any())).thenReturn(brokenStage);

    runToCompletion(
        testKit,
        batchCmd(
            ImmutableList.of(createValidActivity(dedupId), createValidActivity(pendingId)),
            inbox.getRef()));

    var batchRes = (InboxActorResponse.BatchSuccess) inbox.receiveMessage();
    assertThat(batchRes.response().failedRequests()).containsOnlyKeys("1");
    assertThat(batchRes.response().failedRequests().get("1").code()).isEqualTo(503);
    assertThat(batchRes.response().failedRequests().get("1").message()).contains("Executor down");
  }
}
