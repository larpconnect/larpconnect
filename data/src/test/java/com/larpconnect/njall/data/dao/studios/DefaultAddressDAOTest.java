package com.larpconnect.njall.data.dao.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.AddressType;
import com.larpconnect.njall.data.domain.GeoJsonPoint;
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

final class DefaultAddressDAOTest {

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
  @DisplayName("findById retrieves address with GeoJSON Point coordinates")
  void findById_successWithPoint() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    Object[] row =
        new Object[] {
          addressId,
          locationId,
          "PHYSICAL",
          "123 Pine St",
          "Suite A",
          "",
          "Seattle",
          "WA",
          "98101",
          "US",
          "{\"type\":\"Point\",\"coordinates\":[-122.3321,47.6062]}"
        };

    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<Object[]> query = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SELECT a.id, a.location_id"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(Collections.singletonList(row));

    var result = dao.findById(tenantId, locationId, addressId);

    assertThat(result).isPresent();
    var address = result.orElseThrow();
    assertThat(address.id()).isEqualTo(addressId);
    assertThat(address.locationId()).isEqualTo(locationId);
    assertThat(address.addressType()).isEqualTo(AddressType.PHYSICAL);
    assertThat(address.addressLine1()).isEqualTo("123 Pine St");
    assertThat(address.addressLine2()).contains("Suite A");
    assertThat(address.addressLine3()).isEmpty();
    assertThat(address.locality()).isEqualTo("Seattle");
    assertThat(address.geom()).isPresent();
    assertThat(address.geom().orElseThrow().longitude()).isEqualTo(-122.3321);
    assertThat(address.geom().orElseThrow().latitude()).isEqualTo(47.6062);
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById with malformed or non-numeric GeoJSON coordinates returns empty geom")
  void findById_malformedCoordinatesReturnsEmptyGeom() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    Object[] row =
        new Object[] {
          addressId,
          locationId,
          "PHYSICAL",
          "123 Pine St",
          "Suite A",
          "",
          "Seattle",
          "WA",
          "98101",
          "US",
          "{\"type\":\"Point\",\"coordinates\":[\"invalid\",\"coords\"]}"
        };

    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<Object[]> query = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SELECT a.id, a.location_id"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(Collections.singletonList(row));

    var result = dao.findById(tenantId, locationId, addressId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().geom()).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when address row not found")
  void findById_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<Object[]> query = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SELECT a.id, a.location_id"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(Collections.emptyList());

    var result = dao.findById(tenantId, locationId, addressId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("listByLocation returns list of addresses for active location")
  void listByLocation_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);

    Object[] row1 =
        new Object[] {
          addressId,
          locationId,
          "PHYSICAL",
          "123 Pine St",
          "",
          "",
          "Seattle",
          "WA",
          "98101",
          "US",
          null
        };

    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<Object[]> query = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SELECT a.id, a.location_id"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(Collections.singletonList(row1));

    var list = dao.listByLocation(tenantId, locationId);

    assertThat(list).hasSize(1);
    assertThat(list.getFirst().id()).isEqualTo(addressId);
    assertThat(list.getFirst().geom()).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("listByLocation returns empty list when parent location is inactive")
  void listByLocation_inactiveLocation() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(false);

    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<Object[]> query = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SELECT a.id, a.location_id"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter(any(String.class), any())).thenReturn(query);
    when(query.getResultList()).thenReturn(Collections.emptyList());

    var list = dao.listByLocation(tenantId, locationId);

