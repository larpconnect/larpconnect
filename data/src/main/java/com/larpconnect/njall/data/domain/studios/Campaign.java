package com.larpconnect.njall.data.domain.studios;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.common.DatabaseObject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of a tenanted campaign entity.
 *
 * @param id The unique campaign UUID identifier.
 * @param generator The generating actor entity UUID.
 * @param systemId Optional UUID of the associated game system.
 * @param title The required human-readable campaign title.
 * @param summary Optional description summary from the base entity.
 * @param createdOn Creation timestamp.
 * @param updatedOn Last updated timestamp.
 * @param deletedOn Soft deletion timestamp, or empty if active.
 */
@Immutable
public record Campaign(
    UUID id,
    UUID generator,
    Optional<UUID> systemId,
    String title,
    Optional<String> summary,
    Instant createdOn,
    Instant updatedOn,
    Optional<Instant> deletedOn)
    implements DatabaseObject {

  public Campaign(
      UUID id,
      UUID generator,
      @Nullable UUID systemId,
      String title,
      @Nullable String summary,
      Instant createdOn,
      Instant updatedOn,
      @Nullable Instant deletedOn) {
    this(
        id,
        generator,
        Optional.ofNullable(systemId),
        title,
        Optional.ofNullable(summary),
        createdOn,
        updatedOn,
        Optional.ofNullable(deletedOn));
  }

  public boolean isDeleted() {
    return deletedOn.isPresent();
  }
}
