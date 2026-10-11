package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of a tenanted individual entity.
 *
 * @param id The unique individual UUID identifier.
 * @param name The required human-readable name of the individual.
 * @param summary Optional description summary from the base entity.
 * @param createdOn Creation timestamp.
 * @param updatedOn Last updated timestamp.
 * @param deletedOn Soft deletion timestamp, or empty if active.
 */
@Immutable
public record Individual(
    UUID id,
    String name,
    Optional<String> summary,
    Instant createdOn,
    Instant updatedOn,
    Optional<Instant> deletedOn)
    implements DatabaseObject {

  public Individual(
      UUID id,
      String name,
      @Nullable String summary,
      Instant createdOn,
      Instant updatedOn,
      @Nullable Instant deletedOn) {
    this(
        id,
        name,
        Optional.ofNullable(summary),
        createdOn,
        updatedOn,
        Optional.ofNullable(deletedOn));
  }

  public boolean isDeleted() {
    return deletedOn.isPresent();
  }
}
