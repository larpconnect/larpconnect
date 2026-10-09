package com.larpconnect.njall.data.dao.studios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AddressEntityTest {

  @Test
  @DisplayName("AddressEntity getters and setters operate correctly")
  void addressEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var locationId = UUID.randomUUID();

    var entity = new AddressEntity(tenantId, id, locationId);
    entity.setAddressType("PHYSICAL");
    entity.setAddressLine1("123 Main St");
    entity.setAddressLine2("Suite 100");
    entity.setAddressLine3("Bldg B");
    entity.setLocality("Seattle");
    entity.setAdministrativeArea("WA");
    entity.setPostalCode("98101");
    entity.setCountryCode("US");

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getLocationId()).isEqualTo(locationId);
    assertThat(entity.getAddressType()).isEqualTo("PHYSICAL");
    assertThat(entity.getAddressLine1()).isEqualTo("123 Main St");
    assertThat(entity.getAddressLine2()).isEqualTo("Suite 100");
    assertThat(entity.getAddressLine3()).isEqualTo("Bldg B");
    assertThat(entity.getLocality()).isEqualTo("Seattle");
    assertThat(entity.getAdministrativeArea()).isEqualTo("WA");
    assertThat(entity.getPostalCode()).isEqualTo("98101");
    assertThat(entity.getCountryCode()).isEqualTo("US");
  }

  @Test
  @DisplayName("AddressEntity builder sets all fields correctly")
  void addressEntity_builder() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var locationId = UUID.randomUUID();

    var entity =
        AddressEntity.builder(tenantId, id, locationId)
            .addressType("PHYSICAL")
            .addressLine1("123 Main St")
            .addressLine2("Suite 100")
            .addressLine3("Bldg B")
            .locality("Seattle")
            .administrativeArea("WA")
            .postalCode("98101")
            .countryCode("US")
            .build();

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getLocationId()).isEqualTo(locationId);
    assertThat(entity.getAddressType()).isEqualTo("PHYSICAL");
    assertThat(entity.getAddressLine1()).isEqualTo("123 Main St");
    assertThat(entity.getAddressLine2()).isEqualTo("Suite 100");
    assertThat(entity.getAddressLine3()).isEqualTo("Bldg B");
    assertThat(entity.getLocality()).isEqualTo("Seattle");
    assertThat(entity.getAdministrativeArea()).isEqualTo("WA");
    assertThat(entity.getPostalCode()).isEqualTo("98101");
    assertThat(entity.getCountryCode()).isEqualTo("US");
  }
}
