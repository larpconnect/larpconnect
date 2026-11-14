package com.larpconnect.njall.api.studios.campaigns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.studios.campaigns.CampaignDAO;
import com.larpconnect.njall.data.dao.studios.campaigns.PatchCampaignParams;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.Campaign;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CampaignActorTest {

  private static final String STUDIO_ALIAS = "valiant";

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID campaignId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final Instant now = Instant.now();
  private final StudioLookup lookup =
      new StudioLookup(tenantId, studioId, STUDIO_ALIAS, now, now, Optional.empty());

  private Behavior<CampaignCommand> createBehavior(CampaignDAO dao) {
    return CampaignActor.create(dao);
  }

  private Campaign sampleCampaign(Optional<UUID> sysId, Optional<String> summary) {
    return new Campaign(
        campaignId, actorId, sysId, "Seattle By Night", summary, now, now, Optional.empty());
  }

  @Test
  @DisplayName("onCreateCampaign creates campaign with canonical URIs and returns Success")
  void onCreateCampaign_validWithSystem_returnsSuccess() {
    var dao = mock(CampaignDAO.class);
    var domain = sampleCampaign(Optional.of(systemId), Optional.of("Pacific Northwest chronicle"));
    when(dao.create(
            tenantId,
            actorId,
            "Seattle By Night",
            Optional.of("Pacific Northwest chronicle"),
            Optional.of(systemId)))
        .thenReturn(domain);

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();
    var request =
        new CreateCampaignRequest(
            "/api/studios/valiant/v1/actors/" + actorId,
            "Seattle By Night",
            "Pacific Northwest chronicle",
            "/api/studios/valiant/v1/systems/" + systemId);
    testKit.run(
        new CampaignCommand.CreateCampaign(
            tenantId, studioId.toString(), request, inbox.getRef(), Optional.of(lookup)));

    var response = (CampaignActorResponse.Success) inbox.receiveMessage();
    assertThat(response.campaign().id()).isEqualTo(campaignId);
    assertThat(response.campaign().generator())
        .isEqualTo("/api/studios/" + studioId + "/v1/actors/" + actorId);
    assertThat(response.campaign().system())
        .contains("/api/studios/" + studioId + "/v1/systems/" + systemId);
  }

  @Test
  @DisplayName("onCreateCampaign creates systemless campaign when system is absent")
  void onCreateCampaign_systemless_returnsSuccessWithEmptySystem() {
    var dao = mock(CampaignDAO.class);
    var domain = sampleCampaign(Optional.empty(), Optional.empty());
    when(dao.create(tenantId, actorId, "Anthology", Optional.empty(), Optional.empty()))
        .thenReturn(domain);

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();
    var request = new CreateCampaignRequest(actorId.toString(), "Anthology");
    testKit.run(
        new CampaignCommand.CreateCampaign(tenantId, STUDIO_ALIAS, request, inbox.getRef()));

    var response = (CampaignActorResponse.Success) inbox.receiveMessage();
    assertThat(response.campaign().system()).isEmpty();
    assertThat(response.campaign().summary()).isEmpty();
  }

  @Test
  @DisplayName("onCreateCampaign returns 400 on blank title or invalid references")
  void onCreateCampaign_invalidInput_returnsBadRequest() {
    var dao = mock(CampaignDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CampaignCommand.CreateCampaign(
            tenantId,
            STUDIO_ALIAS,
            new CreateCampaignRequest(actorId.toString(), "  "),
            inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new CampaignCommand.CreateCampaign(
            tenantId,
            STUDIO_ALIAS,
            new CreateCampaignRequest("/api/studios/valiant/v1/systems/" + actorId, "Title"),
            inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new CampaignCommand.CreateCampaign(
            tenantId,
            STUDIO_ALIAS,
            new CreateCampaignRequest(actorId.toString(), "Title", null, "   "),
            inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new CampaignCommand.CreateCampaign(
            tenantId,
            STUDIO_ALIAS,
            new CreateCampaignRequest(
                actorId.toString(), "Title", null, "/api/studios/other/v1/systems/" + systemId),
            inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onCreateCampaign maps DAO IllegalArgumentException and generic errors")
  void onCreateCampaign_daoExceptions_mapsStatusCodes() {
    var dao = mock(CampaignDAO.class);
    when(dao.create(any(), any(), eq("Bad"), any(), any()))
        .thenThrow(new IllegalArgumentException("System not active"));
    when(dao.create(any(), any(), eq("NullIae"), any(), any()))
        .thenThrow(new IllegalArgumentException());
    when(dao.create(any(), any(), eq("Fail"), any(), any()))
        .thenThrow(new RuntimeException("DB offline"));
    when(dao.create(any(), any(), eq("NullFail"), any(), any())).thenThrow(new RuntimeException());

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();

    for (var title : ImmutableList.of("Bad", "NullIae", "Fail", "NullFail")) {
      testKit.run(
          new CampaignCommand.CreateCampaign(
              tenantId,
              STUDIO_ALIAS,
              new CreateCampaignRequest(actorId.toString(), title),
              inbox.getRef()));
    }

    var bad = (CampaignActorResponse.Failure) inbox.receiveMessage();
    var nullIae = (CampaignActorResponse.Failure) inbox.receiveMessage();
    var fail = (CampaignActorResponse.Failure) inbox.receiveMessage();
    var nullFail = (CampaignActorResponse.Failure) inbox.receiveMessage();
    assertThat(bad).isEqualTo(new CampaignActorResponse.Failure(400, "System not active"));
    assertThat(nullIae.status()).isEqualTo(400);
    assertThat(nullIae.message()).isNotBlank();
    assertThat(fail).isEqualTo(new CampaignActorResponse.Failure(500, "DB offline"));
    assertThat(nullFail.status()).isEqualTo(500);
    assertThat(nullFail.message()).isNotBlank();
  }

  @Test
  @DisplayName("onGetCampaign returns Success, 404, or 500")
  void onGetCampaign_handlesFoundMissingAndError() {
    var dao = mock(CampaignDAO.class);
    var otherId = UUID.randomUUID();
    var errorId = UUID.randomUUID();
    when(dao.findById(tenantId, campaignId))
        .thenReturn(Optional.of(sampleCampaign(Optional.empty(), Optional.of("Synopsis"))));
    when(dao.findById(tenantId, otherId)).thenReturn(Optional.empty());
    when(dao.findById(tenantId, errorId)).thenThrow(new RuntimeException("Query error"));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CampaignCommand.GetCampaign(tenantId, STUDIO_ALIAS, campaignId, inbox.getRef()));
    var found = (CampaignActorResponse.Success) inbox.receiveMessage();
    assertThat(found.campaign().system()).isEmpty();
    assertThat(found.campaign().summary()).contains("Synopsis");

    testKit.run(new CampaignCommand.GetCampaign(tenantId, STUDIO_ALIAS, otherId, inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);

    testKit.run(new CampaignCommand.GetCampaign(tenantId, STUDIO_ALIAS, errorId, inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onListCampaigns parses optional systemId filter and returns Items or errors")
  void onListCampaigns_handlesFilteringAndErrors() {
    var dao = mock(CampaignDAO.class);
    var domain = sampleCampaign(Optional.of(systemId), Optional.empty());
    when(dao.listAll(tenantId, Optional.empty())).thenReturn(ImmutableList.of(domain));
    when(dao.listAll(tenantId, Optional.of(systemId))).thenReturn(ImmutableList.of(domain));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CampaignCommand.ListCampaigns(
            tenantId, STUDIO_ALIAS, Optional.empty(), inbox.getRef()));
    var all = (CampaignActorResponse.Items) inbox.receiveMessage();
    assertThat(all.campaigns()).hasSize(1);

    testKit.run(
        new CampaignCommand.ListCampaigns(
            tenantId,
            studioId.toString(),
            Optional.of("/api/studios/valiant/v1/systems/" + systemId),
            inbox.getRef(),
            Optional.of(lookup)));
    var filtered = (CampaignActorResponse.Items) inbox.receiveMessage();
    assertThat(filtered.campaigns()).hasSize(1);

    testKit.run(
        new CampaignCommand.ListCampaigns(
            tenantId, STUDIO_ALIAS, Optional.of("not-a-uuid"), inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    when(dao.listAll(tenantId, Optional.empty())).thenThrow(new RuntimeException("List failed"));
    testKit.run(
        new CampaignCommand.ListCampaigns(
            tenantId, STUDIO_ALIAS, Optional.empty(), inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onPatchCampaign applies update_mask and ignores unmasked invalid fields")
  void onPatchCampaign_updateMask_updatesTargetedFieldsOnly() {
    var dao = mock(CampaignDAO.class);
    var updated = sampleCampaign(Optional.of(systemId), Optional.of("Kept"));
    when(dao.patch(eq(tenantId), eq(campaignId), any())).thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();

    var maskedUpdate =
        new UpdateCampaignRequest("Season Two", "Ignored summary", systemId.toString());
    testKit.run(
        new CampaignCommand.PatchCampaign(
            tenantId,
            STUDIO_ALIAS,
            campaignId,
            maskedUpdate,
            Optional.of("title,system"),
            inbox.getRef(),
            Optional.of(lookup)));
    assertThat(inbox.receiveMessage()).isInstanceOf(CampaignActorResponse.Success.class);
    verify(dao)
        .patch(
            tenantId,
            campaignId,
            new PatchCampaignParams(
                Optional.of("Season Two"), Optional.empty(), false, Optional.of(systemId), false));

    var unmaskedInvalidUpdate =
        new UpdateCampaignRequest("   ", "Updated summary", "invalid-system-uri");
    testKit.run(
        new CampaignCommand.PatchCampaign(
            tenantId,
            STUDIO_ALIAS,
            campaignId,
            unmaskedInvalidUpdate,
            Optional.of("summary"),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(CampaignActorResponse.Success.class);
    verify(dao)
        .patch(
            tenantId,
            campaignId,
            new PatchCampaignParams(
                Optional.empty(), Optional.of("Updated summary"), false, Optional.empty(), false));
  }

  @Test
  @DisplayName("onPatchCampaign clears nullable fields only when named in update_mask")
  void onPatchCampaign_clearAndUnmaskedNull_respectsMaskSemantics() {
    var dao = mock(CampaignDAO.class);
    var updated = sampleCampaign(Optional.empty(), Optional.empty());
    when(dao.patch(eq(tenantId), eq(campaignId), any())).thenReturn(Optional.of(updated));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CampaignCommand.PatchCampaign(
            tenantId,
            STUDIO_ALIAS,
            campaignId,
            new UpdateCampaignRequest(),
            Optional.of("system,summary"),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(CampaignActorResponse.Success.class);
    verify(dao)
        .patch(
            tenantId,
            campaignId,
            new PatchCampaignParams(
                Optional.empty(), Optional.empty(), true, Optional.empty(), true));

    testKit.run(
        new CampaignCommand.PatchCampaign(
            tenantId,
            STUDIO_ALIAS,
            campaignId,
            new UpdateCampaignRequest(),
            Optional.empty(),
            inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(CampaignActorResponse.Success.class);
    verify(dao)
        .patch(
            tenantId,
            campaignId,
            new PatchCampaignParams(
                Optional.empty(), Optional.empty(), false, Optional.empty(), false));
  }

  @Test
  @DisplayName("onPatchCampaign rejects cleared title and malformed system URI")
  void onPatchCampaign_validationErrors_returnsBadRequest() {
    var dao = mock(CampaignDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CampaignCommand.PatchCampaign(
            tenantId,
            STUDIO_ALIAS,
            campaignId,
            new UpdateCampaignRequest(),
            Optional.of("title"),
            inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new CampaignCommand.PatchCampaign(
            tenantId,
            STUDIO_ALIAS,
            campaignId,
            new UpdateCampaignRequest(Optional.empty(), Optional.empty(), Optional.of("bad-sys")),
            Optional.of("system"),
            inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);
  }

  @Test
  @DisplayName("onPatchCampaign maps missing target, DAO IllegalArgumentException, and errors")
  void onPatchCampaign_missingAndDaoErrors_returnsExpectedStatuses() {
    var dao = mock(CampaignDAO.class);
    var missingId = UUID.randomUUID();
    when(dao.patch(eq(tenantId), eq(missingId), any())).thenReturn(Optional.empty());
    when(dao.patch(eq(tenantId), eq(campaignId), any()))
        .thenThrow(new IllegalArgumentException("Non-System entity"))
        .thenThrow(new RuntimeException("Patch error"));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();

    testKit.run(
        new CampaignCommand.PatchCampaign(
            tenantId,
            STUDIO_ALIAS,
            missingId,
            new UpdateCampaignRequest("Valid", null, null),
            Optional.of("title"),
            inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);

    testKit.run(
        new CampaignCommand.PatchCampaign(
            tenantId,
            STUDIO_ALIAS,
            campaignId,
            new UpdateCampaignRequest("Valid", null, systemId.toString()),
            Optional.of("title,system"),
            inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new CampaignCommand.PatchCampaign(
            tenantId,
            STUDIO_ALIAS,
            campaignId,
            new UpdateCampaignRequest("Valid", null, null),
            Optional.of("title"),
            inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onDeleteCampaign handles success, not found, and error")
  void onDeleteCampaign_handlesAllOutcomes() {
    var dao = mock(CampaignDAO.class);
    var missingId = UUID.randomUUID();
    var errorId = UUID.randomUUID();
    when(dao.softDelete(tenantId, campaignId)).thenReturn(true);
    when(dao.softDelete(tenantId, missingId)).thenReturn(false);
    when(dao.softDelete(tenantId, errorId)).thenThrow(new RuntimeException("Delete error"));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<CampaignActorResponse> inbox = TestInbox.create();

    testKit.run(new CampaignCommand.DeleteCampaign(tenantId, campaignId, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(CampaignActorResponse.Deleted.class);

    testKit.run(new CampaignCommand.DeleteCampaign(tenantId, missingId, inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);

    testKit.run(new CampaignCommand.DeleteCampaign(tenantId, errorId, inbox.getRef()));
    assertThat(((CampaignActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("DefaultCampaignActorFactory creates runnable CampaignActor behavior")
  void defaultFactory_executesCommand() {
    var dao = mock(CampaignDAO.class);
    when(dao.softDelete(tenantId, campaignId)).thenReturn(true);
    var factory = new DefaultCampaignActorFactory(dao);
    var testKit = BehaviorTestKit.create(factory.create());
    var inbox = TestInbox.<CampaignActorResponse>create();

    testKit.run(new CampaignCommand.DeleteCampaign(tenantId, campaignId, inbox.getRef()));

    assertThat(inbox.receiveMessage()).isInstanceOf(CampaignActorResponse.Deleted.class);
  }
}
