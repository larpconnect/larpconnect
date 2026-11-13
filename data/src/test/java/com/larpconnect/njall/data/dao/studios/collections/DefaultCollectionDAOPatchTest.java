package com.larpconnect.njall.data.dao.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

final class DefaultCollectionDAOPatchTest {

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
  @DisplayName("patch updates name and summary and returns updated collection")
  void patch_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Old summary", now, now, null);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);
    mockUniquePatchNameQuery("New Name", 0L);
    mockEmptyItemQuery();

    var result =
        dao.patch(
            tenantId, generator, collectionId, Optional.of("New Name"), Optional.of("New summary"));

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
    mockActiveActor(generator);

    var entityId = new EntityId(tenantId, collectionId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result =
        dao.patch(tenantId, generator, collectionId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when actorId does not match collection generator")
  void patch_mismatchedActor_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    var otherActor = UUID.randomUUID();
    mockActiveActor(otherActor);

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Old summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var result =
        dao.patch(tenantId, otherActor, collectionId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates only summary when name is empty")
  void patch_onlySummary() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Old summary", now, now, null);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);
    mockEmptyItemQuery();

    var result =
        dao.patch(tenantId, generator, collectionId, Optional.empty(), Optional.of("Updated"));

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
    mockActiveActor(generator);

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Old summary", now, now, null);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);
    mockUniquePatchNameQuery("Only Name", 0L);
    mockEmptyItemQuery();

    var result =
        dao.patch(tenantId, generator, collectionId, Optional.of("Only Name"), Optional.empty());

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("Only Name");
    assertThat(result.orElseThrow().summary()).contains("Old summary");
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch throws IllegalStateException when renamed collection conflicts for owner")
  void patch_duplicateName_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Old summary", now, now, null);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);
    mockUniquePatchNameQuery("Duplicate Name", 1L);

    assertThatThrownBy(
            () ->
                dao.patch(
                    tenantId,
                    generator,
                    collectionId,
                    Optional.of("Duplicate Name"),
                    Optional.empty()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Collection with name 'Duplicate Name' already exists for owner");
    verify(session, never()).merge(any());
    verify(tx).rollback();
  }

  @Test
  @DisplayName("patch returns empty if collection entity is missing")
  void patch_collectionNull() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(null);

    var result =
        dao.patch(tenantId, generator, collectionId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty if entity is soft deleted")
  void patch_deletedEntity() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, now);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Name");
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);

    var result =
        dao.patch(tenantId, generator, collectionId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch rolls back and rethrows on exception")
  void patch_exceptionTriggersRollback() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(eq(DefaultCollectionDAO.SET_TENANT_CONFIG), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(
            () ->
                dao.patch(tenantId, generator, collectionId, Optional.of("Name"), Optional.empty()))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("softDelete marks deletedOn and returns true")
  void softDelete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var result = dao.softDelete(tenantId, generator, collectionId);

    assertThat(result).isTrue();
    assertThat(entity.getDeletedOn()).isNotNull();
    assertThat(entity.getUpdatedOn()).isNotNull();
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when actorId does not match collection generator")
  void softDelete_mismatchedActor_returnsFalse() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    var otherActor = UUID.randomUUID();
    mockActiveActor(otherActor);

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    assertThat(dao.softDelete(tenantId, otherActor, collectionId)).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false if collection already deleted")
  void softDelete_alreadyDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockActiveActor(generator);

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, generator, "OrderedCollection", "Summary", now, now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var result = dao.softDelete(tenantId, generator, collectionId);

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete rolls back and rethrows on exception")
  void softDelete_exceptionTriggersRollback() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(eq(DefaultCollectionDAO.SET_TENANT_CONFIG), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.softDelete(tenantId, generator, collectionId))
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
  private void mockUniquePatchNameQuery(String name, long count) {
    Query<Long> uniquenessQuery = mock(Query.class);
    when(session.createQuery(
            eq(DefaultCollectionDAO.EXISTS_BY_OWNER_AND_NAME_EXCLUDING_ID_HQL), eq(Long.class)))
        .thenReturn(uniquenessQuery);
    when(uniquenessQuery.setParameter("tenantId", tenantId)).thenReturn(uniquenessQuery);
    when(uniquenessQuery.setParameter("ownerId", ownerId)).thenReturn(uniquenessQuery);
    when(uniquenessQuery.setParameter("name", name)).thenReturn(uniquenessQuery);
    when(uniquenessQuery.setParameter("excludeId", collectionId)).thenReturn(uniquenessQuery);
    when(uniquenessQuery.getSingleResult()).thenReturn(count);
  }

  @SuppressWarnings("unchecked")
  private void mockEmptyItemQuery() {
    Query<UUID> itemQuery = mock(Query.class);
    when(session.createQuery(eq(CollectionItemHelper.QUERY_ACTIVE_ITEM_IDS_HQL), eq(UUID.class)))
        .thenReturn(itemQuery);
    when(itemQuery.setParameter("tenantId", tenantId)).thenReturn(itemQuery);
    when(itemQuery.setParameter("collectionId", collectionId)).thenReturn(itemQuery);
    when(itemQuery.getResultList()).thenReturn(List.of());
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
