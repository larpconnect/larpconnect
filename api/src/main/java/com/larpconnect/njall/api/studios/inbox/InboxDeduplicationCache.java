package com.larpconnect.njall.api.studios.inbox;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** In-memory Caffeine L1 cache contract fronting persistent 8-day activity deduplication. */
public interface InboxDeduplicationCache {

  /**
   * Filters the given candidate target actor UUIDs, returning only those not yet recorded for this
   * activity ID within the 8-day deduplication window.
   *
   * @param tenantId Studio tenant UUID.
   * @param activityId Unique FeatherPub activity ID.
   * @param candidateTargets Set of candidate target actor UUIDs.
   * @param now Current timestamp.
   * @return Immutable subset of target actor UUIDs that have not yet processed this activity.
   */
  ImmutableSet<UUID> filterUnseen(
      UUID tenantId, String activityId, Set<UUID> candidateTargets, Instant now);

  /**
   * Filters candidate {@code (activityId -> targetActorId)} pairs across a batch, checking L1 cache
   * first and delegating all L1 misses to a single batched PostgreSQL lookup.
   *
   * @param tenantId Studio tenant UUID.
   * @param candidateTargets Multimap of activity IDs to candidate target actor UUIDs.
   * @param now Current timestamp.
   * @return Multimap of activity IDs to unseen target actor UUIDs.
   */
  ImmutableSetMultimap<String, UUID> filterUnseenBatch(
      UUID tenantId, ImmutableSetMultimap<String, UUID> candidateTargets, Instant now);

  /**
   * Records target actor UUIDs as having successfully accepted and processed this activity.
   *
   * @param tenantId Studio tenant UUID.
   * @param activityId Unique FeatherPub activity ID.
   * @param seenTargets Set of target actor UUIDs.
   */
  void recordSeen(UUID tenantId, String activityId, Set<UUID> seenTargets);

  /** Invalidates all L1 deduplication cache entries. */
  void invalidateAll();
}
