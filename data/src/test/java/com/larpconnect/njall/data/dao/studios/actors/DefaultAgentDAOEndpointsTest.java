package com.larpconnect.njall.data.dao.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class DefaultAgentDAOEndpointsTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultAgentDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultAgentDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("addEndpoint and removeEndpoint update endpoints table")
  void addAndRemoveEndpoint_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity =
        new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);

    var endpointId = new AgentEndpointId(tenantId, actorId, "sharedInbox");
    var existingEndpoint =
        new AgentEndpointEntity(tenantId, actorId, "sharedInbox", "https://old.com");
    when(session.find(AgentEndpointEntity.class, endpointId)).thenReturn(existingEndpoint);

    assertThat(dao.addEndpoint(tenantId, actorId, "sharedInbox", "https://new.com")).isTrue();
    assertThat(existingEndpoint.getEndpoint()).isEqualTo("https://new.com");

    when(session.find(AgentEndpointEntity.class, endpointId)).thenReturn(null);
    assertThat(dao.addEndpoint(tenantId, actorId, "sharedInbox", "https://new.com")).isTrue();
    verify(session).persist(any(AgentEndpointEntity.class));

    when(session.find(AgentEndpointEntity.class, endpointId)).thenReturn(existingEndpoint);
    assertThat(dao.removeEndpoint(tenantId, actorId, "sharedInbox")).isTrue();
    verify(session).remove(existingEndpoint);

    when(session.find(AgentEndpointEntity.class, endpointId)).thenReturn(null);
    assertThat(dao.removeEndpoint(tenantId, actorId, "sharedInbox")).isTrue();
  }

  @Test
  @DisplayName("endpoint mutations return false when actor not found or deleted")
  void endpointMutations_notFoundOrDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    assertThat(dao.addEndpoint(tenantId, actorId, "inbox", "https://ep.com")).isFalse();
    assertThat(dao.removeEndpoint(tenantId, actorId, "inbox")).isFalse();

    var deleted = new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Bio", now, now, now);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deleted);
    assertThat(dao.addEndpoint(tenantId, actorId, "inbox", "https://ep.com")).isFalse();
    assertThat(dao.removeEndpoint(tenantId, actorId, "inbox")).isFalse();
  }

  @Test
  @DisplayName("findInboxId and findOutboxId return collection UUIDs")
  void findInboxAndOutboxId_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity =
        new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);

    var inboxUuid = UUID.randomUUID();
    var outboxUuid = UUID.randomUUID();
    mockCollectionIdQueries(List.of(inboxUuid), List.of(outboxUuid));

    assertThat(dao.findInboxId(tenantId, actorId)).contains(inboxUuid);
    assertThat(dao.findOutboxId(tenantId, actorId)).contains(outboxUuid);
  }

  @Test
  @DisplayName("findInboxId and findOutboxId return empty when actor not found or deleted")
  void findInboxAndOutboxId_notFoundOrDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    assertThat(dao.findInboxId(tenantId, actorId)).isEmpty();
    assertThat(dao.findOutboxId(tenantId, actorId)).isEmpty();

    var deleted = new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Bio", now, now, now);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deleted);
    assertThat(dao.findInboxId(tenantId, actorId)).isEmpty();
    assertThat(dao.findOutboxId(tenantId, actorId)).isEmpty();
  }

  @Test
  @DisplayName("findInboxId and findOutboxId rollback and throw on error")
  void findInboxAndOutboxId_exceptionRollsBack() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    when(session.find(EntityBaseEntity.class, entityId))
        .thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.findInboxId(tenantId, actorId))
        .isInstanceOf(RuntimeException.class);
    assertThatThrownBy(() -> dao.findOutboxId(tenantId, actorId))
        .isInstanceOf(RuntimeException.class);
    verify(tx, Mockito.atLeastOnce()).rollback();
  }

  private void mockTenantConfigQuery() {
    @SuppressWarnings("unchecked")
    NativeQuery<String> query = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class))).thenReturn(query);
    when(query.setParameter(eq("tenantId"), any())).thenReturn(query);
    when(query.getSingleResult()).thenReturn("tenant-set");
  }

  private void mockCollectionIdQueries(List<UUID> inboxResult, List<UUID> outboxResult) {
    @SuppressWarnings("unchecked")
    NativeQuery<UUID> q1 = mock(NativeQuery.class);
    when(q1.setParameter(any(String.class), any())).thenReturn(q1);
    when(q1.getResultList()).thenReturn(inboxResult);

    @SuppressWarnings("unchecked")
    NativeQuery<UUID> q2 = mock(NativeQuery.class);
    when(q2.setParameter(any(String.class), any())).thenReturn(q2);
    when(q2.getResultList()).thenReturn(outboxResult);

    when(session.createNativeQuery(
            contains("WHERE tenant_id = :tenantId AND owner_id = :actorId AND name = :name"),
            eq(UUID.class)))
        .thenReturn(q1)
        .thenReturn(q2);
  }
}
