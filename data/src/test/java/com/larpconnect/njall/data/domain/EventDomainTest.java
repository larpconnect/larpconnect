package com.larpconnect.njall.data.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class EventDomainTest {

  @Test
  @DisplayName("Canonical constructor retains all fields and computes isDeleted")
  void canonicalConstructor_retainsFields() {
    var id = UUID.randomUUID();
    var locationId = UUID.randomUUID();
    var now = Instant.now();
    var start = now.plusSeconds(3600);
    var end = start.plusSeconds(7200);

    var event =
        new Event(
            id,
            Optional.of(locationId),
            "Autumn Fest",
            Optional.of("Festival summary"),
            Optional.of(start),
            Optional.of(end),
            now,
            now,
            Optional.empty());

    assertThat(event.id()).isEqualTo(id);
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
    var now = Instant.now();

    var event =
        new Event(
            id,
            (UUID) null,
            "Town Hall",
            (String) null,
            (Instant) null,
            (Instant) null,
            now,
            now,
            (Instant) null);

    assertThat(event.id()).isEqualTo(id);
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
    var now = Instant.now();

    var event = new Event(id, null, "Past Event", null, null, null, now, now, now);

    assertThat(event.isDeleted()).isTrue();
    assertThat(event.deletedOn()).contains(now);
  }
}
