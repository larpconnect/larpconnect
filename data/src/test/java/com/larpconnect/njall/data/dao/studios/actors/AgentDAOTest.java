package com.larpconnect.njall.data.dao.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

final class AgentDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private Transaction tx;
  private DefaultAgentDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-14T12:00:00Z");
  private final Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    tx = mock(Transaction.class);
    when(sessionFactory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    dao = new DefaultAgentDAO(() -> sessionFactory, fixedClock);
  }

  @Test
  @DisplayName("findById returns active Agent with endpoints and collections")
  void findById_success() {
    var entityId = new EntityId(tenantId, actorId);
    var baseEntity =
        new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Bio text", now, now, null);
    var agentEntity = new AgentEntity(tenantId, actorId, "valkyrie");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(agentEntity);

    var endpointsList =
        ImmutableList.<Object[]>of(new Object[] {"sharedInbox", "https://example.com/inbox"});
    mockEndpointsQuery(endpointsList);
    var inboxUuid = UUID.randomUUID();
    var outboxUuid = UUID.randomUUID();
    mockCollectionIdQueries(List.of(inboxUuid), List.of(outboxUuid));

    var result = dao.findById(tenantId, actorId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(actorId);
    assertThat(result.orElseThrow().generator()).isEqualTo(ownerId);
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
    var entityId = new EntityId(tenantId, actorId);
    var baseEntity =
        new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Bio text", now, now, now);
    var agentEntity = new AgentEntity(tenantId, actorId, "valkyrie");

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
    NativeQuery<Object> query = mock();
    when(session.createNativeQuery(
            contains("lower(preferred_username) = lower(:username)"), eq(Object.class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);

    when(query.getResultList()).thenReturn(List.of(1));
    assertThat(dao.existsByPreferredUsername(tenantId, "valkyrie", Optional.empty())).isTrue();

    when(query.getResultList()).thenReturn(List.of());
    assertThat(dao.existsByPreferredUsername(tenantId, "valkyrie", Optional.empty())).isFalse();

    NativeQuery<Object> excludeQuery = mock();
    when(session.createNativeQuery(contains("AND id != :excludeId"), eq(Object.class)))
        .thenReturn(excludeQuery);
    when(excludeQuery.setParameter(any(String.class), any())).thenReturn(excludeQuery);
    when(excludeQuery.getResultList()).thenReturn(List.of(1));

    assertThat(dao.existsByPreferredUsername(tenantId, "valkyrie", Optional.of(actorId))).isTrue();
  }

  @Test
  @DisplayName("create persists base entity, agent, and collections with explicit owner")
  void create_withExplicitOwner_success() {

    var ownerEntity =
        new EntityBaseEntity(tenantId, ownerId, ownerId, "Actor", "Owner Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, ownerId)))
        .thenReturn(ownerEntity);
    mockCheckUsernameQuery(List.of());
    mockProvisionQueries();

    var agent = dao.create(tenantId, Optional.of(ownerId), "valkyrie", Optional.of("Summary"));

    assertThat(agent.generator()).isEqualTo(ownerId);
    assertThat(agent.preferredUsername()).isEqualTo("valkyrie");
    assertThat(agent.summary()).contains("Summary");
    assertThat(agent.createdOn()).isEqualTo(now);
    assertThat(agent.updatedOn()).isEqualTo(now);
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(AgentEntity.class));
    verify(tx).commit();

    mockFindByUsernameQuery(List.of());
    assertThat(new DefaultAgentDAO(() -> sessionFactory).findByPreferredUsername(tenantId, "none"))
        .isEmpty();
  }

  @Test
  @DisplayName("create successfully persists self-owned actor when owner is empty")
  void create_selfOwned_success() {
    mockCheckUsernameQuery(List.of());
    mockProvisionQueries();

    var agent = dao.create(tenantId, Optional.empty(), "valkyrie", Optional.of("Summary"));

    assertThat(agent.generator()).isEqualTo(agent.id());
    assertThat(agent.preferredUsername()).isEqualTo("valkyrie");
    assertThat(agent.summary()).contains("Summary");
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(AgentEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("create throws when owner does not exist")
  void create_ownerNotFound_throws() {
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, ownerId))).thenReturn(null);

    assertThatThrownBy(
            () -> dao.create(tenantId, Optional.of(ownerId), "valkyrie", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when owner is soft-deleted")
  void create_ownerDeleted_throws() {
    var deletedOwner =
        new EntityBaseEntity(tenantId, ownerId, ownerId, "Actor", "Bio", now, now, now);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, ownerId)))
        .thenReturn(deletedOwner);

    assertThatThrownBy(
            () -> dao.create(tenantId, Optional.of(ownerId), "valkyrie", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when owner is not an Actor")
  void create_ownerNotActor_throws() {
    var nonActorOwner =
        new EntityBaseEntity(tenantId, ownerId, ownerId, "Event", "Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, ownerId)))
        .thenReturn(nonActorOwner);

    assertThatThrownBy(
            () -> dao.create(tenantId, Optional.of(ownerId), "valkyrie", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when username is already taken")
  void create_usernameTaken_throws() {
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, ownerId)))
        .thenReturn(
            new EntityBaseEntity(tenantId, ownerId, ownerId, "Actor", "Bio", now, now, null));
    mockCheckUsernameQuery(List.of(1));

    assertThatThrownBy(
            () -> dao.create(tenantId, Optional.of(ownerId), "valkyrie", Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("update modifies username and summary")
  void update_success() {

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity =
        new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Old Bio", now, now, null);
    var agentEntity = new AgentEntity(tenantId, actorId, "old_name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(agentEntity);
    mockEndpointsQuery(List.of());
    mockCollectionIdQueries(List.of(UUID.randomUUID()), List.of(UUID.randomUUID()));

    NativeQuery<Object> excludeQuery = mock();
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
    var baseEntity =
        new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Old Bio", now, now, null);
    var agentEntity = new AgentEntity(tenantId, actorId, "old_name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(agentEntity);

    NativeQuery<Object> excludeQuery = mock();
    when(session.createNativeQuery(contains("AND id != :excludeId"), eq(Object.class)))
        .thenReturn(excludeQuery);
    when(excludeQuery.setParameter(any(String.class), any())).thenReturn(excludeQuery);
    when(excludeQuery.getResultList()).thenReturn(List.of(1));

    assertThatThrownBy(
            () -> dao.update(tenantId, actorId, Optional.of("new_name"), Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("delete soft-deletes actor and cascades to collections")
  void delete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var baseEntity =
        new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Bio", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);

    NativeQuery<Object> cascadeQuery = mock();
    when(session.createNativeQuery(contains("SET deleted_on = :now"), eq(Object.class)))
        .thenReturn(cascadeQuery);
    when(cascadeQuery.setParameter(any(String.class), any())).thenReturn(cascadeQuery);
    when(cascadeQuery.executeUpdate()).thenReturn(2);

    assertThat(dao.delete(tenantId, actorId)).isTrue();
    assertThat(baseEntity.getDeletedOn()).isEqualTo(now);
    assertThat(baseEntity.getUpdatedOn()).isEqualTo(now);
    verify(tx).commit();
  }

  @Test
  @DisplayName("update returns empty when actor is not found, deleted, or missing subtype")
  void update_notFoundOrDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, actorId);
    var deletedEntity =
        new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Bio", now, now, now);
    var agentEntity = new AgentEntity(tenantId, actorId, "old");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deletedEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(agentEntity);
    assertThat(dao.update(tenantId, actorId, Optional.empty(), Optional.empty())).isEmpty();

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    assertThat(dao.update(tenantId, actorId, Optional.empty(), Optional.empty())).isEmpty();

    var activeEntity =
        new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(activeEntity);
    when(session.find(AgentEntity.class, entityId)).thenReturn(null);
    assertThat(dao.update(tenantId, actorId, Optional.empty(), Optional.empty())).isEmpty();
  }

  @Test
  @DisplayName("update with empty optionals leaves properties unchanged")
  void update_emptyOptionals() {
    var entityId = new EntityId(tenantId, actorId);
    var baseEntity =
        new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Old Bio", now, now, null);
    var agentEntity = new AgentEntity(tenantId, actorId, "old_name");

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
    var entityId = new EntityId(tenantId, actorId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    assertThat(dao.delete(tenantId, actorId)).isFalse();

    var deleted = new EntityBaseEntity(tenantId, actorId, ownerId, "Actor", "Bio", now, now, now);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deleted);
    assertThat(dao.delete(tenantId, actorId)).isFalse();
  }

  @Test
  @DisplayName("DAO methods rollback and rethrow on transaction error")
  void daoMethods_rollbackOnError() {
    when(session.find(eq(EntityBaseEntity.class), any()))
        .thenThrow(new RuntimeException("DB error"));
    assertThatThrownBy(() -> dao.findById(tenantId, actorId)).isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("findByPreferredUsername resolves actor case-insensitively in a single session")
  void findByPreferredUsername_success() {
    mockFindByUsernameQuery(List.of(actorId));
    mockAgentEntities(actorId, "valkyrie");

    var result = dao.findByPreferredUsername(tenantId, "VALKYRIE");
    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(actorId);
    verify(sessionFactory).openSession();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findByPreferredUsername returns empty when username not found")
  void findByPreferredUsername_notFound() {
    mockFindByUsernameQuery(List.of());

    assertThat(dao.findByPreferredUsername(tenantId, "unknown")).isEmpty();
  }

  @Test
  @DisplayName("findByIdentifier resolves UUID directly and username fallback in a single session")
  void findByIdentifier_scenarios() {
    mockAgentEntities(actorId, "valkyrie");
    mockFindByUsernameQuery(List.of(actorId));

    assertThat(dao.findByIdentifier(tenantId, actorId.toString())).isPresent();
    assertThat(dao.findByIdentifier(tenantId, "valkyrie")).isPresent();

    mockFindByUsernameQuery(List.of());
    when(session.find(eq(EntityBaseEntity.class), any())).thenReturn(null);
    assertThat(dao.findByIdentifier(tenantId, UUID.randomUUID().toString())).isEmpty();
    verify(sessionFactory, times(3)).openSession();

    when(session.find(eq(EntityBaseEntity.class), any()))
        .thenThrow(new RuntimeException("DB lookup error"));
    assertThatThrownBy(() -> dao.findByIdentifier(tenantId, UUID.randomUUID().toString()))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  private void mockAgentEntities(UUID id, String username) {
    var entityId = new EntityId(tenantId, id);
    var baseEntity = new EntityBaseEntity(tenantId, id, ownerId, "Actor", "Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(baseEntity);
    when(session.find(AgentEntity.class, entityId))
        .thenReturn(new AgentEntity(tenantId, id, username));
    mockEndpointsQuery(List.of());
    mockCollectionIdQueries(List.of(), List.of());
  }

  private void mockFindByUsernameQuery(List<UUID> result) {
    NativeQuery<UUID> query = mock();
    when(session.createNativeQuery(contains("FROM njall_users.actors a"), eq(UUID.class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(result);
  }

  private void mockTenantConfigQuery() {
    NativeQuery<String> query = mock();
    when(session.createNativeQuery(contains("set_config"), eq(String.class))).thenReturn(query);
    when(query.setParameter(eq("tenantId"), any())).thenReturn(query);
    when(query.getSingleResult()).thenReturn("tenant-set");
  }

  private void mockCheckUsernameQuery(List<Object> result) {
    NativeQuery<Object> query = mock();
    when(session.createNativeQuery(contains("SELECT 1 FROM njall_users.actors"), eq(Object.class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(result);
  }

  private void mockProvisionQueries() {
    NativeQuery<Object> q1 = mock();
    when(session.createNativeQuery(contains("INSERT INTO njall_users.entities"), eq(Object.class)))
        .thenReturn(q1);
    when(q1.setParameter(any(String.class), any())).thenReturn(q1);
    when(q1.executeUpdate()).thenReturn(1);
    NativeQuery<Object> q2 = mock();
    when(session.createNativeQuery(
            contains("INSERT INTO njall_users.collections"), eq(Object.class)))
        .thenReturn(q2);
    when(q2.setParameter(any(String.class), any())).thenReturn(q2);
    when(q2.executeUpdate()).thenReturn(1);
  }

  private void mockCollectionIdQueries(List<UUID> inboxResult, List<UUID> outboxResult) {
    NativeQuery<UUID> q1 = mock();
    when(q1.setParameter(any(String.class), any())).thenReturn(q1);
    when(q1.getResultList()).thenReturn(inboxResult);
    NativeQuery<UUID> q2 = mock();
    when(q2.setParameter(any(String.class), any())).thenReturn(q2);
    when(q2.getResultList()).thenReturn(outboxResult);

    when(session.createNativeQuery(contains("AND name = :name"), eq(UUID.class)))
        .thenReturn(q1)
        .thenReturn(q2);
  }

  private void mockEndpointsQuery(List<Object[]> result) {
    NativeQuery<Object[]> query = mock();
    when(session.createNativeQuery(
            contains("FROM njall_users.actor_endpoints"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(result);
  }
}
