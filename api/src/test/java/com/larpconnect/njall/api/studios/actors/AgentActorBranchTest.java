package com.larpconnect.njall.api.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.larpconnect.njall.data.dao.studios.actors.AgentDAO;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import com.larpconnect.njall.data.domain.studios.Agent;
import com.larpconnect.njall.data.domain.studios.Collection;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AgentActorBranchTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();
  private final UUID inboxId = UUID.randomUUID();
  private final UUID outboxId = UUID.randomUUID();
  private final Instant now = Instant.now();
  private static final String STUDIO_IDENTIFIER = "valiant";

  private Behavior<AgentCommand> createBehavior(AgentDAO agentDao, CollectionDAO collectionDao) {
    return Behaviors.setup(context -> new AgentActor(context, agentDao, collectionDao));
  }

  private Agent sampleAgent() {
    return new Agent(
        actorId,
        ownerId,
        "valkyrie_lead",
        Optional.of("Lead singer"),
        inboxId,
        outboxId,
        ImmutableMap.of(),
        now,
        now,
        Optional.empty());
  }

  @Test
  @DisplayName("onMutateEndpoint handles validation failures and not-found states")
  void mutateEndpoint_branches() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    when(agentDao.addEndpoint(any(), any(), any(), any())).thenReturn(false);
    when(agentDao.removeEndpoint(any(), any(), any())).thenReturn(false);

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    testKit.run(
        new AgentCommand.MutateEndpoint(
            tenantId,
            actorId,
            AgentCommand.EndpointAction.ADD,
            "invalid-hyphen",
            Optional.of("https://example.com"),
            inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new AgentCommand.MutateEndpoint(
            tenantId,
            actorId,
            AgentCommand.EndpointAction.ADD,
            "sharedInbox",
            Optional.of("https://example.com"),
            inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);

    testKit.run(
        new AgentCommand.MutateEndpoint(
            tenantId,
            actorId,
            AgentCommand.EndpointAction.REMOVE,
            "   ",
            Optional.empty(),
            inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new AgentCommand.MutateEndpoint(
            tenantId,
            actorId,
            AgentCommand.EndpointAction.REMOVE,
            "sharedInbox",
            Optional.empty(),
            inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onPatchAgent handles validation failures and missing actor")
  void patchAgent_branches() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    when(agentDao.findById(tenantId, actorId)).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    var blankReq = new UpdateActorRequest(Optional.of("   "), Optional.empty());
    testKit.run(
        new AgentCommand.PatchAgent(
            tenantId, STUDIO_IDENTIFIER, actorId, blankReq, Optional.empty(), inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    var validReq = new UpdateActorRequest("new_name", "new_summary");
    testKit.run(
        new AgentCommand.PatchAgent(
            tenantId, STUDIO_IDENTIFIER, actorId, validReq, Optional.empty(), inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);

    when(agentDao.findById(tenantId, actorId)).thenReturn(Optional.of(sampleAgent()));
    when(agentDao.update(any(), any(), any(), any())).thenReturn(Optional.empty());
    testKit.run(
        new AgentCommand.PatchAgent(
            tenantId, STUDIO_IDENTIFIER, actorId, validReq, Optional.empty(), inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);
  }

  @Test
  @DisplayName(
      "resolveAndReturnCollection handles missing actor, missing id, and missing collection")
  void accessCollection_missingBranches() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    when(agentDao.findByIdentifier(tenantId, actorId.toString())).thenReturn(Optional.empty());
    when(agentDao.findInboxId(tenantId, actorId)).thenReturn(Optional.of(inboxId));
    testKit.run(
        new AgentCommand.AccessCollection(
            tenantId,
            STUDIO_IDENTIFIER,
            actorId,
            AgentCommand.CollectionTarget.INBOX,
            AgentCommand.CollectionAction.GET,
            Optional.empty(),
            inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);

    when(agentDao.findByIdentifier(tenantId, actorId.toString()))
        .thenReturn(Optional.of(sampleAgent()));
    when(agentDao.findInboxId(tenantId, actorId)).thenReturn(Optional.empty());
    testKit.run(
        new AgentCommand.AccessCollection(
            tenantId,
            STUDIO_IDENTIFIER,
            actorId,
            AgentCommand.CollectionTarget.INBOX,
            AgentCommand.CollectionAction.GET,
            Optional.empty(),
            inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);

    when(agentDao.findInboxId(tenantId, actorId)).thenReturn(Optional.of(inboxId));
    when(collectionDao.findById(tenantId, actorId, inboxId)).thenReturn(Optional.empty());
    testKit.run(
        new AgentCommand.AccessCollection(
            tenantId,
            STUDIO_IDENTIFIER,
            actorId,
            AgentCommand.CollectionTarget.INBOX,
            AgentCommand.CollectionAction.GET,
            Optional.empty(),
            inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("AgentActor handles unexpected DAO exceptions and state exceptions")
  void unexpectedExceptions_branches() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    when(agentDao.create(any(), any(), any(), any()))
        .thenThrow(new IllegalStateException("Entity conflict"));

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    var req =
        new CreateActorRequest(Optional.of(ownerId.toString()), "valkyrie_lead", Optional.empty());
    testKit.run(new AgentCommand.CreateAgent(tenantId, STUDIO_IDENTIFIER, req, inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(409);

    when(agentDao.findByIdentifier(any(), any())).thenThrow(new RuntimeException("DB offline"));
    testKit.run(new AgentCommand.GetAgent(tenantId, STUDIO_IDENTIFIER, actorId, inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(500);

    when(agentDao.delete(any(), any())).thenThrow(new RuntimeException("DB offline"));
    testKit.run(new AgentCommand.DeleteAgent(tenantId, actorId, inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("AgentActor handleError falls back to default reason on null message")
  void unexpectedExceptions_nullMessage() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    when(agentDao.findByIdentifier(any(), any())).thenThrow(new RuntimeException((String) null));

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    testKit.run(new AgentCommand.GetAgent(tenantId, STUDIO_IDENTIFIER, actorId, inbox.getRef()));
    var nullMsgFailure = (AgentActorResponse.Failure) inbox.receiveMessage();
    assertThat(nullMsgFailure.status()).isEqualTo(500);
    assertThat(nullMsgFailure.message()).isEqualTo("Error executing get actor");
  }

  @Test
  @DisplayName("AgentActor returns 400 on invalid generator URI")
  void createAgent_invalidGenerator_returnsBadRequest() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    var invalidGenReq =
        new CreateActorRequest(Optional.of("invalid-gen"), "valkyrie_lead", Optional.empty());
    testKit.run(
        new AgentCommand.CreateAgent(tenantId, STUDIO_IDENTIFIER, invalidGenReq, inbox.getRef()));
    assertThat(((AgentActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("resolveAgent delegates username and UUID-formatted username to findByIdentifier")
  void resolveAgent_usernameAndUuidUsername_delegatesToFindByIdentifier() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    var agent = sampleAgent();
    var uuidFormattedUsername = UUID.randomUUID().toString();

    when(agentDao.findByIdentifier(tenantId, "valkyrie_lead")).thenReturn(Optional.of(agent));
    when(agentDao.findByIdentifier(tenantId, uuidFormattedUsername)).thenReturn(Optional.of(agent));
    when(agentDao.findInboxId(tenantId, actorId)).thenReturn(Optional.of(inboxId));
    when(collectionDao.findById(tenantId, actorId, inboxId))
        .thenReturn(Optional.of(sampleInboxCollection()));
    when(collectionDao.findExpandedItems(eq(tenantId), eq(actorId), any()))
        .thenReturn(ImmutableList.of());

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    testKit.run(
        new AgentCommand.GetAgent(tenantId, STUDIO_IDENTIFIER, "valkyrie_lead", inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(AgentActorResponse.Success.class);

    testKit.run(
        new AgentCommand.GetAgent(
            tenantId, STUDIO_IDENTIFIER, uuidFormattedUsername, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(AgentActorResponse.Success.class);

    testKit.run(
        new AgentCommand.AccessCollection(
            tenantId,
            STUDIO_IDENTIFIER,
            "valkyrie_lead",
            AgentCommand.CollectionTarget.INBOX,
            AgentCommand.CollectionAction.GET,
            Optional.empty(),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(AgentActorResponse.Success.class);
    verify(agentDao, never()).findById(any(), any());
  }

  private Collection sampleInboxCollection() {
    return new Collection(
        inboxId,
        actorId,
        actorId,
        "inbox",
        Optional.empty(),
        0,
        ImmutableList.of(),
        now,
        now,
        Optional.empty());
  }
}
