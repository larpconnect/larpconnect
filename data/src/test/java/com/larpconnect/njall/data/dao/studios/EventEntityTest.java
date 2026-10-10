package com.larpconnect.njall.data.dao.studios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class EventEntityTest {

  @Test
  @DisplayName("EventEntity getters and setters operate correctly")
  void eventEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var locationId = UUID.randomUUID();
    var now = Instant.now();
    var later = now.plusSeconds(3600);

    var defaultEvent = new EventEntity();
    assertThat(defaultEvent.getId()).isNull();
    var fullEvent = new EventEntity(tenantId, id, locationId, "Opening", now, later);
    fullEvent.setTitle("Grand Opening");
    fullEvent.setLocationId(null);
    fullEvent.setStartTime(null);
    fullEvent.setEndTime(null);

    assertThat(fullEvent.getTenantId()).isEqualTo(tenantId);
    assertThat(fullEvent.getId()).isEqualTo(id);
    assertThat(fullEvent.getTitle()).isEqualTo("Grand Opening");
    assertThat(fullEvent.getLocationId()).isNull();
    assertThat(fullEvent.getStartTime()).isNull();
    assertThat(fullEvent.getEndTime()).isNull();
  }

  @Test
  @DisplayName("EventEntity constructor initializes all fields")
  void eventEntity_constructor() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var locationId = UUID.randomUUID();
    var start = Instant.now();
    var end = start.plusSeconds(7200);

    var event = new EventEntity(tenantId, id, locationId, "Solstice Camp", start, end);

    assertThat(event.getTenantId()).isEqualTo(tenantId);
    assertThat(event.getId()).isEqualTo(id);
    assertThat(event.getLocationId()).isEqualTo(locationId);
    assertThat(event.getTitle()).isEqualTo("Solstice Camp");
    assertThat(event.getStartTime()).isEqualTo(start);
    assertThat(event.getEndTime()).isEqualTo(end);
  }
}
