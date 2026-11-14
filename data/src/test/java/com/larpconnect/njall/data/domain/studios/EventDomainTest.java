package com.larpconnect.njall.data.domain.studios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class EventDomainTest {

  @Test
  @DisplayName("EventType contains all expected enum values")
  void eventType_values() {
    assertThat(EventType.values())
        .containsExactlyInAnyOrder(
            EventType.EVENT_TYPE_UNSPECIFIED,
            EventType.GAME,
            EventType.WORKSHOP,
            EventType.CONFERENCE,
            EventType.MEETUP);
  }

  @Test
  @DisplayName("Canonical constructor retains all fields and computes isDeleted")
  void canonicalConstructor_retainsFields() {
    var id = UUID.randomUUID();
    var generator = UUID.randomUUID();
    var locationId = UUID.randomUUID();
    var now = Instant.now();
    var start = now.plusSeconds(3600);
    var end = start.plusSeconds(7200);

    var event =
        new Event(
            id,
            generator,
            EventType.WORKSHOP,
            Optional.of(locationId),
            "Autumn Fest",
            Optional.of("Festival summary"),
            Optional.of(start),
            Optional.of(end),
            now,
            now,
            Optional.empty());

    assertThat(event.id()).isEqualTo(id);
    assertThat(event.generator()).isEqualTo(generator);
    assertThat(event.eventType()).isEqualTo(EventType.WORKSHOP);
    assertThat(event.locationId()).contains(locationId);
    assertThat(event.title()).isEqualTo("Autumn Fest");
    assertThat(event.summary()).contains("Festival summary");
    assertThat(event.startTime()).contains(start);
    assertThat(event.endTime()).contains(end);
    assertThat(event.createdOn()).isEqualTo(now);
    assertThat(event.updatedOn()).isEqualTo(now);
    assertThat(event.deletedOn()).isEmpty();
    assertThat(event.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("Nullable constructor wraps nulls into empty Optionals")
  void nullableConstructor_wrapsNulls() {
    var id = UUID.randomUUID();
    var generator = UUID.randomUUID();
    var now = Instant.now();

    var event =
        new Event(
            id,
            generator,
            EventType.EVENT_TYPE_UNSPECIFIED,
            (UUID) null,
            "Town Hall",
            (String) null,
            (Instant) null,
            (Instant) null,
            now,
            now,
            (Instant) null);

    assertThat(event.id()).isEqualTo(id);
    assertThat(event.generator()).isEqualTo(generator);
    assertThat(event.eventType()).isEqualTo(EventType.EVENT_TYPE_UNSPECIFIED);
    assertThat(event.locationId()).isEmpty();
    assertThat(event.title()).isEqualTo("Town Hall");
    assertThat(event.summary()).isEmpty();
    assertThat(event.startTime()).isEmpty();
    assertThat(event.endTime()).isEmpty();
    assertThat(event.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("isDeleted returns true when deletedOn is present")
  void isDeleted_returnsTrueWhenDeletedOnPresent() {
    var id = UUID.randomUUID();
    var generator = UUID.randomUUID();
    var now = Instant.now();

    var event =
        new Event(
            id,
            generator,
            EventType.CONFERENCE,
            null,
            "Past Event",
            null,
            null,
            null,
            now,
            now,
            now);

    assertThat(event.isDeleted()).isTrue();
    assertThat(event.eventType()).isEqualTo(EventType.CONFERENCE);
    assertThat(event.generator()).isEqualTo(generator);
    assertThat(event.deletedOn()).contains(now);
  }
}
