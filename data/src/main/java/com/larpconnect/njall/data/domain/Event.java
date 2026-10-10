package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of a tenanted event entity.
 *
 * @param id The unique event UUID identifier.
 * @param locationId Optional UUID of the associated physical location.
 * @param title The required human-readable title of the event.
 * @param summary Optional description summary from the base entity.
 * @param startTime Optional starting timestamp of the event.
 * @param endTime Optional ending timestamp of the event.
 * @param createdOn Creation timestamp.
 * @param updatedOn Last updated timestamp.
 * @param deletedOn Soft deletion timestamp, or empty if active.
 */
@Immutable
public record Event(
    UUID id,
    Optional<UUID> locationId,
    String title,
    Optional<String> summary,
    Optional<Instant> startTime,
    Optional<Instant> endTime,
    Instant createdOn,
    Instant updatedOn,
    Optional<Instant> deletedOn)
    implements DatabaseObject {

  public Event(
      UUID id,
      @Nullable UUID locationId,
      String title,
      @Nullable String summary,
      @Nullable Instant startTime,
      @Nullable Instant endTime,
      Instant createdOn,
      Instant updatedOn,
      @Nullable Instant deletedOn) {
    this(
        id,
        Optional.ofNullable(locationId),
        title,
        Optional.ofNullable(summary),
        Optional.ofNullable(startTime),
        Optional.ofNullable(endTime),
        createdOn,
        updatedOn,
        Optional.ofNullable(deletedOn));
  }

  public boolean isDeleted() {
    return deletedOn.isPresent();
  }
}
