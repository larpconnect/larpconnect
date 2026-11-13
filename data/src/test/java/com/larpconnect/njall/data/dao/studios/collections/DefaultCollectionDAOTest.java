package com.larpconnect.njall.data.dao.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultCollectionDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultCollectionDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID collectionId = UUID.randomUUID();
  private final UUID generator = UUID.randomUUID();
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
  @DisplayName("isActiveActor returns true for active Actor and false otherwise")
  void isActiveActor_checksActorStatus() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    assertThat(dao.isActiveActor(tenantId, generator)).isTrue();
    verify(tx).commit();
  }

  @Test
  @DisplayName("isActiveActor returns false when actor is missing or soft deleted")
  void isActiveActor_missingOrDeleted_returnsFalse() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generator))).thenReturn(null);

    assertThat(dao.isActiveActor(tenantId, generator)).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns active Collection with item IDs for generating actor")
  void findById_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var entityId = new EntityId(tenantId, collectionId);
    mockEntity(collectionId, "OrderedCollection", null);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Cast");
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);

    var itemId = UUID.randomUUID();
    @SuppressWarnings("unchecked")
    Query<UUID> itemQuery = mock(Query.class);
    when(session.createQuery(eq(CollectionItemHelper.QUERY_ACTIVE_ITEM_IDS_HQL), eq(UUID.class)))
        .thenReturn(itemQuery);
    when(itemQuery.setParameter("tenantId", tenantId)).thenReturn(itemQuery);
    when(itemQuery.setParameter("collectionId", collectionId)).thenReturn(itemQuery);
    when(itemQuery.getResultList()).thenReturn(List.of(itemId));

    var result = dao.findById(tenantId, generator, collectionId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(collectionId);
    assertThat(result.orElseThrow().generator()).isEqualTo(generator);
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
    mockActiveActor(generator);
    var entityId = new EntityId(tenantId, collectionId);
    mockEntity(collectionId, "OrderedCollection", now);
    when(session.find(CollectionEntity.class, entityId))
        .thenReturn(new CollectionEntity(tenantId, collectionId, ownerId, "Cast"));

    assertThat(dao.findById(tenantId, generator, collectionId)).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when actorId does not match collection generator")
  void findById_mismatchedActor_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    var otherActor = UUID.randomUUID();
    mockActiveActor(otherActor);
    mockEntity(collectionId, "OrderedCollection", null);

    assertThat(dao.findById(tenantId, otherActor, collectionId)).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when generating actor is soft deleted")
  void findById_deletedActor_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockEntity(generator, "Actor", now);

    assertThat(dao.findById(tenantId, generator, collectionId)).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when target entity is not an OrderedCollection")
  void findById_wrongEntityType_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);
    mockEntity(collectionId, "Individual", null);

    assertThat(dao.findById(tenantId, generator, collectionId)).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when collection is not found")
  void findById_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(null);

    assertThat(dao.findById(tenantId, generator, collectionId)).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when collection entity is null")
  void findById_collectionEntityNull() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);
    mockEntity(collectionId, "OrderedCollection", null);
    when(session.find(CollectionEntity.class, new EntityId(tenantId, collectionId)))
        .thenReturn(null);

    assertThat(dao.findById(tenantId, generator, collectionId)).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findExpandedItems returns ordered list of ExpandedEntity")
  void findExpandedItems_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);
    mockEntity(collectionId, "OrderedCollection", null);

    @SuppressWarnings("unchecked")
    NativeQuery<Object[]> nativeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(
            eq(CollectionItemHelper.EXPANDED_ITEMS_QUERY), eq(Object[].class)))
        .thenReturn(nativeQuery);
    when(nativeQuery.setParameter("tenantId", tenantId)).thenReturn(nativeQuery);
    when(nativeQuery.setParameter("collectionId", collectionId)).thenReturn(nativeQuery);

    var refId = UUID.randomUUID();
    Object[] row = new Object[] {refId, generator, "Individual", "Lead bard", "Alice"};
    when(nativeQuery.getResultList()).thenReturn(List.<Object[]>of(row));

    var items = dao.findExpandedItems(tenantId, generator, collectionId);
    assertThat(items).hasSize(1);
    assertThat(items.get(0).id()).isEqualTo(refId);
    assertThat(items.get(0).generator()).isEqualTo(generator);
    assertThat(items.get(0).type()).isEqualTo("Individual");
    assertThat(items.get(0).name()).contains("Alice");
    assertThat(items.get(0).summary()).contains("Lead bard");
    verify(tx).commit();
  }

  @Test
  @DisplayName("findExpandedItems returns empty when actorId does not match collection generator")
  void findExpandedItems_mismatchedActor_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    var otherActor = UUID.randomUUID();
    mockActiveActor(otherActor);
    mockEntity(collectionId, "OrderedCollection", null);

    assertThat(dao.findExpandedItems(tenantId, otherActor, collectionId)).isEmpty();
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
    when(session.createQuery(eq(DefaultCollectionDAO.EXISTS_BY_OWNER_AND_NAME_HQL), eq(Long.class)))
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
    when(session.createQuery(
            eq(DefaultCollectionDAO.EXISTS_BY_OWNER_AND_NAME_EXCLUDING_ID_HQL), eq(Long.class)))
        .thenReturn(query);
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
  @DisplayName("create persists base and collection entities for active non-Actor owner entity")
  void create_persistsEntities() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);
    mockEntity(ownerId, "Individual", null);
    mockCreateUniquenessQuery(ownerId, "Cast", 0L);

    var collection = dao.create(tenantId, generator, ownerId, "Cast", Optional.of("Summary"));
    assertThat(collection.name()).isEqualTo("Cast");
    assertThat(collection.generator()).isEqualTo(generator);
    assertThat(collection.ownerId()).isEqualTo(ownerId);
    assertThat(collection.summary()).contains("Summary");
    assertThat(collection.totalItems()).isEqualTo(0);
    assertThat(collection.itemIds()).isEmpty();
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(CollectionEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("create with ownerId equal to generator looks up EntityBaseEntity once")
  void create_sameOwnerAsGenerator_looksUpEntityOnce() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);
    mockCreateUniquenessQuery(generator, "Cast", 0L);

    var collection = dao.create(tenantId, generator, generator, "Cast", Optional.empty());
    assertThat(collection.generator()).isEqualTo(generator);
    assertThat(collection.ownerId()).isEqualTo(generator);
    verify(session, times(1)).find(EntityBaseEntity.class, new EntityId(tenantId, generator));
    verify(tx).commit();
  }

  @Test
  @DisplayName("create throws IllegalStateException on duplicate collection name for owner")
  void create_duplicateName_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);
    mockEntity(ownerId, "Individual", null);
    mockCreateUniquenessQuery(ownerId, "Cast", 1L);

    assertThatThrownBy(
            () -> dao.create(tenantId, generator, ownerId, "Cast", Optional.of("Summary")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Collection with name 'Cast' already exists for owner");
    verify(session, never()).persist(any());
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when generator does not exist")
  void create_generatorNotFound_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generator))).thenReturn(null);
    assertThatThrownBy(
            () -> dao.create(tenantId, generator, ownerId, "Cast", Optional.of("Summary")))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when generator is soft deleted")
  void create_generatorDeleted_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockEntity(generator, "Actor", now);
    assertThatThrownBy(
            () -> dao.create(tenantId, generator, ownerId, "Cast", Optional.of("Summary")))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when generator is not an Actor")
  void create_generatorNotActor_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockEntity(generator, "Event", null);
    assertThatThrownBy(
            () -> dao.create(tenantId, generator, ownerId, "Cast", Optional.of("Summary")))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when ownerId entity does not exist")
  void create_ownerEntityNotFound_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, ownerId))).thenReturn(null);
    assertThatThrownBy(
            () -> dao.create(tenantId, generator, ownerId, "Cast", Optional.of("Summary")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Referenced entity does not exist or has been deleted");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when ownerId entity is soft deleted")
  void create_ownerEntityDeleted_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);
    mockEntity(ownerId, "Individual", now);
    assertThatThrownBy(
            () -> dao.create(tenantId, generator, ownerId, "Cast", Optional.of("Summary")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Referenced entity does not exist or has been deleted");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create rolls back and rethrows on exception")
  void create_exceptionTriggersRollback() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(eq(DefaultCollectionDAO.SET_TENANT_CONFIG), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));
    assertThatThrownBy(() -> dao.create(tenantId, generator, ownerId, "Cast", Optional.empty()))
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
    when(session.createNativeQuery(eq(DefaultCollectionDAO.SET_TENANT_CONFIG), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(any(String.class), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenThrow(new RuntimeException("DB error"));
    assertThatThrownBy(() -> dao.findById(tenantId, generator, collectionId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("DB error");
    verify(tx).rollback();
  }

  private void mockActiveActor(UUID actorId) {
    mockEntity(actorId, "Actor", null);
  }

  private void mockEntity(UUID entityId, String entityType, @Nullable Instant deletedOn) {
    var entity =
        new EntityBaseEntity(
            tenantId, entityId, generator, entityType, "Summary", now, now, deletedOn);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, entityId))).thenReturn(entity);
  }

  @SuppressWarnings("unchecked")
  private void mockCreateUniquenessQuery(UUID targetOwnerId, String name, long count) {
    Query<Long> uniquenessQuery = mock(Query.class);
    when(session.createQuery(eq(DefaultCollectionDAO.EXISTS_BY_OWNER_AND_NAME_HQL), eq(Long.class)))
        .thenReturn(uniquenessQuery);
    when(uniquenessQuery.setParameter("tenantId", tenantId)).thenReturn(uniquenessQuery);
    when(uniquenessQuery.setParameter("ownerId", targetOwnerId)).thenReturn(uniquenessQuery);
    when(uniquenessQuery.setParameter("name", name)).thenReturn(uniquenessQuery);
    when(uniquenessQuery.getSingleResult()).thenReturn(count);
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
