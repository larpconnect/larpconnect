package com.larpconnect.njall.data.dao.studios.individuals;

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
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultIndividualDAOPatchTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultIndividualDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID individualId = UUID.randomUUID();
  private final UUID owner = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultIndividualDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("patch updates name and summary on active individual")
  void patch_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    var entity =
        new EntityBaseEntity(
            tenantId, individualId, owner, "Individual", "Old Summary", now, now, null);
    var individual = new IndividualEntity(tenantId, individualId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(IndividualEntity.class, entityId)).thenReturn(individual);

    var result =
        dao.patch(tenantId, individualId, Optional.of("New Name"), Optional.of("New Summary"));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("New Name");
    assertThat(result.orElseThrow().summary()).contains("New Summary");
    verify(session).merge(entity);
    verify(session).merge(individual);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates only name when summary is empty")
  void patch_updatesOnlyName() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    var entity =
        new EntityBaseEntity(
            tenantId, individualId, owner, "Individual", "Old Summary", now, now, null);
    var individual = new IndividualEntity(tenantId, individualId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(IndividualEntity.class, entityId)).thenReturn(individual);

    var result = dao.patch(tenantId, individualId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("New Name");
    assertThat(result.orElseThrow().summary()).contains("Old Summary");
    verify(session).merge(entity);
    verify(session).merge(individual);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates only summary when name is empty")
  void patch_updatesOnlySummary() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    var entity =
        new EntityBaseEntity(
            tenantId, individualId, owner, "Individual", "Old Summary", now, now, null);
    var individual = new IndividualEntity(tenantId, individualId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(IndividualEntity.class, entityId)).thenReturn(individual);

    var result = dao.patch(tenantId, individualId, Optional.empty(), Optional.of("New Summary"));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("Old Name");
    assertThat(result.orElseThrow().summary()).contains("New Summary");
    verify(session).merge(entity);
    verify(session).merge(individual);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch leaves fields unchanged when both optionals are empty")
  void patch_noUpdates() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    var entity =
        new EntityBaseEntity(
            tenantId, individualId, owner, "Individual", "Old Summary", now, now, null);
    var individual = new IndividualEntity(tenantId, individualId, "Old Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(IndividualEntity.class, entityId)).thenReturn(individual);

    var result = dao.patch(tenantId, individualId, Optional.empty(), Optional.empty());

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().name()).isEqualTo("Old Name");
    assertThat(result.orElseThrow().summary()).contains("Old Summary");
    verify(session).merge(entity);
    verify(session).merge(individual);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when individual row is missing")
  void patch_missingIndividualSubtype() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    var entity =
        new EntityBaseEntity(
            tenantId, individualId, owner, "Individual", "Old Summary", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(IndividualEntity.class, entityId)).thenReturn(null);

    var result = dao.patch(tenantId, individualId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when individual is not found")
  void patch_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result = dao.patch(tenantId, individualId, Optional.of("Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when individual is soft-deleted")
  void patch_softDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    var entity =
        new EntityBaseEntity(tenantId, individualId, owner, "Individual", "Summary", now, now, now);
    var individual = new IndividualEntity(tenantId, individualId, "Name");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(IndividualEntity.class, entityId)).thenReturn(individual);

    var result = dao.patch(tenantId, individualId, Optional.of("New Name"), Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch rolls back and throws on merge failure")
  void patch_rollsBackOnException() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    when(session.find(EntityBaseEntity.class, entityId))
        .thenThrow(new RuntimeException("Find error"));

    assertThatThrownBy(
            () -> dao.patch(tenantId, individualId, Optional.of("Name"), Optional.empty()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Find error");

    verify(tx).rollback();
  }

  @Test
  @DisplayName("softDelete sets deletedOn and updatedOn and returns true")
  void softDelete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    var entity =
        new EntityBaseEntity(
            tenantId, individualId, owner, "Individual", "Summary", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var result = dao.softDelete(tenantId, individualId);

    assertThat(result).isTrue();
    assertThat(entity.getDeletedOn()).isNotNull();
    assertThat(entity.getUpdatedOn()).isNotNull();
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity is missing or already deleted")
  void softDelete_notFoundOrAlreadyDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result = dao.softDelete(tenantId, individualId);

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity has non-null deletedOn")
  void softDelete_alreadyDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    var entity =
        new EntityBaseEntity(tenantId, individualId, owner, "Individual", "Summary", now, now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var result = dao.softDelete(tenantId, individualId);

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete rolls back and throws on merge failure")
  void softDelete_rollsBackOnException() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, individualId);
    when(session.find(EntityBaseEntity.class, entityId))
        .thenThrow(new RuntimeException("Delete error"));

    assertThatThrownBy(() -> dao.softDelete(tenantId, individualId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Delete error");

    verify(tx).rollback();
  }

  @SuppressWarnings("unchecked")
  private void mockTenantConfigQuery() {
    var query = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class))).thenReturn(query);
    when(query.setParameter(eq("tenantId"), any())).thenReturn(query);
    when(query.getSingleResult()).thenReturn("ok");
  }
}
