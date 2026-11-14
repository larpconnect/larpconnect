package com.larpconnect.njall.data.domain.studios;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.data.domain.common.GeoJsonPoint;
import java.time.Instant;
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
  @DisplayName("Location domain record constructor and accessors operate correctly")
  void location_accessors() {
    var id = UUID.randomUUID();
    var generator = UUID.randomUUID();
    var now = Instant.now();
    var location = new Location(id, generator, "Pineville Camp", "Outdoor venue", now, now, null);

    assertThat(location.id()).isEqualTo(id);
    assertThat(location.generator()).isEqualTo(generator);
    assertThat(location.name()).isEqualTo("Pineville Camp");
    assertThat(location.summary()).contains("Outdoor venue");
    assertThat(location.createdOn()).isEqualTo(now);
    assertThat(location.updatedOn()).isEqualTo(now);
    assertThat(location.deletedOn()).isEmpty();
    assertThat(location.isDeleted()).isFalse();

    var deletedLocation =
        new Location(id, generator, "Pineville Camp", Optional.empty(), now, now, Optional.of(now));
    assertThat(deletedLocation.generator()).isEqualTo(generator);
    assertThat(deletedLocation.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("Address domain record constructor filters blank optional lines and retains geom")
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
            "   ",
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

    var minimalAddress =
        new Address(
            id,
            locationId,
            AddressType.MAILING,
            "PO Box 1",
            " ",
            "Building C",
            "Pineville",
            "WA",
            "98101",
            "US",
            null);
    assertThat(minimalAddress.addressLine2()).isEmpty();
    assertThat(minimalAddress.addressLine3()).contains("Building C");
    assertThat(minimalAddress.geom()).isEmpty();
  }
}
