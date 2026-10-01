package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class DefaultLocationDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultLocationDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultLocationDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById sets app.tenant_id and returns active Location")
  void findById_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    var entity =
        new EntityBaseEntity(tenantId, locationId, "Location", "Main campsite", now, now, null);
    var location = new LocationEntity(tenantId, locationId, "Camp Whispering Pines");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LocationEntity.class, entityId)).thenReturn(location);

    var result = dao.findById(tenantId, locationId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(locationId);
    assertThat(result.orElseThrow().name()).isEqualTo("Camp Whispering Pines");
    assertThat(result.orElseThrow().summary()).contains("Main campsite");
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when location is soft-deleted")
  void findById_softDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    var entity =
        new EntityBaseEntity(tenantId, locationId, "Location", "Main campsite", now, now, now);
    var location = new LocationEntity(tenantId, locationId, "Camp Whispering Pines");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LocationEntity.class, entityId)).thenReturn(location);

    var result = dao.findById(tenantId, locationId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when entity does not exist")
  void findById_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    when(session.find(LocationEntity.class, entityId)).thenReturn(null);

    var result = dao.findById(tenantId, locationId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("create inserts entity and location rows within tenant context")
  void create_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var created = dao.create(tenantId, "Pine Camp", Optional.of("New camp"));

    assertThat(created.name()).isEqualTo("Pine Camp");
    assertThat(created.summary()).contains("New camp");
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(LocationEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates mutable fields and returns updated Location")
  void patch_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    var entity =
        new EntityBaseEntity(tenantId, locationId, "Location", "Old summary", now, now, null);
    var location = new LocationEntity(tenantId, locationId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LocationEntity.class, entityId)).thenReturn(location);

    var updated =
        dao.patch(tenantId, locationId, Optional.of("New Name"), Optional.of("New summary"));

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().name()).isEqualTo("New Name");
    assertThat(updated.orElseThrow().summary()).contains("New summary");
    verify(session).merge(entity);
    verify(session).merge(location);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when location is not found")
  void patch_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result = dao.patch(tenantId, locationId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete sets deleted_on on active location")
  void softDelete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    var entity = new EntityBaseEntity(tenantId, locationId, "Location", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var deleted = dao.softDelete(tenantId, locationId);

    assertThat(deleted).isTrue();
    assertThat(entity.getDeletedOn()).isNotNull();
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when location already deleted or missing")
  void softDelete_alreadyDeletedOrMissing() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    var entity = new EntityBaseEntity(tenantId, locationId, "Location", "Summary", now, now, now);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var deleted = dao.softDelete(tenantId, locationId);

    assertThat(deleted).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when location entity is missing")
  void findById_locationEntityMissing() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    var entity =
        new EntityBaseEntity(tenantId, locationId, "Location", "Main campsite", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LocationEntity.class, entityId)).thenReturn(null);

    var result = dao.findById(tenantId, locationId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when location entity is missing")
  void patch_locationEntityMissing() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    var entity =
        new EntityBaseEntity(tenantId, locationId, "Location", "Old summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LocationEntity.class, entityId)).thenReturn(null);

    var result = dao.patch(tenantId, locationId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when location is soft-deleted")
  void patch_softDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    var entity =
        new EntityBaseEntity(tenantId, locationId, "Location", "Old summary", now, now, now);
    var location = new LocationEntity(tenantId, locationId, "Old Name");
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LocationEntity.class, entityId)).thenReturn(location);

    var result = dao.patch(tenantId, locationId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity does not exist")
  void softDelete_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, locationId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var deleted = dao.softDelete(tenantId, locationId);

    assertThat(deleted).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById rollbacks on error and suppresses rollback exception")
  void findById_rollbackOnError_withSuppression() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("DB query failed"));
    Mockito.doThrow(new RuntimeException("Rollback failed")).when(tx).rollback();

    assertThatThrownBy(() -> dao.findById(tenantId, locationId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("DB query failed")
        .satisfies(
            e -> {
              assertThat(e.getSuppressed()).hasSize(1);
              assertThat(e.getSuppressed()[0]).hasMessage("Rollback failed");
            });
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create rollbacks on error")
  void create_rollbackOnError() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("Persist failed"));

    assertThatThrownBy(() -> dao.create(tenantId, "Camp", Optional.empty()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Persist failed");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("patch rollbacks on error")
  void patch_rollbackOnError() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("Patch failed"));

    assertThatThrownBy(() -> dao.patch(tenantId, locationId, Optional.empty(), Optional.empty()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Patch failed");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("softDelete rollbacks on error")
  void softDelete_rollbackOnError() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("Soft delete failed"));

    assertThatThrownBy(() -> dao.softDelete(tenantId, locationId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Soft delete failed");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("Unsupported methods throw UnsupportedOperationException")
  void unsupportedMethods_throwException() {
    assertThatThrownBy(() -> dao.list()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> dao.findById(UUID.randomUUID()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private void mockTenantConfigQuery() {
    @SuppressWarnings("unchecked")
    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());
  }
}
