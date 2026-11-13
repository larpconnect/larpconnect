package com.larpconnect.njall.data.dao.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultCollectionDAORemoveItemsTest {

  private Session session;
  private DefaultCollectionDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID collectionId = UUID.randomUUID();
  private final UUID generator = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    var sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultCollectionDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("removeItem returns false if collection does not exist")
  void removeItem_collectionNotFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(null);

    var result = dao.removeItem(tenantId, generator, collectionId, UUID.randomUUID());

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("removeItem returns false if actorId does not match collection generator")
  void removeItem_mismatchedActor_returnsFalse() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    var otherActor = UUID.randomUUID();
    mockActiveActor(otherActor);

    var entityBase =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(entityBase);

    var result = dao.removeItem(tenantId, otherActor, collectionId, UUID.randomUUID());

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("removeItem returns false if collection is soft deleted")
  void removeItem_collectionDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var deletedBase =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, now);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(deletedBase);

    var result = dao.removeItem(tenantId, generator, collectionId, UUID.randomUUID());

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("removeItem removes item and compacts subsequent ordinals")
  void removeItem_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var refId = UUID.randomUUID();
    var entityBase =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(entityBase);

    var existingItem =
        new CollectionItemEntity(
            UUID.randomUUID(), tenantId, collectionId, 1, now, tenantId, refId);
    mockExistingItem(refId, existingItem);

    var mutationQuery = mock(MutationQuery.class);
    when(session.createMutationQuery(eq(CollectionItemHelper.SHIFT_ORDINALS_DOWN_HQL)))
        .thenReturn(mutationQuery);
    when(mutationQuery.setParameter("tenantId", tenantId)).thenReturn(mutationQuery);
    when(mutationQuery.setParameter("collectionId", collectionId)).thenReturn(mutationQuery);
    when(mutationQuery.setParameter("removedOrdinal", 1)).thenReturn(mutationQuery);

    var result = dao.removeItem(tenantId, generator, collectionId, refId);

    assertThat(result).isTrue();
    verify(session).remove(existingItem);
    verify(mutationQuery).executeUpdate();
    verify(tx).commit();
  }

  @Test
  @DisplayName("removeItem returns true if item not present (idempotent)")
  void removeItem_notPresent() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var refId = UUID.randomUUID();
    var entityBase =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(entityBase);

    mockItemQueryEmpty(refId);

    var result = dao.removeItem(tenantId, generator, collectionId, refId);

    assertThat(result).isTrue();
    verify(tx).commit();
  }

  @Test
  @DisplayName("removeItem rolls back and rethrows on exception")
  void removeItem_exceptionTriggersRollback() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(eq(DefaultCollectionDAO.SET_TENANT_CONFIG), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.removeItem(tenantId, generator, collectionId, UUID.randomUUID()))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  private void mockActiveActor(UUID actorId) {
    var actorEntity =
        new EntityBaseEntity(tenantId, actorId, actorId, "Actor", "Actor Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, actorId)))
        .thenReturn(actorEntity);
  }

  @SuppressWarnings("unchecked")
  private void mockExistingItem(UUID refId, CollectionItemEntity item) {
    Query<CollectionItemEntity> itemQuery = mock(Query.class);
    when(session.createQuery(
            eq(CollectionItemHelper.FIND_ITEM_BY_REFERS_TO_HQL), eq(CollectionItemEntity.class)))
        .thenReturn(itemQuery);
    when(itemQuery.setParameter("tenantId", tenantId)).thenReturn(itemQuery);
    when(itemQuery.setParameter("collectionId", collectionId)).thenReturn(itemQuery);
    when(itemQuery.setParameter("refersToId", refId)).thenReturn(itemQuery);
    when(itemQuery.uniqueResultOptional()).thenReturn(Optional.of(item));
  }

  @SuppressWarnings("unchecked")
  private void mockItemQueryEmpty(UUID refId) {
    Query<CollectionItemEntity> itemQuery = mock(Query.class);
    when(session.createQuery(
            eq(CollectionItemHelper.FIND_ITEM_BY_REFERS_TO_HQL), eq(CollectionItemEntity.class)))
        .thenReturn(itemQuery);
    when(itemQuery.setParameter("tenantId", tenantId)).thenReturn(itemQuery);
    when(itemQuery.setParameter("collectionId", collectionId)).thenReturn(itemQuery);
    when(itemQuery.setParameter("refersToId", refId)).thenReturn(itemQuery);
    when(itemQuery.uniqueResultOptional()).thenReturn(Optional.empty());
  }

  @SuppressWarnings("unchecked")
  private void mockTenantConfigQuery() {
    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(eq(DefaultCollectionDAO.SET_TENANT_CONFIG), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(any(String.class), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn("ok");
  }
}
