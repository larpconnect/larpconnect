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

final class DefaultCollectionDAOPatchTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultCollectionDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID collectionId = UUID.randomUUID();
  private final UUID owner = UUID.randomUUID();
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

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, owner, "OrderedCollection", "Old summary", now, now, null);
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
            tenantId, collectionId, owner, "OrderedCollection", "Old summary", now, now, null);
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
            tenantId, collectionId, owner, "OrderedCollection", "Old summary", now, now, null);
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

  @Test
  @DisplayName("patch returns empty if collection entity is missing")
  void patch_collectionNull() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, owner, "OrderedCollection", "Summary", now, now, null);
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
        new EntityBaseEntity(
            tenantId, collectionId, owner, "OrderedCollection", "Summary", now, now, now);
    var collection = new CollectionEntity(tenantId, collectionId, ownerId, "Name");
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(CollectionEntity.class, entityId)).thenReturn(collection);

    var result = dao.patch(tenantId, collectionId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
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
  @DisplayName("softDelete marks deletedOn and returns true")
  void softDelete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, collectionId);
    var entity =
        new EntityBaseEntity(
            tenantId, collectionId, owner, "OrderedCollection", "Summary", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var result = dao.softDelete(tenantId, collectionId);

    assertThat(result).isTrue();
    assertThat(entity.getDeletedOn()).isNotNull();
    assertThat(entity.getUpdatedOn()).isNotNull();
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
        new EntityBaseEntity(
            tenantId, collectionId, owner, "OrderedCollection", "Summary", now, now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var result = dao.softDelete(tenantId, collectionId);

    assertThat(result).isFalse();
    verify(tx).commit();
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

  @SuppressWarnings("unchecked")
  private void mockEmptyItemQuery() {
    Query<UUID> itemQuery = mock(Query.class);
    when(session.createQuery(contains("SELECT ci.refersToId"), eq(UUID.class)))
        .thenReturn(itemQuery);
    when(itemQuery.setParameter("tenantId", tenantId)).thenReturn(itemQuery);
    when(itemQuery.setParameter("collectionId", collectionId)).thenReturn(itemQuery);
    when(itemQuery.getResultList()).thenReturn(List.of());
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
