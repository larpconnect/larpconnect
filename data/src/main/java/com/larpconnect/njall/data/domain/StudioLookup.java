package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of a multi-tenant studio lookup entry.
 *
 * @param tenantId The internal database tenant UUID.
 * @param studioId The public studio UUID.
 * @param alias The unique studio alias.
 * @param createdAt Creation timestamp.
 * @param updatedAt Last update timestamp.
 * @param deletedAt Soft deletion timestamp, or empty if active.
 */
@Immutable
public record StudioLookup(
    UUID tenantId,
    UUID studioId,
    String alias,
    Instant createdAt,
    Instant updatedAt,
    Optional<Instant> deletedAt)
    implements DatabaseObject {

  public StudioLookup(
      UUID tenantId,
      UUID studioId,
      String alias,
      Instant createdAt,
      Instant updatedAt,
      @Nullable Instant deletedAt) {
    this(tenantId, studioId, alias, createdAt, updatedAt, Optional.ofNullable(deletedAt));
  }

  @Override
  public UUID id() {
    return studioId;
  }

  public boolean isDeleted() {
    return deletedAt.isPresent();
  }
}
