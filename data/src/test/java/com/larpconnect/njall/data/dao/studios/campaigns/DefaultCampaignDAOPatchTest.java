package com.larpconnect.njall.data.dao.studios.campaigns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultCampaignDAOPatchTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultCampaignDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID campaignId = UUID.randomUUID();
  private final UUID owner = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultCampaignDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("patch updates title, summary, and validated active systemId")
  void patch_allFieldsPresent_updatesCampaign() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var entity =
        new EntityBaseEntity(
            tenantId, campaignId, owner, "Campaign", "Old summary", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, null, "Season One");
    var newSystemId = UUID.randomUUID();
    var activeSystem =
        new EntityBaseEntity(tenantId, newSystemId, owner, "System", "Ruleset", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, newSystemId)))
        .thenReturn(activeSystem);

    var updated =
        dao.patch(
            tenantId,
            campaignId,
            new PatchCampaignParams(
                Optional.of("Season Two"), Optional.of("New summary"), Optional.of(newSystemId)));

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().title()).isEqualTo("Season Two");
    assertThat(updated.orElseThrow().summary()).contains("New summary");
    assertThat(updated.orElseThrow().systemId()).contains(newSystemId);
    assertThat(campaign.getTitle()).isEqualTo("Season Two");
    assertThat(campaign.getSystemId()).isEqualTo(newSystemId);
    assertThat(entity.getSummary()).isEqualTo("New summary");
    verify(session).merge(entity);
    verify(session).merge(campaign);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch preserves active systemId and masks soft-deleted existing systemId")
  void patch_titleOnly_preservesOrMasksExistingSystem() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var entity =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, systemId, "Old Title");
    var activeSystem =
        new EntityBaseEntity(tenantId, systemId, owner, "System", "Ruleset", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(activeSystem);

    var params =
        new PatchCampaignParams(Optional.of("New Title"), Optional.empty(), Optional.empty());
    var preserved = dao.patch(tenantId, campaignId, params);
    assertThat(preserved.orElseThrow().systemId()).contains(systemId);

    var deletedSystem =
        new EntityBaseEntity(tenantId, systemId, owner, "System", "Ruleset", now, now, now);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(deletedSystem);

    var masked = dao.patch(tenantId, campaignId, params);
    assertThat(masked.orElseThrow().systemId()).isEmpty();
  }

  @Test
  @DisplayName("patch clears nullable fields when clear flags are set")
  void patch_clearFlags_clearsNullableFields() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var ignoredId = UUID.randomUUID();
    var entity =
        new EntityBaseEntity(
            tenantId, campaignId, owner, "Campaign", "Old summary", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, systemId, "Anthology");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);

    var updated =
        dao.patch(
            tenantId,
            campaignId,
            new PatchCampaignParams(
                Optional.empty(), Optional.of("Ignored"), true, Optional.of(ignoredId), true));

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().summary()).isEmpty();
    assertThat(updated.orElseThrow().systemId()).isEmpty();
    assertThat(entity.getSummary()).isNull();
    assertThat(campaign.getSystemId()).isNull();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch rejects missing, soft-deleted, or non-System systemId")
  void patch_invalidSystem_throws() {
    assertInvalidSystemPatch(null);

    var deletedSystem =
        new EntityBaseEntity(tenantId, systemId, owner, "System", "Ruleset", now, now, now);
    assertInvalidSystemPatch(deletedSystem);

    var wrongType =
        new EntityBaseEntity(tenantId, systemId, owner, "Location", "Venue", now, now, null);
    assertInvalidSystemPatch(wrongType);
  }

  private void assertInvalidSystemPatch(@Nullable EntityBaseEntity systemEntity) {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var entity =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, null, "Chronicle");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(systemEntity);

    assertThatThrownBy(
            () ->
                dao.patch(
                    tenantId,
                    campaignId,
                    new PatchCampaignParams(
                        Optional.empty(), Optional.empty(), Optional.of(systemId))))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("patch returns empty when missing, soft-deleted, wrong type, or missing subtype")
  void patch_inactiveOrMissing_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var params = new PatchCampaignParams(Optional.of("Title"), Optional.empty(), Optional.empty());

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    assertThat(dao.patch(tenantId, campaignId, params)).isEmpty();

    var wrongType =
        new EntityBaseEntity(tenantId, campaignId, owner, "Game", "Game", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, null, "Game");
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(wrongType);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);
    assertThat(dao.patch(tenantId, campaignId, params)).isEmpty();

    var deletedBase =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, now);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deletedBase);
    assertThat(dao.patch(tenantId, campaignId, params)).isEmpty();

    var activeBase =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(activeBase);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(null);
    assertThat(dao.patch(tenantId, campaignId, params)).isEmpty();

    verify(session, never()).merge(any());
  }

  @Test
  @DisplayName("patch rolls back and rethrows on merge failure")
  void patch_mergeError_rollsBack() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var entity =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, null, "Chronicle");
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);
    doThrow(new RuntimeException("Merge failed")).when(session).merge(entity);

    assertThatThrownBy(
            () ->
                dao.patch(
                    tenantId,
                    campaignId,
                    new PatchCampaignParams(Optional.empty(), Optional.empty(), Optional.empty())))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Merge failed");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("softDelete marks deleted_on on active Campaign")
  void softDelete_activeCampaign_returnsTrue() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var entity =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var deleted = dao.softDelete(tenantId, campaignId);

    assertThat(deleted).isTrue();
    assertThat(entity.getDeletedOn()).isNotNull();
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when missing, already deleted, or wrong entity_type")
  void softDelete_inactiveOrWrongType_returnsFalse() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    assertThat(dao.softDelete(tenantId, campaignId)).isFalse();

    var deletedEntity =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, now);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deletedEntity);
    assertThat(dao.softDelete(tenantId, campaignId)).isFalse();

    var wrongType =
        new EntityBaseEntity(tenantId, campaignId, owner, "System", "Ruleset", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(wrongType);
    assertThat(dao.softDelete(tenantId, campaignId)).isFalse();

    verify(session, never()).merge(any());
  }

  @Test
  @DisplayName("softDelete rolls back and rethrows on database failure")
  void softDelete_error_rollsBack() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    when(session.find(EntityBaseEntity.class, entityId))
        .thenThrow(new RuntimeException("Delete error"));

    assertThatThrownBy(() -> dao.softDelete(tenantId, campaignId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Delete error");
    verify(tx).rollback();
  }

  private void mockTenantConfigQuery() {
    NativeQuery<String> configQuery = mock();
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());
  }
}
