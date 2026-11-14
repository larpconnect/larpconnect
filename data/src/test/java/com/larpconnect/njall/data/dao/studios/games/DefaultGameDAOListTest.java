package com.larpconnect.njall.data.dao.studios.games;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.studios.events.EventEntity;
import com.larpconnect.njall.data.domain.studios.EventType;
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

final class DefaultGameDAOListTest {

  private Session session;
  private DefaultGameDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final UUID campaignId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    var sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultGameDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("listAll(tenantId) executes ordered query and masks deleted campaigns")
  void listAll_noFilters_returnsGamesAndMasksCampaign() {
    var tx = beginTxWithTenant();
    var id1 = UUID.randomUUID();
    var id2 = UUID.randomUUID();
    var query =
        mockListQuery(
            List.of(
                new Object[] {
                  new EntityBaseEntity(tenantId, id1, actorId, "Game", "First", now, now, null),
                  new EventEntity(tenantId, id1, EventType.GAME, null, "Session One", now, null),
                  new GameEntity(tenantId, id1, systemId, campaignId),
                  campaignId
                },
                new Object[] {
                  new EntityBaseEntity(tenantId, id2, actorId, "Game", null, now, now, null),
                  new EventEntity(tenantId, id2, EventType.GAME, null, "One-Shot", null, null),
                  new GameEntity(tenantId, id2, systemId, campaignId),
                  null
                }));

    var games = dao.listAll(tenantId);

    assertThat(games).hasSize(2);
    assertThat(games.get(0).campaignId()).contains(campaignId);
    assertThat(games.get(1).campaignId()).isEmpty();
    assertThat(games.get(1).systemId()).isEqualTo(systemId);
    verify(query).setParameter("tenantId", tenantId);
    verify(query).setParameter("entityType", "Game");
    verify(query).setParameter("campaignEntityType", "Campaign");
    verify(tx).commit();
  }

  @Test
  @DisplayName("listAll applies systemId and campaignId filters when present")
  void listAll_withFilters_bindsMatchingParameters() {
    var tx1 = beginTxWithTenant();
    var sysQuery = mockListQuery(List.of());
    when(sysQuery.setParameter("systemId", systemId)).thenReturn(sysQuery);
    assertThat(dao.listAll(tenantId, Optional.of(systemId), Optional.empty())).isEmpty();
    verify(sysQuery).setParameter("systemId", systemId);
    verify(tx1).commit();

    var tx2 = beginTxWithTenant();
    var campQuery = mockListQuery(List.of());
    when(campQuery.setParameter("campaignId", campaignId)).thenReturn(campQuery);
    assertThat(dao.listAll(tenantId, Optional.empty(), Optional.of(campaignId))).isEmpty();
    verify(campQuery).setParameter("campaignId", campaignId);
    verify(tx2).commit();

    var tx3 = beginTxWithTenant();
    var bothQuery = mockListQuery(List.of());
    when(bothQuery.setParameter("systemId", systemId)).thenReturn(bothQuery);
    when(bothQuery.setParameter("campaignId", campaignId)).thenReturn(bothQuery);
    assertThat(dao.listAll(tenantId, Optional.of(systemId), Optional.of(campaignId))).isEmpty();
    verify(bothQuery).setParameter("systemId", systemId);
    verify(bothQuery).setParameter("campaignId", campaignId);
    verify(tx3).commit();
  }

  @Test
  @DisplayName("listAll rolls back transaction when query fails")
  void listAll_exception_rollsBack() {
    var tx = beginTxWithTenant();
    when(session.createQuery(any(String.class), eq(Object[].class)))
        .thenThrow(new IllegalStateException("Query failed"));

    assertThatThrownBy(() -> dao.listAll(tenantId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Query failed");
    verify(tx).rollback();
  }

  private Query<Object[]> mockListQuery(List<Object[]> results) {
    Query<Object[]> query = mock();
    when(session.createQuery(any(String.class), eq(Object[].class))).thenReturn(query);
    when(query.setParameter("tenantId", tenantId)).thenReturn(query);
    when(query.setParameter("entityType", "Game")).thenReturn(query);
    when(query.setParameter("campaignEntityType", "Campaign")).thenReturn(query);
    when(query.getResultList()).thenReturn(results);
    return query;
  }

  private Transaction beginTxWithTenant() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    NativeQuery<String> configQuery = mock();
    when(session.createNativeQuery(any(String.class), eq(String.class))).thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), eq(tenantId.toString()))).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn("ok");
    return tx;
  }
}