    assertThat(list).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("create fluid builder inserts row and returns persisted Address")
  void create_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);

    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<Void> geomQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(
            contains("UPDATE njall_users.addresses SET geom"), eq(Void.class)))
        .thenReturn(geomQuery);
    when(geomQuery.setParameter(any(String.class), any())).thenReturn(geomQuery);

    var created =
        dao.create(tenantId, locationId)
            .addressType(AddressType.PHYSICAL)
            .addressLine1("123 Pine St")
            .addressLine2(Optional.of("Suite A"))
            .addressLine3("Bldg 2")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .geom(new GeoJsonPoint(-122.33, 47.60))
            .execute();

    assertThat(created.addressLine1()).isEqualTo("123 Pine St");
    assertThat(created.addressLine2()).contains("Suite A");
    assertThat(created.addressLine3()).contains("Bldg 2");
    assertThat(created.geom()).contains(new GeoJsonPoint(-122.33, 47.60));
    verify(session).persist(any(AddressEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("create throws exception when parent location is inactive")
  void create_inactiveLocation_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(false);

    assertThatThrownBy(
            () ->
                dao.create(tenantId, locationId)
                    .addressType(AddressType.PHYSICAL)
                    .addressLine1("Line 1")
                    .locality("Seattle")
                    .administrativeArea("WA")
                    .postalCode("98101")
                    .countryCode("US")
                    .execute())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Parent location not found or inactive");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws NullPointerException when required fields are missing")
  void create_missingRequired_throws() {
    assertThatThrownBy(() -> dao.create(tenantId, locationId).execute())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("patch fluid builder updates mutable fields and returns updated Address")
  void patch_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);

    var entity =
        AddressEntity.builder(tenantId, addressId, locationId)
            .addressType("PHYSICAL")
            .addressLine1("123 Pine St")
            .addressLine2("Suite A")
            .addressLine3("Bldg 2")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .build();
    when(session.find(eq(AddressEntity.class), any(EntityId.class))).thenReturn(entity);

    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<Void> clearGeomQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(
            contains("UPDATE njall_users.addresses SET geom = NULL"), eq(Void.class)))
        .thenReturn(clearGeomQuery);
    when(clearGeomQuery.setParameter(any(String.class), any())).thenReturn(clearGeomQuery);

    var updated =
        dao.patch(tenantId, locationId, addressId)
            .addressType(Optional.of(AddressType.MAILING))
            .addressLine1(Optional.of("200 Pine St"))
            .addressLine2(Optional.empty())
            .addressLine3("Suite B")
            .locality(Optional.of("Seattle"))
            .administrativeArea("WA")
            .postalCode(Optional.of("98101"))
            .countryCode("US")
            .clearGeom()
            .execute();

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().addressType()).isEqualTo(AddressType.MAILING);
    assertThat(updated.orElseThrow().geom()).isEmpty();
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch fluid builder updates geom point when geom is provided")
  void patch_updateGeom_withPoint() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);

    var entity =
        AddressEntity.builder(tenantId, addressId, locationId)
            .addressType("PHYSICAL")
            .addressLine1("123 Pine St")
            .addressLine2("")
            .addressLine3("")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .build();
    when(session.find(eq(AddressEntity.class), any(EntityId.class))).thenReturn(entity);

    // Suppressed due to Hibernate generic query mocking type erasure.
    @SuppressWarnings("unchecked")
    NativeQuery<Void> geomQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(
            contains("UPDATE njall_users.addresses SET geom"), eq(Void.class)))
        .thenReturn(geomQuery);
    when(geomQuery.setParameter(any(String.class), any())).thenReturn(geomQuery);

    var updated =
        dao.patch(tenantId, locationId, addressId)
            .geom(Optional.of(new GeoJsonPoint(-122.33, 47.60)))
            .execute();

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().geom()).contains(new GeoJsonPoint(-122.33, 47.60));
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch retains existing geom when geom is not modified")
  void patch_retainsExistingGeom() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(true);

    var entity =
        AddressEntity.builder(tenantId, addressId, locationId)
            .addressType("PHYSICAL")
            .addressLine1("123 Pine St")
            .addressLine2("")
            .addressLine3("")
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
    when(loadGeomQuery.getResultList())
        .thenReturn(
            Collections.singletonList("{\"type\":\"Point\",\"coordinates\":[-122.33,47.60]}"));

    var updated = dao.patch(tenantId, locationId, addressId).addressLine1("Updated Line").execute();

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().geom()).contains(new GeoJsonPoint(-122.33, 47.60));
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when parent location is inactive")
  void patch_inactiveLocation_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    mockLocationActive(false);

    var result =
        dao.patch(tenantId, locationId, addressId)
            .addressLine1("New Line")
            .geom(Optional.of(new GeoJsonPoint(-122.33, 47.60)))
            .execute();

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("delete removes address and returns true when found")
  void delete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entity =
        AddressEntity.builder(tenantId, addressId, locationId)
            .addressType("PHYSICAL")
            .addressLine1("123 Pine St")
            .addressLine2("")
            .addressLine3("")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .build();
    when(session.find(eq(AddressEntity.class), any(EntityId.class))).thenReturn(entity);

    var deleted = dao.delete(tenantId, locationId, addressId);

    assertThat(deleted).isTrue();
    verify(session).remove(entity);
    verify(tx).commit();
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
