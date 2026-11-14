package com.larpconnect.njall.data.dao.studios.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.data.domain.studios.EventType;
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
    assertThat(defaultEvent.getEventType()).isEqualTo(EventType.EVENT_TYPE_UNSPECIFIED);

    var fullEvent =
        new EventEntity(tenantId, id, EventType.CONFERENCE, locationId, "Opening", now, later);
    fullEvent.setEventType(EventType.WORKSHOP);
    fullEvent.setTitle("Grand Opening");
    fullEvent.setLocationId(null);
    fullEvent.setStartTime(null);
    fullEvent.setEndTime(null);

    assertThat(fullEvent.getTenantId()).isEqualTo(tenantId);
    assertThat(fullEvent.getId()).isEqualTo(id);
    assertThat(fullEvent.getEventType()).isEqualTo(EventType.WORKSHOP);
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

    var event =
        new EventEntity(tenantId, id, EventType.MEETUP, locationId, "Solstice Camp", start, end);

    assertThat(event.getTenantId()).isEqualTo(tenantId);
    assertThat(event.getId()).isEqualTo(id);
    assertThat(event.getEventType()).isEqualTo(EventType.MEETUP);
    assertThat(event.getLocationId()).isEqualTo(locationId);
    assertThat(event.getTitle()).isEqualTo("Solstice Camp");
    assertThat(event.getStartTime()).isEqualTo(start);
    assertThat(event.getEndTime()).isEqualTo(end);
  }
}
