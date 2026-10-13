package com.larpconnect.njall.data.domain;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of a tenanted actor entity (Agent).
 *
 * @param id The unique actor UUID identifier.
 * @param ownerId The owner entity UUID.
 * @param preferredUsername The case-insensitive unique username within the tenant.
 * @param summary Optional description summary from the base entity.
 * @param inboxId The UUID of the paired inbox collection.
 * @param outboxId The UUID of the paired outbox collection.
 * @param endpoints Federated endpoints mapping (name -> URI).
 * @param createdOn Creation timestamp.
 * @param updatedOn Last updated timestamp.
 * @param deletedOn Soft deletion timestamp, or empty if active.
 */
@Immutable
public record Agent(
    UUID id,
    UUID ownerId,
    String preferredUsername,
    Optional<String> summary,
    UUID inboxId,
    UUID outboxId,
    ImmutableMap<String, String> endpoints,
    Instant createdOn,
    Instant updatedOn,
    Optional<Instant> deletedOn)
    implements DatabaseObject {

  public Agent {
    endpoints = ImmutableMap.copyOf(endpoints);
  }

  public Agent(
      UUID id,
      UUID ownerId,
      String preferredUsername,
      @Nullable String summary,
      UUID inboxId,
      UUID outboxId,
      Map<String, String> endpoints,
      Instant createdOn,
      Instant updatedOn,
      @Nullable Instant deletedOn) {
    this(
        id,
        ownerId,
        preferredUsername,
        Optional.ofNullable(summary),
        inboxId,
        outboxId,
        ImmutableMap.copyOf(endpoints),
        createdOn,
        updatedOn,
        Optional.ofNullable(deletedOn));
  }

  public boolean isDeleted() {
    return deletedOn.isPresent();
  }
}
