package com.larpconnect.njall.api.studios.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.common.config.ServerConfig;
import com.larpconnect.njall.data.dao.studios.actors.AgentDAO;
import com.larpconnect.njall.data.dao.studios.inbox.InboxActivityDAO;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.Agent;
import com.larpconnect.njall.queue.publisher.InboxActivityPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class InboxActorTest {

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
    when(agentDao.findByIdentifier(tenantId, "valkyrie")).thenReturn(Optional.of(sampleAgent()));
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

  private InboxCommand.DeliverActivity singleCmd(
      JsonNode activity, ActorRef<InboxActorResponse> replyTo) {
    return new InboxCommand.DeliverActivity(
        tenantId,
        studioId,
        "valiant",
        receivingActorId.toString(),
        activity,
        Optional.empty(),
        replyTo,
        Optional.of(lookup));
  }

  private static void runToCompletion(BehaviorTestKit<InboxCommand> testKit, InboxCommand cmd) {
    testKit.run(cmd);
    drainSelfInbox(testKit);
  }

  private static void drainSelfInbox(BehaviorTestKit<InboxCommand> testKit) {
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
    ArrayNode to = node.putArray("to");
    to.add("/api/studios/valiant/v1/actors/galahad");
    node.set("object", mapper.createObjectNode().put("name", "Quest"));
    return node;
  }

  @Test
  @DisplayName("DeliverActivity pipes asynchronous AMQP completion to actor thread before DAO save")
  void deliverActivity_asyncPipeToSelf_succeeds() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    String activityId = "https://larpconnect.app/act/1";
    ObjectNode activity = createValidActivity(activityId);

    when(inboxActivityDao.resolveRoutableRecipients(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableSet.of(targetActorId));
    when(cache.filterUnseen(eq(tenantId), eq(activityId), any(), any()))
        .thenReturn(ImmutableSet.of(targetActorId));

    var pendingPublish = new CompletableFuture<Void>();
    when(publisher.publish(any())).thenReturn(pendingPublish);

    var cmd =
        new InboxCommand.DeliverActivity(
            tenantId,
            studioId,
            "valiant",
            "valkyrie",
            activity,
            Optional.of("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"),
            inbox.getRef(),
            Optional.of(lookup));
    activity.put("type", "MutatedOutside");
    ((ObjectNode) cmd.activity()).put("type", "MutatedAccessor");

    testKit.run(cmd);
    assertThat(inbox.hasMessages()).isFalse();
    verify(inboxActivityDao, never()).recordAcceptedActivity(any(), any(), any(), any(), any());

    pendingPublish.complete(null);
    drainSelfInbox(testKit);

    assertThat(inbox.receiveMessage()).isInstanceOf(InboxActorResponse.Success.class);
    verify(inboxActivityDao)
        .recordAcceptedActivity(eq(tenantId), eq(activityId), any(), any(), any());
    verify(cache).recordSeen(eq(tenantId), eq(activityId), any());
  }

  @Test
  @DisplayName("DeliverActivity returns 404 when actor does not exist")
  void deliverActivity_missingActor_returns404() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    when(agentDao.findByIdentifier(tenantId, "ghost")).thenReturn(Optional.empty());

    runToCompletion(
        testKit,
        new InboxCommand.DeliverActivity(
            tenantId,
            studioId,
            "valiant",
            "ghost",
            createValidActivity("https://larpconnect.app/act/ghost"),
            Optional.empty(),
            inbox.getRef(),
            Optional.empty()));

    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(404);
    verify(publisher, never()).publish(any());
  }

  @Test
  @DisplayName("DeliverActivity skips AMQP publish when all target recipients were deduplicated")
  void deliverActivity_allDeduplicated_skipsPublish() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    String activityId = "https://larpconnect.app/act/2";

    when(inboxActivityDao.resolveRoutableRecipients(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableSet.of(targetActorId));
    when(cache.filterUnseen(eq(tenantId), eq(activityId), any(), any()))
        .thenReturn(ImmutableSet.of());

    runToCompletion(testKit, singleCmd(createValidActivity(activityId), inbox.getRef()));

    assertThat(inbox.receiveMessage()).isInstanceOf(InboxActorResponse.Success.class);
    verify(publisher, never()).publish(any());
  }

  @Test
  @DisplayName("DeliverActivity returns 400 when activity fails validation")
  void deliverActivity_invalidActivity_returns400() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();

    runToCompletion(testKit, singleCmd(mapper.createObjectNode(), inbox.getRef()));

    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(400);
  }

  @Test
  @DisplayName("DeliverActivity returns 400 when activity contains only foreign recipients")
  void deliverActivity_foreignRecipientOnly_returns400() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    ObjectNode foreign = createValidActivity("https://larpconnect.app/act/foreign");
    foreign.putArray("to").add("https://larpconnect.org/api/studios/other/v1/actors/someone");

    runToCompletion(testKit, singleCmd(foreign, inbox.getRef()));

    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(400);
  }

  @Test
  @DisplayName("DeliverActivity returns 400 when candidate recipients are not routable")
  void deliverActivity_unroutableTarget_returns400() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    when(inboxActivityDao.resolveRoutableRecipients(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableSet.of());

    runToCompletion(
        testKit,
        singleCmd(createValidActivity("https://larpconnect.app/act/unroutable"), inbox.getRef()));

    var unroutable = (InboxActorResponse.Failure) inbox.receiveMessage();
    assertThat(unroutable.statusCode()).isEqualTo(400);
    assertThat(unroutable.message()).contains("Candidate recipients are not routable");
  }

  @Test
  @DisplayName("DeliverActivity returns 500 on unexpected runtime failure")
  void deliverActivity_unexpectedRuntimeFailure_returns500() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    when(inboxActivityDao.resolveRoutableRecipients(eq(tenantId), eq(receivingActorId), any()))
        .thenThrow(new IllegalStateException("DB crash"));

    runToCompletion(
        testKit,
        singleCmd(createValidActivity("https://larpconnect.app/act/crash"), inbox.getRef()));

    assertThat(((InboxActorResponse.Failure) inbox.receiveMessage()).statusCode()).isEqualTo(500);
  }

  @Test
  @DisplayName("DeliverActivity returns 503 and skips DAO write when AMQP broker publish fails")
  void deliverActivity_brokerPublishFailure_returns503() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    String activityId = "https://larpconnect.app/act/4";

    when(inboxActivityDao.resolveRoutableRecipients(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableSet.of(targetActorId));
    when(cache.filterUnseen(eq(tenantId), eq(activityId), any(), any()))
        .thenReturn(ImmutableSet.of(targetActorId));
    when(publisher.publish(any()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException((String) null)));

    runToCompletion(testKit, singleCmd(createValidActivity(activityId), inbox.getRef()));

    var brokerFailure = (InboxActorResponse.Failure) inbox.receiveMessage();
    assertThat(brokerFailure.statusCode()).isEqualTo(503);
    assertThat(brokerFailure.message()).contains("RuntimeException");
    verify(inboxActivityDao, never()).recordAcceptedActivity(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("DeliverActivity returns 503 and skips cache update when post-publish DAO fails")
  void deliverActivity_daoPersistenceFailure_returns503() {
    var testKit = createTestKit();
    TestInbox<InboxActorResponse> inbox = TestInbox.create();
    String activityId = "https://larpconnect.app/act/5";

    when(inboxActivityDao.resolveRoutableRecipients(eq(tenantId), eq(receivingActorId), any()))
        .thenReturn(ImmutableSet.of(targetActorId));
    when(cache.filterUnseen(eq(tenantId), eq(activityId), any(), any()))
        .thenReturn(ImmutableSet.of(targetActorId));
    when(publisher.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    doThrow(new IllegalStateException("DB write failed"))
        .when(inboxActivityDao)
        .recordAcceptedActivity(any(), any(), any(), any(), any());

    runToCompletion(testKit, singleCmd(createValidActivity(activityId), inbox.getRef()));

    var daoFailure = (InboxActorResponse.Failure) inbox.receiveMessage();
    assertThat(daoFailure.statusCode()).isEqualTo(503);
    assertThat(daoFailure.message()).contains("Failed to persist accepted activity");
    verify(cache, never()).recordSeen(any(), any(), any());
  }
}
