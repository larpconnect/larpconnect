package com.larpconnect.njall.data.domain.studios;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.common.DatabaseObject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of a tenanted game system catalog entity.
 *
 * @param id The unique system UUID identifier.
 * @param generator The generating actor entity UUID.
 * @param name The unique human-readable system name within the tenant.
 * @param summary Optional description summary from the base entity.
 * @param createdOn Creation timestamp.
 * @param updatedOn Last updated timestamp.
 * @param deletedOn Soft deletion timestamp, or empty if active.
 */
@Immutable
public record GameSystem(
    UUID id,
    UUID generator,
    String name,
    Optional<String> summary,
    Instant createdOn,
    Instant updatedOn,
    Optional<Instant> deletedOn)
    implements DatabaseObject {

  public GameSystem(
      UUID id,
      UUID generator,
      String name,
      @Nullable String summary,
      Instant createdOn,
      Instant updatedOn,
      @Nullable Instant deletedOn) {
    this(
        id,
        generator,
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
