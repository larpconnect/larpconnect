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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultCampaignDAOTest {

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
  @DisplayName("findById sets app.tenant_id and returns active Campaign with active System")
  void findById_activeCampaignWithActiveSystem_returnsCampaign() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var entity =
        new EntityBaseEntity(
            tenantId, campaignId, owner, "Campaign", "Camarilla chronicle", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, systemId, "Seattle By Night");
    var activeSystem =
        new EntityBaseEntity(tenantId, systemId, owner, "System", "VtM 5e", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(activeSystem);

    var result = dao.findById(tenantId, campaignId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(campaignId);
    assertThat(result.orElseThrow().generator()).isEqualTo(owner);
    assertThat(result.orElseThrow().systemId()).contains(systemId);
    assertThat(result.orElseThrow().title()).isEqualTo("Seattle By Night");
    assertThat(result.orElseThrow().summary()).contains("Camarilla chronicle");
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns active systemless Campaign without querying system entity")
  void findById_activeCampaignWithoutSystem_returnsCampaign() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var entity =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", null, now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, null, "Freeform Anthology");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);

    var result = dao.findById(tenantId, campaignId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().systemId()).isEmpty();
    assertThat(result.orElseThrow().summary()).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById masks soft-deleted systemId to empty Optional")
  void findById_softDeletedSystem_masksSystemId() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var entity =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, systemId, "Surviving Chronicle");
    var deletedSystem =
        new EntityBaseEntity(tenantId, systemId, owner, "System", "Retired", now, now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(deletedSystem);

    var result = dao.findById(tenantId, campaignId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().systemId()).isEmpty();
    assertThat(result.orElseThrow().title()).isEqualTo("Surviving Chronicle");
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById masks missing or non-System referenced entity to empty Optional")
  void findById_missingOrWrongTypeSystem_masksSystemId() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var entity =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, systemId, "Orphaned Ref");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId))).thenReturn(null);

    assertThat(dao.findById(tenantId, campaignId).orElseThrow().systemId()).isEmpty();

    var wrongType =
        new EntityBaseEntity(tenantId, systemId, owner, "Location", "Not system", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(wrongType);

    assertThat(dao.findById(tenantId, campaignId).orElseThrow().systemId()).isEmpty();
  }

  @Test
  @DisplayName("findById returns empty when entity_type is not Campaign")
  void findById_wrongEntityType_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    var wrongBase =
        new EntityBaseEntity(tenantId, campaignId, owner, "Event", "Event summary", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, systemId, "Wrong Type");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(wrongBase);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);

    var result = dao.findById(tenantId, campaignId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when campaign is missing, soft-deleted, or incomplete")
  void findById_missingOrDeletedOrIncomplete_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, campaignId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(null);
    assertThat(dao.findById(tenantId, campaignId)).isEmpty();

    var deletedBase =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, now);
    var campaign = new CampaignEntity(tenantId, campaignId, null, "Deleted");
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deletedBase);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(campaign);
    assertThat(dao.findById(tenantId, campaignId)).isEmpty();

    var activeBase =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(activeBase);
    when(session.find(CampaignEntity.class, entityId)).thenReturn(null);
    assertThat(dao.findById(tenantId, campaignId)).isEmpty();
  }

  @Test
  @DisplayName("listAll without systemId returns active campaigns and masks soft-deleted systems")
  void listAll_withoutFilter_returnsActiveAndMaskedCampaigns() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var query = mockObjectArrayQuery();
    when(query.setParameter(eq("tenantId"), eq(tenantId))).thenReturn(query);
    when(query.setParameter(eq("entityType"), eq("Campaign"))).thenReturn(query);
    when(query.setParameter(eq("systemEntityType"), eq("System"))).thenReturn(query);

    var secondId = UUID.randomUUID();
    var entity1 =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "One", now, now, null);
    var camp1 = new CampaignEntity(tenantId, campaignId, systemId, "Alpha Chronicle");
    var entity2 = new EntityBaseEntity(tenantId, secondId, owner, "Campaign", null, now, now, null);
    var camp2 = new CampaignEntity(tenantId, secondId, UUID.randomUUID(), "Beta Chronicle");
    when(query.getResultList())
        .thenReturn(
            List.<Object[]>of(
                new Object[] {entity1, camp1, systemId}, new Object[] {entity2, camp2, null}));

    var results = dao.listAll(tenantId, Optional.empty());

    assertThat(results).hasSize(2);
    assertThat(results.get(0).title()).isEqualTo("Alpha Chronicle");
    assertThat(results.get(0).systemId()).contains(systemId);
    assertThat(results.get(1).title()).isEqualTo("Beta Chronicle");
    assertThat(results.get(1).systemId()).isEmpty();
    verify(query, never()).setParameter(eq("systemId"), any());
    verify(tx).commit();
    assertThat(dao.listAll(tenantId)).isEqualTo(results);
  }

  @Test
  @DisplayName("listAll with systemId filters by active system and returns empty when inactive")
  void listAll_withSystemIdFilter_filtersProperly() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var query = mockObjectArrayQuery();
    when(query.setParameter(eq("tenantId"), eq(tenantId))).thenReturn(query);
    when(query.setParameter(eq("entityType"), eq("Campaign"))).thenReturn(query);
    when(query.setParameter(eq("systemEntityType"), eq("System"))).thenReturn(query);
    when(query.setParameter(eq("systemId"), eq(systemId))).thenReturn(query);

    var entity =
        new EntityBaseEntity(tenantId, campaignId, owner, "Campaign", "Desc", now, now, null);
    var campaign = new CampaignEntity(tenantId, campaignId, systemId, "Filtered Chronicle");
    when(query.getResultList())
        .thenReturn(List.<Object[]>of(new Object[] {entity, campaign, systemId}));

    var results = dao.listAll(tenantId, Optional.of(systemId));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).systemId()).contains(systemId);
    verify(query).setParameter("systemId", systemId);

    when(query.getResultList()).thenReturn(List.of());
    assertThat(dao.listAll(tenantId, Optional.of(systemId))).isEmpty();
  }

  @Test
  @DisplayName("listAll rolls back and rethrows on database failure")
  void listAll_queryError_rollsBack() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    when(session.createQuery(any(String.class), eq(Object[].class)))
        .thenThrow(new RuntimeException("Query failed"));

    assertThatThrownBy(() -> dao.listAll(tenantId, Optional.empty()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Query failed");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("Database errors trigger rollback and suppress secondary rollback errors")
  void rollbackOnError_withSuppression() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));
    doThrow(new RuntimeException("Rollback error")).when(tx).rollback();

    assertThatThrownBy(() -> dao.findById(tenantId, campaignId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("DB error")
        .satisfies(
            e -> {
              assertThat(e.getSuppressed()).hasSize(1);
              assertThat(e.getSuppressed()[0]).hasMessage("Rollback error");
            });
    verify(tx).rollback();
  }

  @Test
  @DisplayName("Unsupported unscoped DAO methods throw UnsupportedOperationException")
  void unsupportedMethods_throwException() {
    assertThatThrownBy(() -> dao.list()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> dao.findById(UUID.randomUUID()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private Query<Object[]> mockObjectArrayQuery() {
    Query<Object[]> query = mock();
    when(session.createQuery(any(String.class), eq(Object[].class))).thenReturn(query);
    return query;
  }

  private void mockTenantConfigQuery() {
    NativeQuery<String> configQuery = mock();
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());
  }
}
