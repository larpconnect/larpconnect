package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.domain.AddressType;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultAddressDAOBranchTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultAddressDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final UUID addressId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultAddressDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("create without geom succeeds and leaves geom empty")
  void create_withoutGeom() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);

    var created =
        dao.create(tenantId, locationId)
            .addressType(AddressType.PHYSICAL)
            .addressLine1("123 Main St")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .execute();

    assertThat(created.geom()).isEmpty();
    verify(session).persist(any(AddressEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch with addressLine2 updates the field")
  void patch_withAddressLine2() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);

    var entity =
        AddressEntity.builder(tenantId, addressId, locationId)
            .addressType("PHYSICAL")
            .addressLine1("123 Pine St")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .build();
    when(session.find(eq(AddressEntity.class), any(EntityId.class))).thenReturn(entity);

    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<String> loadGeomQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SELECT ST_AsGeoJSON(geom)"), eq(String.class)))
        .thenReturn(loadGeomQuery);
    when(loadGeomQuery.setParameter(any(String.class), any())).thenReturn(loadGeomQuery);
    when(loadGeomQuery.getResultList()).thenReturn(Collections.emptyList());

    var updated =
        dao.patch(tenantId, locationId, addressId).addressLine2(Optional.of("Suite 400")).execute();

    assertThat(updated).isPresent();
    assertThat(entity.getAddressLine2()).isEqualTo("Suite 400");
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when entity belongs to different location")
  void patch_wrongLocationId_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);

    var entity =
        AddressEntity.builder(tenantId, addressId, UUID.randomUUID())
            .addressType("PHYSICAL")
            .addressLine1("123 Pine St")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .build();
    when(session.find(eq(AddressEntity.class), any(EntityId.class))).thenReturn(entity);

    var result =
        dao.patch(tenantId, locationId, addressId).addressLine1(Optional.of("New St")).execute();

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("delete returns false when entity belongs to different location")
  void delete_wrongLocationId_returnsFalse() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entity =
        AddressEntity.builder(tenantId, addressId, UUID.randomUUID())
            .addressType("PHYSICAL")
            .addressLine1("123 Pine St")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .build();
    when(session.find(eq(AddressEntity.class), any(EntityId.class))).thenReturn(entity);

    var deleted = dao.delete(tenantId, locationId, addressId);

    assertThat(deleted).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById rollbacks on error and suppresses rollback exception")
  void findById_rollbackOnError_withSuppression() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("Query error"));
    doThrow(new RuntimeException("Rollback failure")).when(tx).rollback();

    assertThatThrownBy(() -> dao.findById(tenantId, locationId, addressId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Query error")
        .satisfies(
            e -> {
              assertThat(e.getSuppressed()).hasSize(1);
              assertThat(e.getSuppressed()[0]).hasMessage("Rollback failure");
            });
    verify(tx).rollback();
  }

  @Test
  @DisplayName("listByLocation rollbacks on error")
  void listByLocation_rollbackOnError() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("List error"));

    assertThatThrownBy(() -> dao.listByLocation(tenantId, locationId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("List error");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create rollbacks on error")
  void create_rollbackOnError() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);
    doThrow(new RuntimeException("Persist error")).when(session).persist(any());

    assertThatThrownBy(
            () ->
                dao.create(tenantId, locationId)
                    .addressType(AddressType.PHYSICAL)
                    .addressLine1("123 Main")
                    .locality("Seattle")
                    .administrativeArea("WA")
                    .postalCode("98101")
                    .countryCode("US")
                    .execute())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Persist error");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("patch rollbacks on error")
  void patch_rollbackOnError() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);

    var entity =
        AddressEntity.builder(tenantId, addressId, locationId)
            .addressType("PHYSICAL")
            .addressLine1("123 Pine St")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .build();
    when(session.find(eq(AddressEntity.class), any(EntityId.class))).thenReturn(entity);
    doThrow(new RuntimeException("Merge error")).when(session).merge(any());

    assertThatThrownBy(
            () ->
                dao.patch(tenantId, locationId, addressId)
                    .addressLine1(Optional.of("New Line"))
                    .execute())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Merge error");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("delete rollbacks on error")
  void delete_rollbackOnError() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entity =
        AddressEntity.builder(tenantId, addressId, locationId)
            .addressType("PHYSICAL")
            .addressLine1("123 Pine St")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .build();
    when(session.find(eq(AddressEntity.class), any(EntityId.class))).thenReturn(entity);
    doThrow(new RuntimeException("Remove error")).when(session).remove(any());

    assertThatThrownBy(() -> dao.delete(tenantId, locationId, addressId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Remove error");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("patch returns empty when entity not found or wrong location")
  void patch_notFound_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);

    when(session.find(eq(AddressEntity.class), any(EntityId.class))).thenReturn(null);

    var result = dao.patch(tenantId, locationId, addressId).addressLine1("New Line").execute();

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("delete returns false when entity not found")
  void delete_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    when(session.find(eq(AddressEntity.class), any(EntityId.class))).thenReturn(null);

    var deleted = dao.delete(tenantId, locationId, addressId);

    assertThat(deleted).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("Unsupported methods throw UnsupportedOperationException")
  void unsupportedMethods_throwException() {
    assertThatThrownBy(() -> dao.list()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> dao.findById(UUID.randomUUID()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private void mockTenantConfigQuery() {
    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());
  }

  private void mockLocationActive(boolean active) {
    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<Long> checkQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(
            contains("SELECT count(1) FROM njall_users.locations"), eq(Long.class)))
        .thenReturn(checkQuery);
    when(checkQuery.setParameter(any(String.class), any())).thenReturn(checkQuery);
    when(checkQuery.getSingleResult()).thenReturn(active ? 1L : 0L);
  }
}
