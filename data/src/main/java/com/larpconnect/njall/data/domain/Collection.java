package com.larpconnect.njall.data.domain;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of a tenanted collection entity.
 *
 * @param id The unique collection UUID identifier.
 * @param ownerId The owner entity UUID.
 * @param name The collection name, unique per owner.
 * @param summary Optional description summary from the base entity.
 * @param totalItems Count of active items in the collection.
 * @param itemIds Ordered list of referenced entity UUIDs.
 * @param createdOn Creation timestamp.
 * @param updatedOn Last updated timestamp.
 * @param deletedOn Soft deletion timestamp, or empty if active.
 */
@Immutable
public record Collection(
    UUID id,
    UUID ownerId,
    String name,
    Optional<String> summary,
    int totalItems,
    ImmutableList<UUID> itemIds,
    Instant createdOn,
    Instant updatedOn,
    Optional<Instant> deletedOn)
    implements DatabaseObject {

  public Collection {
    itemIds = ImmutableList.copyOf(itemIds);
  }

  public Collection(
      UUID id,
      UUID ownerId,
      String name,
      @Nullable String summary,
      int totalItems,
      List<UUID> itemIds,
      Instant createdOn,
      Instant updatedOn,
      @Nullable Instant deletedOn) {
    this(
        id,
        ownerId,
        name,
        Optional.ofNullable(summary),
        totalItems,
        ImmutableList.copyOf(itemIds),
        createdOn,
        updatedOn,
        Optional.ofNullable(deletedOn));
  }

  public boolean isDeleted() {
    return deletedOn.isPresent();
  }
}
