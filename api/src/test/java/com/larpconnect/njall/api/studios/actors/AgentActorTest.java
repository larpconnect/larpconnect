package com.larpconnect.njall.api.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.larpconnect.njall.api.studios.collections.OrderedCollectionResponse;
import com.larpconnect.njall.data.dao.studios.actors.AgentDAO;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import com.larpconnect.njall.data.domain.Agent;
import com.larpconnect.njall.data.domain.Collection;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AgentActorTest {

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
        ImmutableMap.of("sharedInbox", "https://fed.example.com/inbox"),
        now,
        now,
        Optional.empty());
  }

  private Collection sampleCollection(UUID id, String name) {
    return new Collection(
        id,
        actorId,
        name,
        Optional.of(name + " collection"),
        0,
        ImmutableList.of(),
        now,
        now,
        Optional.empty());
  }

  @Test
  @DisplayName("onCreateAgent returns Success on valid payload")
  void onCreateAgent_valid_returnsSuccess() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    var domain = sampleAgent();

    when(agentDao.existsByPreferredUsername(tenantId, "valkyrie_lead", Optional.empty()))
        .thenReturn(false);
    when(agentDao.create(
            eq(tenantId), eq(ownerId), eq("valkyrie_lead"), eq(Optional.of("Lead singer"))))
        .thenReturn(domain);

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    var request = new CreateActorRequest(ownerId, "valkyrie_lead", Optional.of("Lead singer"));
    testKit.run(new AgentCommand.CreateAgent(tenantId, STUDIO_IDENTIFIER, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AgentActorResponse.Success.class);
    var success = (AgentActorResponse.Success) response;
    var actor = (ActorResponse) success.payload().orElseThrow();
    assertThat(actor.id()).isEqualTo(actorId);
    assertThat(actor.preferredUsername()).isEqualTo("valkyrie_lead");
    assertThat(actor.inbox()).endsWith("/inbox");
    assertThat(actor.outbox()).endsWith("/outbox");
  }

  @Test
  @DisplayName("onCreateAgent returns BadRequest on blank username")
  void onCreateAgent_blankUsername_returnsBadRequest() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    var request = new CreateActorRequest(ownerId, "   ", Optional.empty());
    testKit.run(new AgentCommand.CreateAgent(tenantId, STUDIO_IDENTIFIER, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AgentActorResponse.Failure.class);
    assertThat(((AgentActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateAgent returns Conflict on duplicate username")
  void onCreateAgent_duplicateUsername_returnsConflict() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    when(agentDao.existsByPreferredUsername(tenantId, "valkyrie_lead", Optional.empty()))
        .thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    var request = new CreateActorRequest(ownerId, "valkyrie_lead", Optional.empty());
    testKit.run(new AgentCommand.CreateAgent(tenantId, STUDIO_IDENTIFIER, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AgentActorResponse.Failure.class);
    assertThat(((AgentActorResponse.Failure) response).status()).isEqualTo(409);
  }

  @Test
  @DisplayName("onCreateAgent returns BadRequest on missing owner")
  void onCreateAgent_missingOwner_returnsBadRequest() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    when(agentDao.existsByPreferredUsername(tenantId, "valkyrie_lead", Optional.empty()))
        .thenReturn(false);
    when(agentDao.create(any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Owner entity not found or deleted"));

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    var request = new CreateActorRequest(ownerId, "valkyrie_lead", Optional.empty());
    testKit.run(new AgentCommand.CreateAgent(tenantId, STUDIO_IDENTIFIER, request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AgentActorResponse.Failure.class);
    assertThat(((AgentActorResponse.Failure) response).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onGetAgent returns Success when found and NotFound when absent")
  void onGetAgent_retrieval() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    when(agentDao.findById(tenantId, actorId)).thenReturn(Optional.of(sampleAgent()));

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    testKit.run(new AgentCommand.GetAgent(tenantId, STUDIO_IDENTIFIER, actorId, inbox.getRef()));
    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(AgentActorResponse.Success.class);

    var missingId = UUID.randomUUID();
    when(agentDao.findById(tenantId, missingId)).thenReturn(Optional.empty());
    testKit.run(new AgentCommand.GetAgent(tenantId, STUDIO_IDENTIFIER, missingId, inbox.getRef()));
    var notFoundRes = inbox.receiveMessage();
    assertThat(notFoundRes).isInstanceOf(AgentActorResponse.Failure.class);
    assertThat(((AgentActorResponse.Failure) notFoundRes).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onPatchAgent updates properties and returns updated actor")
  void onPatchAgent_updates() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    var agent = sampleAgent();

    when(agentDao.findById(tenantId, actorId)).thenReturn(Optional.of(agent));
    when(agentDao.existsByPreferredUsername(tenantId, "valkyrie_commander", Optional.of(actorId)))
        .thenReturn(false);
    when(agentDao.update(
            tenantId, actorId, Optional.of("valkyrie_commander"), Optional.of("New bio")))
        .thenReturn(
            Optional.of(
                new Agent(
                    actorId,
                    ownerId,
                    "valkyrie_commander",
                    Optional.of("New bio"),
                    inboxId,
                    outboxId,
                    ImmutableMap.of(),
                    now,
                    now,
                    Optional.empty())));

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    var updateReq = new UpdateActorRequest("valkyrie_commander", "New bio");
    testKit.run(
        new AgentCommand.PatchAgent(
            tenantId,
            STUDIO_IDENTIFIER,
            actorId,
            updateReq,
            Optional.of("preferredUsername,summary"),
            inbox.getRef()));

    var res = inbox.receiveMessage();
    assertThat(res).isInstanceOf(AgentActorResponse.Success.class);
    var success = (AgentActorResponse.Success) res;
    var updated = (ActorResponse) success.payload().orElseThrow();
    assertThat(updated.preferredUsername()).isEqualTo("valkyrie_commander");
  }

  @Test
  @DisplayName("onPatchAgent returns Conflict when username taken")
  void onPatchAgent_conflict() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    var agent = sampleAgent();

    when(agentDao.findById(tenantId, actorId)).thenReturn(Optional.of(agent));
    when(agentDao.existsByPreferredUsername(tenantId, "taken_name", Optional.of(actorId)))
        .thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    var conflictReq = new UpdateActorRequest("taken_name", null);
    testKit.run(
        new AgentCommand.PatchAgent(
            tenantId,
            STUDIO_IDENTIFIER,
            actorId,
            conflictReq,
            Optional.of("preferredUsername"),
            inbox.getRef()));
    var conflictRes = inbox.receiveMessage();
    assertThat(conflictRes).isInstanceOf(AgentActorResponse.Failure.class);
    assertThat(((AgentActorResponse.Failure) conflictRes).status()).isEqualTo(409);
  }

  @Test
  @DisplayName("onDeleteAgent deletes and returns NoContent or NotFound")
  void onDeleteAgent_delete() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    when(agentDao.delete(tenantId, actorId)).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    testKit.run(new AgentCommand.DeleteAgent(tenantId, actorId, inbox.getRef()));
    var res = inbox.receiveMessage();
    assertThat(res).isEqualTo(AgentActorResponse.noContent());

    var missingId = UUID.randomUUID();
    when(agentDao.delete(tenantId, missingId)).thenReturn(false);
    testKit.run(new AgentCommand.DeleteAgent(tenantId, missingId, inbox.getRef()));
    var notFoundRes = inbox.receiveMessage();
    assertThat(notFoundRes).isInstanceOf(AgentActorResponse.Failure.class);
    assertThat(((AgentActorResponse.Failure) notFoundRes).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onMutateEndpoint handles endpoint mutations")
  void endpoints_mutation() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    when(agentDao.addEndpoint(tenantId, actorId, "sharedInbox", "https://fed.example.com/inbox"))
        .thenReturn(true);
    when(agentDao.removeEndpoint(tenantId, actorId, "sharedInbox")).thenReturn(true);

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    testKit.run(
        new AgentCommand.MutateEndpoint(
            tenantId,
            actorId,
            AgentCommand.EndpointAction.ADD,
            "sharedInbox",
            Optional.of("https://fed.example.com/inbox"),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isEqualTo(AgentActorResponse.noContent());

    testKit.run(
        new AgentCommand.MutateEndpoint(
            tenantId,
            actorId,
            AgentCommand.EndpointAction.REMOVE,
            "sharedInbox",
            Optional.empty(),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isEqualTo(AgentActorResponse.noContent());
  }

  @Test
  @DisplayName("onAccessCollection returns expanded collections")
  void inboxOutbox_retrieval() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    var agent = sampleAgent();

    when(agentDao.findById(tenantId, actorId)).thenReturn(Optional.of(agent));
    when(agentDao.findInboxId(tenantId, actorId)).thenReturn(Optional.of(inboxId));
    when(agentDao.findOutboxId(tenantId, actorId)).thenReturn(Optional.of(outboxId));
    when(collectionDao.findById(tenantId, inboxId))
        .thenReturn(Optional.of(sampleCollection(inboxId, "inbox")));
    when(collectionDao.findById(tenantId, outboxId))
        .thenReturn(Optional.of(sampleCollection(outboxId, "outbox")));
    when(collectionDao.findExpandedItems(eq(tenantId), any())).thenReturn(ImmutableList.of());

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();

    testKit.run(
        new AgentCommand.AccessCollection(
            tenantId,
            actorId,
            AgentCommand.CollectionTarget.INBOX,
            AgentCommand.CollectionAction.GET,
            Optional.empty(),
            inbox.getRef()));
    var inboxRes = inbox.receiveMessage();
    assertThat(inboxRes).isInstanceOf(AgentActorResponse.Success.class);
    var cs1 =
        (OrderedCollectionResponse) ((AgentActorResponse.Success) inboxRes).payload().orElseThrow();
    assertThat(cs1.name()).isEqualTo("inbox");

    testKit.run(
        new AgentCommand.AccessCollection(
            tenantId,
            actorId,
            AgentCommand.CollectionTarget.OUTBOX,
            AgentCommand.CollectionAction.GET,
            Optional.empty(),
            inbox.getRef()));
    var outboxRes = inbox.receiveMessage();
    assertThat(outboxRes).isInstanceOf(AgentActorResponse.Success.class);
    var cs2 =
        (OrderedCollectionResponse)
            ((AgentActorResponse.Success) outboxRes).payload().orElseThrow();
    assertThat(cs2.name()).isEqualTo("outbox");
  }

  @Test
  @DisplayName("onAccessCollection POST returns Created when actor exists")
  void inboxOutbox_posting() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    when(agentDao.findById(tenantId, actorId)).thenReturn(Optional.of(sampleAgent()));

    var testKit = BehaviorTestKit.create(createBehavior(agentDao, collectionDao));
    TestInbox<AgentActorResponse> inbox = TestInbox.create();
    var payload = JsonNodeFactory.instance.objectNode().put("type", "Create");

    testKit.run(
        new AgentCommand.AccessCollection(
            tenantId,
            actorId,
            AgentCommand.CollectionTarget.INBOX,
            AgentCommand.CollectionAction.POST,
            Optional.of(payload),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isEqualTo(AgentActorResponse.created());

    testKit.run(
        new AgentCommand.AccessCollection(
            tenantId,
            actorId,
            AgentCommand.CollectionTarget.OUTBOX,
            AgentCommand.CollectionAction.POST,
            Optional.of(payload),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isEqualTo(AgentActorResponse.created());

    var missingId = UUID.randomUUID();
    when(agentDao.findById(tenantId, missingId)).thenReturn(Optional.empty());
    testKit.run(
        new AgentCommand.AccessCollection(
            tenantId,
            missingId,
            AgentCommand.CollectionTarget.INBOX,
            AgentCommand.CollectionAction.POST,
            Optional.of(payload),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(AgentActorResponse.Failure.class);
  }

  @Test
  @DisplayName("DefaultAgentActorFactory creates behavior")
  void factory_createsBehavior() {
    var agentDao = mock(AgentDAO.class);
    var collectionDao = mock(CollectionDAO.class);
    var factory = new DefaultAgentActorFactory(agentDao, collectionDao);
    assertThat(factory.create()).isNotNull();
  }
}
