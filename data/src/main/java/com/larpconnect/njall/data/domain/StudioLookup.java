package com.larpconnect.njall.data.domain;

import java.time.Instant;
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
 * @param deletedAt Soft deletion timestamp, or null if active.
 */
public record StudioLookup(
    UUID tenantId,
    UUID studioId,
    String alias,
    Instant createdAt,
    Instant updatedAt,
    @Nullable Instant deletedAt)
    implements DatabaseObject {

  @Override
  public UUID id() {
    return studioId;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public static StudioLookup of(
      UUID tenantId,
      UUID studioId,
      String alias,
      Instant createdAt,
      Instant updatedAt,
      @Nullable Instant deletedAt) {
    return new StudioLookup(tenantId, studioId, alias, createdAt, updatedAt, deletedAt);
  }
}
