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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultAgentDAOTest {

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
  @DisplayName("findById returns active Agent with endpoints and collections")
  void findById_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity = new EntityBaseEntity(tenantId, actorId, "Actor", "Bio text", now, now, null);
    var agentEntity = new AgentEntity(tenantId, actorId, ownerId, "valkyrie");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(agentEntity);

    var endpointsList = new ArrayList<Object[]>();
    endpointsList.add(new Object[] {"sharedInbox", "https://example.com/inbox"});
    mockEndpointsQuery(endpointsList);
    var inboxUuid = UUID.randomUUID();
    var outboxUuid = UUID.randomUUID();
    mockCollectionIdQueries(List.of(inboxUuid), List.of(outboxUuid));

    var result = dao.findById(tenantId, actorId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(actorId);
    assertThat(result.orElseThrow().ownerId()).isEqualTo(ownerId);
    assertThat(result.orElseThrow().preferredUsername()).isEqualTo("valkyrie");
    assertThat(result.orElseThrow().endpoints())
        .containsEntry("sharedInbox", "https://example.com/inbox");
    assertThat(result.orElseThrow().inboxId()).isEqualTo(inboxUuid);
    assertThat(result.orElseThrow().outboxId()).isEqualTo(outboxUuid);
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when actor is soft-deleted or not found")
  void findById_notFoundOrDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity = new EntityBaseEntity(tenantId, actorId, "Actor", "Bio text", now, now, now);
    var agentEntity = new AgentEntity(tenantId, actorId, ownerId, "valkyrie");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(agentEntity);
    assertThat(dao.findById(tenantId, actorId)).isEmpty();

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(null);
    assertThat(dao.findById(tenantId, actorId)).isEmpty();

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    assertThat(dao.findById(tenantId, actorId)).isEmpty();
  }

  @Test
  @DisplayName("existsByPreferredUsername detects existing and non-existing usernames")
  void existsByPreferredUsername_scenarios() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    @SuppressWarnings("unchecked")
    NativeQuery<Object> query = mock(NativeQuery.class);
    when(session.createNativeQuery(
            contains("lower(preferred_username) = lower(:username)"), eq(Object.class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);

    when(query.getResultList()).thenReturn(List.of(1));
    assertThat(dao.existsByPreferredUsername(tenantId, "valkyrie", Optional.empty())).isTrue();

    when(query.getResultList()).thenReturn(List.of());
    assertThat(dao.existsByPreferredUsername(tenantId, "valkyrie", Optional.empty())).isFalse();

    @SuppressWarnings("unchecked")
    NativeQuery<Object> excludeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("AND id != :excludeId"), eq(Object.class)))
        .thenReturn(excludeQuery);
    when(excludeQuery.setParameter(any(String.class), any())).thenReturn(excludeQuery);
    when(excludeQuery.getResultList()).thenReturn(List.of(1));

    assertThat(dao.existsByPreferredUsername(tenantId, "valkyrie", Optional.of(actorId))).isTrue();
  }

  @Test
  @DisplayName("create successfully persists base entity, agent entity, and paired collections")
  void create_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    mockCheckOwnerQuery(List.of(1));
    mockCheckUsernameQuery(List.of());
    mockProvisionQueries();

    var agent = dao.create(tenantId, ownerId, "valkyrie", Optional.of("Summary"));

    assertThat(agent.ownerId()).isEqualTo(ownerId);
    assertThat(agent.preferredUsername()).isEqualTo("valkyrie");
    assertThat(agent.summary()).contains("Summary");
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(AgentEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("create throws when owner does not exist")
  void create_ownerNotFound_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockCheckOwnerQuery(List.of());

    assertThatThrownBy(() -> dao.create(tenantId, ownerId, "valkyrie", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when username is already taken")
  void create_usernameTaken_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockCheckOwnerQuery(List.of(1));
    mockCheckUsernameQuery(List.of(1));

    assertThatThrownBy(() -> dao.create(tenantId, ownerId, "valkyrie", Optional.empty()))
        .isInstanceOf(IllegalStateException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("update modifies username and summary")
  void update_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity = new EntityBaseEntity(tenantId, actorId, "Actor", "Old Bio", now, now, null);
    var agentEntity = new AgentEntity(tenantId, actorId, ownerId, "old_name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(agentEntity);
    mockEndpointsQuery(List.of());
    mockCollectionIdQueries(List.of(UUID.randomUUID()), List.of(UUID.randomUUID()));

    @SuppressWarnings("unchecked")
    NativeQuery<Object> excludeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("AND id != :excludeId"), eq(Object.class)))
        .thenReturn(excludeQuery);
    when(excludeQuery.setParameter(any(String.class), any())).thenReturn(excludeQuery);
    when(excludeQuery.getResultList()).thenReturn(List.of());

    var result = dao.update(tenantId, actorId, Optional.of("new_name"), Optional.of("New Bio"));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().preferredUsername()).isEqualTo("new_name");
    assertThat(result.orElseThrow().summary()).contains("New Bio");
    verify(tx).commit();
  }

  @Test
  @DisplayName("update throws when updated username conflicts with another actor")
  void update_usernameConflict_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity = new EntityBaseEntity(tenantId, actorId, "Actor", "Old Bio", now, now, null);
    var agentEntity = new AgentEntity(tenantId, actorId, ownerId, "old_name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(agentEntity);

    @SuppressWarnings("unchecked")
    NativeQuery<Object> excludeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("AND id != :excludeId"), eq(Object.class)))
        .thenReturn(excludeQuery);
    when(excludeQuery.setParameter(any(String.class), any())).thenReturn(excludeQuery);
    when(excludeQuery.getResultList()).thenReturn(List.of(1));

    assertThatThrownBy(
            () -> dao.update(tenantId, actorId, Optional.of("new_name"), Optional.empty()))
        .isInstanceOf(IllegalStateException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("delete soft-deletes actor and cascades to collections")
  void delete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity = new EntityBaseEntity(tenantId, actorId, "Actor", "Bio", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);

    @SuppressWarnings("unchecked")
    NativeQuery<Object> cascadeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SET deleted_on = :now"), eq(Object.class)))
        .thenReturn(cascadeQuery);
    when(cascadeQuery.setParameter(any(String.class), any())).thenReturn(cascadeQuery);
    when(cascadeQuery.executeUpdate()).thenReturn(2);

    assertThat(dao.delete(tenantId, actorId)).isTrue();
    assertThat(baseEntity.getDeletedOn()).isNotNull();
    verify(tx).commit();
  }

  @Test
  @DisplayName("update returns empty when actor is not found, deleted, or missing subtype")
  void update_notFoundOrDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var deletedEntity = new EntityBaseEntity(tenantId, actorId, "Actor", "Bio", now, now, now);
    var agentEntity = new AgentEntity(tenantId, actorId, ownerId, "old");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deletedEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(agentEntity);
    assertThat(dao.update(tenantId, actorId, Optional.empty(), Optional.empty())).isEmpty();

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    assertThat(dao.update(tenantId, actorId, Optional.empty(), Optional.empty())).isEmpty();

    var activeEntity = new EntityBaseEntity(tenantId, actorId, "Actor", "Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(activeEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(null);
    assertThat(dao.update(tenantId, actorId, Optional.empty(), Optional.empty())).isEmpty();
  }

  @Test
  @DisplayName("update with empty optionals leaves properties unchanged")
  void update_emptyOptionals() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity = new EntityBaseEntity(tenantId, actorId, "Actor", "Old Bio", now, now, null);
    var agentEntity = new AgentEntity(tenantId, actorId, ownerId, "old_name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(agentEntity);
    mockEndpointsQuery(List.of());
    mockCollectionIdQueries(List.of(), List.of());

    var result = dao.update(tenantId, actorId, Optional.empty(), Optional.empty());
    assertThat(result).isPresent();
    assertThat(result.orElseThrow().inboxId()).isEqualTo(actorId);
    assertThat(result.orElseThrow().outboxId()).isEqualTo(actorId);
  }

  @Test
  @DisplayName("delete returns false when actor is not found or already deleted")
  void delete_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    assertThat(dao.delete(tenantId, actorId)).isFalse();

    var deleted = new EntityBaseEntity(tenantId, actorId, "Actor", "Bio", now, now, now);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deleted);
    assertThat(dao.delete(tenantId, actorId)).isFalse();
  }

  @Test
  @DisplayName("addEndpoint and removeEndpoint perform upsert and deletion")
  void endpointMutations_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity = new EntityBaseEntity(tenantId, actorId, "Actor", "Bio", now, now, null);
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

    var deleted = new EntityBaseEntity(tenantId, actorId, "Actor", "Bio", now, now, now);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deleted);
    assertThat(dao.addEndpoint(tenantId, actorId, "inbox", "https://ep.com")).isFalse();
    assertThat(dao.removeEndpoint(tenantId, actorId, "inbox")).isFalse();
  }

  @Test
  @DisplayName("DAO methods rollback and rethrow on transaction error")
  void daoMethods_rollbackOnError() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    when(session.find(eq(EntityBaseEntity.class), any()))
        .thenThrow(new RuntimeException("DB error"));
    assertThatThrownBy(() -> dao.findById(tenantId, actorId)).isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("findInboxId and findOutboxId return collection UUIDs")
  void findInboxAndOutboxId_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var inboxUuid = UUID.randomUUID();
    var outboxUuid = UUID.randomUUID();
    mockCollectionIdQueries(List.of(inboxUuid), List.of(outboxUuid));

    assertThat(dao.findInboxId(tenantId, actorId)).contains(inboxUuid);
    assertThat(dao.findOutboxId(tenantId, actorId)).contains(outboxUuid);
  }

  private void mockTenantConfigQuery() {
    @SuppressWarnings("unchecked")
    NativeQuery<String> query = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class))).thenReturn(query);
    when(query.setParameter(eq("tenantId"), any())).thenReturn(query);
    when(query.getSingleResult()).thenReturn("tenant-set");
  }

  private void mockCheckOwnerQuery(List<Object> result) {
    @SuppressWarnings("unchecked")
    NativeQuery<Object> query = mock(NativeQuery.class);
    when(session.createNativeQuery(
            contains("WHERE tenant_id = :tenantId AND id = :ownerId"), eq(Object.class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(result);
  }

  private void mockCheckUsernameQuery(List<Object> result) {
    @SuppressWarnings("unchecked")
    NativeQuery<Object> query = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SELECT 1 FROM njall_users.actors"), eq(Object.class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(result);
  }

  private void mockProvisionQueries() {
    @SuppressWarnings("unchecked")
    NativeQuery<Object> q1 = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("INSERT INTO njall_users.entities"), eq(Object.class)))
        .thenReturn(q1);
    when(q1.setParameter(any(String.class), any())).thenReturn(q1);
    when(q1.executeUpdate()).thenReturn(1);

    @SuppressWarnings("unchecked")
    NativeQuery<Object> q2 = mock(NativeQuery.class);
    when(session.createNativeQuery(
            contains("INSERT INTO njall_users.collections"), eq(Object.class)))
        .thenReturn(q2);
    when(q2.setParameter(any(String.class), any())).thenReturn(q2);
    when(q2.executeUpdate()).thenReturn(1);
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

  private void mockEndpointsQuery(List<Object[]> result) {
    @SuppressWarnings("unchecked")
    NativeQuery<Object[]> query = mock(NativeQuery.class);
    when(session.createNativeQuery(
            contains("FROM njall_users.actor_endpoints"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(result);
  }
}
