package com.larpconnect.njall.data.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LocationAndAddressDomainTest {

  @Test
  @DisplayName("AddressType contains all expected enum values")
  void addressType_values() {
    assertThat(AddressType.values())
        .containsExactlyInAnyOrder(
            AddressType.PHYSICAL,
            AddressType.MAILING,
            AddressType.PO_BOX,
            AddressType.BILLING,
            AddressType.OTHER);
  }

  @Test
  @DisplayName("GeoJsonPoint validates valid coordinates and provides accessors")
  void geoJsonPoint_validCoordinates() {
    var point = new GeoJsonPoint(-122.3321, 47.6062);
    assertThat(point.type()).isEqualTo("Point");
    assertThat(point.longitude()).isEqualTo(-122.3321);
    assertThat(point.latitude()).isEqualTo(47.6062);
    assertThat(point.coordinates()).containsExactly(-122.3321, 47.6062);

    var pointFromList = new GeoJsonPoint(List.of(10.0, 20.0));
    assertThat(pointFromList.longitude()).isEqualTo(10.0);
    assertThat(pointFromList.latitude()).isEqualTo(20.0);
  }

  @Test
  @DisplayName("GeoJsonPoint rejects invalid types, sizes, and boundary violations")
  void geoJsonPoint_invalidInputs() {
    assertThatThrownBy(() -> new GeoJsonPoint("Polygon", ImmutableList.of(-122.0, 47.0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("type must be 'Point'");

    assertThatThrownBy(() -> new GeoJsonPoint("Point", ImmutableList.of(-122.0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("contain exactly 2 numbers");

    assertThatThrownBy(() -> new GeoJsonPoint(-181.0, 45.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Longitude must be between -180.0 and 180.0");

    assertThatThrownBy(() -> new GeoJsonPoint(181.0, 45.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Longitude must be between -180.0 and 180.0");

    assertThatThrownBy(() -> new GeoJsonPoint(0.0, -91.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Latitude must be between -90.0 and 90.0");

    assertThatThrownBy(() -> new GeoJsonPoint(0.0, 91.0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Latitude must be between -90.0 and 90.0");
  }

  @Test
  @DisplayName("Location domain record constructor and accessors operate correctly")
  void location_accessors() {
    var id = UUID.randomUUID();
    var now = Instant.now();
    var location = new Location(id, "Pineville Camp", "Outdoor venue", now, now, null);

    assertThat(location.id()).isEqualTo(id);
    assertThat(location.name()).isEqualTo("Pineville Camp");
    assertThat(location.summary()).contains("Outdoor venue");
    assertThat(location.createdOn()).isEqualTo(now);
    assertThat(location.updatedOn()).isEqualTo(now);
    assertThat(location.deletedOn()).isEmpty();
    assertThat(location.isDeleted()).isFalse();

    var deletedLocation =
        new Location(id, "Pineville Camp", Optional.empty(), now, now, Optional.of(now));
    assertThat(deletedLocation.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("Address domain record constructor and accessors operate correctly")
  void address_accessors() {
    var id = UUID.randomUUID();
    var locationId = UUID.randomUUID();
    var point = new GeoJsonPoint(-122.33, 47.60);

    var address =
        new Address(
            id,
            locationId,
            AddressType.PHYSICAL,
            "123 Forest Road",
            "Suite 100",
            null,
            "Pineville",
            "WA",
            "98101",
            "US",
            point);

    assertThat(address.id()).isEqualTo(id);
    assertThat(address.locationId()).isEqualTo(locationId);
    assertThat(address.addressType()).isEqualTo(AddressType.PHYSICAL);
    assertThat(address.addressLine1()).isEqualTo("123 Forest Road");
    assertThat(address.addressLine2()).contains("Suite 100");
    assertThat(address.addressLine3()).isEmpty();
    assertThat(address.locality()).isEqualTo("Pineville");
    assertThat(address.administrativeArea()).isEqualTo("WA");
    assertThat(address.postalCode()).isEqualTo("98101");
    assertThat(address.countryCode()).isEqualTo("US");
    assertThat(address.geom()).contains(point);
  }
}
