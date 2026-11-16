package com.larpconnect.njall.api.studios.inbox;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.errorprone.annotations.Immutable;
import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.inbox.InboxActivityDAO;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Default Caffeine-backed implementation of {@link InboxDeduplicationCache}. */
final class DefaultInboxDeduplicationCache implements InboxDeduplicationCache {

  private static final Duration DEFAULT_TTL = Duration.ofDays(8);
  private static final long DEFAULT_MAX_SIZE = 50_000L;

  private final InboxActivityDAO inboxActivityDao;
  private final Cache<DeduplicationKey, Boolean> cache;

  @Inject
  DefaultInboxDeduplicationCache(InboxActivityDAO inboxActivityDao, Ticker ticker) {
    this(inboxActivityDao, ticker, DEFAULT_TTL, DEFAULT_MAX_SIZE);
  }

  DefaultInboxDeduplicationCache(
      InboxActivityDAO inboxActivityDao, Ticker ticker, Duration ttl, long maximumSize) {
    this.inboxActivityDao = inboxActivityDao;
    this.cache =
        Caffeine.newBuilder().ticker(ticker).expireAfterWrite(ttl).maximumSize(maximumSize).build();
  }

  @Override
  public ImmutableSet<UUID> filterUnseen(
      UUID tenantId, String activityId, Set<UUID> candidateTargets, Instant now) {
    if (candidateTargets.isEmpty()) {
      return ImmutableSet.of();
    }

    var missesBuilder = ImmutableSet.<UUID>builder();
    for (UUID target : candidateTargets) {
      if (cache.getIfPresent(createKey(tenantId, activityId, target)) == null) {
        missesBuilder.add(target);
      }
    }
    var unseenCandidates = missesBuilder.build();
    if (unseenCandidates.isEmpty()) {
      return ImmutableSet.of();
    }

    ImmutableSet<UUID> daoUnseen =
        inboxActivityDao.filterUnseenTargets(tenantId, activityId, unseenCandidates, now);
    for (UUID candidate : unseenCandidates) {
      if (!daoUnseen.contains(candidate)) {
        cache.put(createKey(tenantId, activityId, candidate), Boolean.TRUE);
      }
    }
    return daoUnseen;
  }

  @Override
  public ImmutableSetMultimap<String, UUID> filterUnseenBatch(
      UUID tenantId, ImmutableSetMultimap<String, UUID> candidateTargets, Instant now) {
    if (candidateTargets.isEmpty()) {
      return ImmutableSetMultimap.of();
    }

    var missesBuilder = ImmutableSetMultimap.<String, UUID>builder();
    for (var entry : candidateTargets.entries()) {
      String activityId = entry.getKey();
      UUID target = entry.getValue();
      if (cache.getIfPresent(createKey(tenantId, activityId, target)) == null) {
        missesBuilder.put(activityId, target);
      }
    }
    var misses = missesBuilder.build();
    if (misses.isEmpty()) {
      return ImmutableSetMultimap.of();
    }

    var daoUnseen = inboxActivityDao.filterUnseenTargetsBatch(tenantId, misses, now);
    for (var entry : misses.entries()) {
      String activityId = entry.getKey();
      UUID target = entry.getValue();
      if (!daoUnseen.containsEntry(activityId, target)) {
        cache.put(createKey(tenantId, activityId, target), Boolean.TRUE);
      }
    }
    return daoUnseen;
  }

  @Override
  public void recordSeen(UUID tenantId, String activityId, Set<UUID> seenTargets) {
    for (UUID target : seenTargets) {
      cache.put(createKey(tenantId, activityId, target), Boolean.TRUE);
    }
  }

  @Override
  public void invalidateAll() {
    cache.invalidateAll();
  }

  private static DeduplicationKey createKey(UUID tenantId, String activityId, UUID targetActorId) {
    return new DeduplicationKey(tenantId, activityId, targetActorId);
  }

  @Immutable
  private record DeduplicationKey(UUID tenantId, String activityId, UUID targetActorId) {}
}
