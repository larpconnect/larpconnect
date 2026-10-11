package com.larpconnect.njall.data.dao.studios.collections;

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

final class DefaultCollectionDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultCollectionDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID collectionId = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultCollectionDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById returns active Collection with item IDs")
  void findById_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, "OrderedCollection", "Summary", now, now, null);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Cast");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);

    var itemId = UUID.randomUUID();
    @SuppressWarnings("unchecked")
    Query<UUID> itemQuery = mock(Query.class);
    when(session.createQuery(contains("SELECT ci.refersToId"), eq(UUID.class)))
        .thenReturn(itemQuery);
    when(itemQuery.setParameter("tenantId", tenantId)).thenReturn(itemQuery);
    when(itemQuery.setParameter("collectionId", collectionId)).thenReturn(itemQuery);
    when(itemQuery.getResultList()).thenReturn(List.of(itemId));

    var result = dao.findById(tenantId, collectionId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(collectionId);
    assertThat(result.orElseThrow().ownerId()).isEqualTo(ownerId);
    assertThat(result.orElseThrow().name()).isEqualTo("Cast");
    assertThat(result.orElseThrow().totalItems()).isEqualTo(1);
    assertThat(result.orElseThrow().itemIds()).containsExactly(itemId);
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when collection is soft deleted")
  void findById_softDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(tenantId, collectionId, "OrderedCollection", "Summary", now, now, now);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Cast");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);

    var result = dao.findById(tenantId, collectionId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when collection is not found")
  void findById_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result = dao.findById(tenantId, collectionId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findExpandedItems returns ordered list of ExpandedEntity")
  void findExpandedItems_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    @SuppressWarnings("unchecked")
    NativeQuery<Object[]> nativeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(
            contains("FROM njall_users.collection_items ci"), eq(Object[].class)))
        .thenReturn(nativeQuery);
    when(nativeQuery.setParameter("tenantId", tenantId)).thenReturn(nativeQuery);
    when(nativeQuery.setParameter("collectionId", collectionId)).thenReturn(nativeQuery);

    var refId = UUID.randomUUID();
    Object[] row = new Object[] {refId, "Individual", "Lead bard", "Alice"};
    when(nativeQuery.getResultList()).thenReturn(List.<Object[]>of(row));

    var items = dao.findExpandedItems(tenantId, collectionId);

    assertThat(items).hasSize(1);
    assertThat(items.get(0).id()).isEqualTo(refId);
    assertThat(items.get(0).type()).isEqualTo("Individual");
    assertThat(items.get(0).name()).contains("Alice");
    assertThat(items.get(0).summary()).contains("Lead bard");
    verify(tx).commit();
  }

  @Test
  @DisplayName("existsByOwnerAndName returns true when matching active collection exists")
  void existsByOwnerAndName_true() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    @SuppressWarnings("unchecked")
    Query<Long> query = mock(Query.class);
    when(session.createQuery(contains("SELECT COUNT(c) FROM CollectionEntity c"), eq(Long.class)))
        .thenReturn(query);
    when(query.setParameter("tenantId", tenantId)).thenReturn(query);
    when(query.setParameter("ownerId", ownerId)).thenReturn(query);
    when(query.setParameter("name", "Cast")).thenReturn(query);
    when(query.getSingleResult()).thenReturn(1L);

    var exists = dao.existsByOwnerAndName(tenantId, ownerId, "Cast", Optional.empty());

    assertThat(exists).isTrue();
    verify(tx).commit();
  }

  @Test
  @DisplayName("existsByOwnerAndName with excludeId binds excludeId")
  void existsByOwnerAndName_withExcludeId() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var excludeId = UUID.randomUUID();
    @SuppressWarnings("unchecked")
    Query<Long> query = mock(Query.class);
    when(session.createQuery(contains("AND c.id != :excludeId"), eq(Long.class))).thenReturn(query);
    when(query.setParameter("tenantId", tenantId)).thenReturn(query);
    when(query.setParameter("ownerId", ownerId)).thenReturn(query);
    when(query.setParameter("name", "Cast")).thenReturn(query);
    when(query.setParameter("excludeId", excludeId)).thenReturn(query);
    when(query.getSingleResult()).thenReturn(0L);

    var exists = dao.existsByOwnerAndName(tenantId, ownerId, "Cast", Optional.of(excludeId));

    assertThat(exists).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("create persists base and collection entities")
  void create_persistsEntities() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var collection = dao.create(tenantId, ownerId, "Cast", Optional.of("Summary"));

    assertThat(collection.name()).isEqualTo("Cast");
    assertThat(collection.ownerId()).isEqualTo(ownerId);
    assertThat(collection.summary()).contains("Summary");
    assertThat(collection.totalItems()).isEqualTo(0);
    assertThat(collection.itemIds()).isEmpty();
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(CollectionEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates name and summary and returns updated collection")
  void patch_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, "OrderedCollection", "Old summary", now, now, null);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);

    mockEmptyItemQuery();

    var result =
        dao.patch(tenantId, collectionId, Optional.of("New Name"), Optional.of("New summary"));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("New Name");
    assertThat(result.orElseThrow().summary()).contains("New summary");
    verify(session).merge(entity);
    verify(session).merge(collection);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty if entity does not exist")
  void patch_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result = dao.patch(tenantId, collectionId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates only summary when name is empty")
  void patch_onlySummary() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, "OrderedCollection", "Old summary", now, now, null);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);
    mockEmptyItemQuery();

    var result = dao.patch(tenantId, collectionId, Optional.empty(), Optional.of("Updated"));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().summary()).contains("Updated");
    assertThat(result.orElseThrow().name()).isEqualTo("Old Name");
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates only name when summary is empty")
  void patch_onlyName() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, "OrderedCollection", "Old summary", now, now, null);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);
    mockEmptyItemQuery();

    var result = dao.patch(tenantId, collectionId, Optional.of("Only Name"), Optional.empty());

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("Only Name");
    assertThat(result.orElseThrow().summary()).contains("Old summary");
    verify(tx).commit();
  }

  @SuppressWarnings("unchecked")
  private void mockEmptyItemQuery() {
    Query<UUID> itemQuery = mock(Query.class);
    when(session.createQuery(contains("SELECT ci.refersToId"), eq(UUID.class)))
        .thenReturn(itemQuery);
    when(itemQuery.setParameter("tenantId", tenantId)).thenReturn(itemQuery);
    when(itemQuery.setParameter("collectionId", collectionId)).thenReturn(itemQuery);
    when(itemQuery.getResultList()).thenReturn(List.of());
  }

  @Test
  @DisplayName("softDelete marks deletedOn and returns true")
  void softDelete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, "OrderedCollection", "Summary", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var result = dao.softDelete(tenantId, collectionId);

    assertThat(result).isTrue();
    assertThat(entity.getDeletedOn()).isNotNull();
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false if collection already deleted")
  void softDelete_alreadyDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(tenantId, collectionId, "OrderedCollection", "Summary", now, now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var result = dao.softDelete(tenantId, collectionId);

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty if collection entity is missing")
  void patch_collectionNull() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, "OrderedCollection", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(null);

    var result = dao.patch(tenantId, collectionId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty if entity is soft deleted")
  void patch_deletedEntity() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(tenantId, collectionId, "OrderedCollection", "Summary", now, now, now);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Name");
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);

    var result = dao.patch(tenantId, collectionId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("create rolls back and rethrows on exception")
  void create_exceptionTriggersRollback() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.create(tenantId, ownerId, "Cast", Optional.empty()))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("patch rolls back and rethrows on exception")
  void patch_exceptionTriggersRollback() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(
            () -> dao.patch(tenantId, collectionId, Optional.of("Name"), Optional.empty()))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("softDelete rolls back and rethrows on exception")
  void softDelete_exceptionTriggersRollback() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.softDelete(tenantId, collectionId))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("Exception during transaction triggers rollback and rethrows")
  void exceptionTriggersRollback() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    @SuppressWarnings("unchecked")
    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(any(String.class), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.findById(tenantId, collectionId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("DB error");
    verify(tx).rollback();
  }

  @SuppressWarnings("unchecked")
  private void mockTenantConfigQuery() {
    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(any(String.class), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn("ok");
  }
}
