package com.larpconnect.njall.data.dao.studios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LocationEntityTest {

  @Test
  @DisplayName("LocationEntity getters and setters operate correctly")
  void locationEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();

    var location = new LocationEntity(tenantId, id, "Initial Name");
    location.setName("Main Campsite");

    assertThat(location.getTenantId()).isEqualTo(tenantId);
    assertThat(location.getId()).isEqualTo(id);
    assertThat(location.getName()).isEqualTo("Main Campsite");
  }

  @Test
  @DisplayName("LocationEntity constructor sets all fields correctly")
  void locationEntity_constructor() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();

    var location = new LocationEntity(tenantId, id, "Grand Hall");

    assertThat(location.getTenantId()).isEqualTo(tenantId);
    assertThat(location.getId()).isEqualTo(id);
    assertThat(location.getName()).isEqualTo("Grand Hall");
  }
}
