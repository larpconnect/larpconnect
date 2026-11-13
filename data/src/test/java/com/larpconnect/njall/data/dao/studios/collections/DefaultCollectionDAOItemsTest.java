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

final class DefaultCollectionDAOItemsTest {

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
  @DisplayName("addItem returns false if collection or entity does not exist")
  void addItem_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(null);

    var result =
        dao.addItem(
            tenantId,
            generator,
            collectionId,
            UUID.randomUUID(),
            Optional.empty(),
            Optional.empty());

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("addItem returns false if actorId does not match collection generator")
  void addItem_mismatchedActor_returnsFalse() {
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

    var result =
        dao.addItem(
            tenantId,
            otherActor,
            collectionId,
            UUID.randomUUID(),
            Optional.empty(),
            Optional.empty());

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("addItem returns true if item already present without shifting")
  void addItem_alreadyPresent() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var refId = UUID.randomUUID();
    mockEntityBases(refId);
    mockCountQuery(refId, 1L);

    var result =
        dao.addItem(tenantId, generator, collectionId, refId, Optional.empty(), Optional.empty());

    assertThat(result).isTrue();
    verify(tx).commit();
  }

  @Test
  @DisplayName("addItem prepends to head and shifts existing ordinals by default")
  void addItem_defaultPrepend() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var refId = UUID.randomUUID();
    mockEntityBases(refId);
    mockCountQuery(refId, 0L);

    var mutationQuery = mockShiftQuery(0);

    var result =
        dao.addItem(tenantId, generator, collectionId, refId, Optional.empty(), Optional.empty());

    assertThat(result).isTrue();
    verify(mutationQuery).executeUpdate();
    verify(session).persist(any(CollectionItemEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("addItem with before resolves target ordinal from referenced item")
  void addItem_withBefore() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var refId = UUID.randomUUID();
    var beforeId = UUID.randomUUID();
    mockEntityBases(refId);
    mockCountQuery(refId, 0L);

    var existingItem =
        new CollectionItemEntity(
            UUID.randomUUID(), tenantId, collectionId, 2, now, tenantId, beforeId);
    mockBeforeQuery(beforeId, existingItem);
    var mutationQuery = mockShiftQuery(2);

    var result =
        dao.addItem(
            tenantId, generator, collectionId, refId, Optional.of(beforeId), Optional.empty());

    assertThat(result).isTrue();
    verify(mutationQuery).executeUpdate();
    verify(session).persist(any(CollectionItemEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("addItem with before not found falls back to index 0")
  void addItem_beforeNotFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var refId = UUID.randomUUID();
    var beforeId = UUID.randomUUID();
    mockEntityBases(refId);
    mockCountQuery(refId, 0L);

    mockItemQueryEmpty(beforeId);
    var mutationQuery = mockShiftQuery(0);

    var result =
        dao.addItem(
            tenantId, generator, collectionId, refId, Optional.of(beforeId), Optional.empty());

    assertThat(result).isTrue();
    verify(mutationQuery).executeUpdate();
    verify(tx).commit();
  }

  @Test
  @DisplayName("addItem with index uses provided integer index")
  void addItem_withIndex() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var refId = UUID.randomUUID();
    mockEntityBases(refId);
    mockCountQuery(refId, 0L);

    var mutationQuery = mockShiftQuery(3);

    var result =
        dao.addItem(tenantId, generator, collectionId, refId, Optional.empty(), Optional.of(3));

    assertThat(result).isTrue();
    verify(mutationQuery).executeUpdate();
    verify(tx).commit();
  }

  @Test
  @DisplayName("addItem returns false if collection is soft deleted")
  void addItem_collectionDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var deletedBase =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, now);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(deletedBase);

    var result =
        dao.addItem(
            tenantId,
            generator,
            collectionId,
            UUID.randomUUID(),
            Optional.empty(),
            Optional.empty());

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("addItem returns false if target entity does not exist or is soft deleted")
  void addItem_targetDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var refId = UUID.randomUUID();
    var entityBase =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, null);
    var deletedRef =
        new EntityBaseEntity(tenantId, refId, generator, "Individual", "Scholar", now, now, now);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(entityBase);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, refId)))
        .thenReturn(deletedRef);

    var result =
        dao.addItem(tenantId, generator, collectionId, refId, Optional.empty(), Optional.empty());

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("addItem returns false if target entity is null")
  void addItem_targetNull() {
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
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, refId))).thenReturn(null);

    var result =
        dao.addItem(tenantId, generator, collectionId, refId, Optional.empty(), Optional.empty());

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("addItem rolls back and rethrows on exception")
  void addItem_exceptionTriggersRollback() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(eq(DefaultCollectionDAO.SET_TENANT_CONFIG), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(
            () ->
                dao.addItem(
                    tenantId,
                    generator,
                    collectionId,
                    UUID.randomUUID(),
                    Optional.empty(),
                    Optional.empty()))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  private void mockActiveActor(UUID actorId) {
    var actorEntity =
        new EntityBaseEntity(tenantId, actorId, actorId, "Actor", "Actor Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, actorId)))
        .thenReturn(actorEntity);
  }

  private void mockEntityBases(UUID refId) {
    var entityBase =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, null);
    var refBase =
        new EntityBaseEntity(tenantId, refId, generator, "Individual", "Scholar", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(entityBase);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, refId))).thenReturn(refBase);
  }

  @SuppressWarnings("unchecked")
  private void mockCountQuery(UUID refId, long count) {
    Query<Long> countQuery = mock(Query.class);
    when(session.createQuery(eq(CollectionItemHelper.COUNT_ITEM_BY_REFERS_TO_HQL), eq(Long.class)))
        .thenReturn(countQuery);
    when(countQuery.setParameter("tenantId", tenantId)).thenReturn(countQuery);
    when(countQuery.setParameter("collectionId", collectionId)).thenReturn(countQuery);
    when(countQuery.setParameter("refersToId", refId)).thenReturn(countQuery);
    when(countQuery.getSingleResult()).thenReturn(count);
  }

  private MutationQuery mockShiftQuery(int targetOrdinal) {
    var mutationQuery = mock(MutationQuery.class);
    when(session.createMutationQuery(eq(CollectionItemHelper.SHIFT_ORDINALS_UP_HQL)))
        .thenReturn(mutationQuery);
    when(mutationQuery.setParameter("tenantId", tenantId)).thenReturn(mutationQuery);
    when(mutationQuery.setParameter("collectionId", collectionId)).thenReturn(mutationQuery);
    when(mutationQuery.setParameter("targetOrdinal", targetOrdinal)).thenReturn(mutationQuery);
    return mutationQuery;
  }

  @SuppressWarnings("unchecked")
  private void mockBeforeQuery(UUID beforeId, CollectionItemEntity item) {
    Query<CollectionItemEntity> beforeQuery = mock(Query.class);
    when(session.createQuery(
            eq(CollectionItemHelper.FIND_ITEM_BY_REFERS_TO_HQL), eq(CollectionItemEntity.class)))
        .thenReturn(beforeQuery);
    when(beforeQuery.setParameter("tenantId", tenantId)).thenReturn(beforeQuery);
    when(beforeQuery.setParameter("collectionId", collectionId)).thenReturn(beforeQuery);
    when(beforeQuery.setParameter("refersToId", beforeId)).thenReturn(beforeQuery);
    when(beforeQuery.uniqueResultOptional()).thenReturn(Optional.of(item));
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
