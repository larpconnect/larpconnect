package com.larpconnect.njall.api.studios.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.larpconnect.njall.data.dao.studios.inbox.InboxActivityDAO;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class InboxDeduplicationCacheTest {

  private static final String ACTIVITY_ID = "https://larpconnect.app/act/123";
  private static final String SECOND_ACTIVITY_ID = "https://larpconnect.app/act/456";

  private InboxActivityDAO dao;
  private FakeTicker ticker;
  private InboxDeduplicationCache cache;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID target1 = UUID.randomUUID();
  private final UUID target2 = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-14T12:00:00Z");

  private static final class FakeTicker implements Ticker {
    private final AtomicLong nanos = new AtomicLong(0L);

    @Override
    public long read() {
      return nanos.get();
    }

    void advance(Duration duration) {
      nanos.addAndGet(duration.toNanos());
    }
  }

  @BeforeEach
  void setUp() {
    dao = mock(InboxActivityDAO.class);
    ticker = new FakeTicker();
    cache = new DefaultInboxDeduplicationCache(dao, ticker, Duration.ofDays(8), 1000);
  }

  @Test
  @DisplayName("filterUnseen delegates to DAO on cache miss and caches DAO-seen targets")
  void filterUnseen_cacheMiss_delegatesToDaoAndCachesSeen() {
    Set<UUID> candidates = ImmutableSet.of(target1, target2);
    when(dao.filterUnseenTargets(eq(tenantId), eq(ACTIVITY_ID), eq(candidates), any()))
        .thenReturn(ImmutableSet.of(target1));

    var unseen = cache.filterUnseen(tenantId, ACTIVITY_ID, candidates, now);

    assertThat(unseen).containsExactly(target1);
    verify(dao, times(1)).filterUnseenTargets(eq(tenantId), eq(ACTIVITY_ID), eq(candidates), any());

    // target2 was already seen in DB and should now be cached in L1
    assertThat(cache.filterUnseen(tenantId, ACTIVITY_ID, ImmutableSet.of(target2), now)).isEmpty();
    verify(dao, times(1)).filterUnseenTargets(any(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "recordSeen marks targets as seen in L1 cache and invalidateAll clears cached entries")
  void recordSeen_andInvalidateAll() {
    cache.recordSeen(tenantId, ACTIVITY_ID, ImmutableSet.of(target1));

    assertThat(cache.filterUnseen(tenantId, ACTIVITY_ID, ImmutableSet.of(target1), now)).isEmpty();
    verify(dao, never()).filterUnseenTargets(any(), any(), any(), any());

    cache.invalidateAll();
    when(dao.filterUnseenTargets(
            eq(tenantId), eq(ACTIVITY_ID), eq(ImmutableSet.of(target1)), any()))
        .thenReturn(ImmutableSet.of(target1));

    assertThat(cache.filterUnseen(tenantId, ACTIVITY_ID, ImmutableSet.of(target1), now))
        .containsExactly(target1);
  }

  @Test
  @DisplayName("Cache entries expire after 8 days, causing subsequent calls to check DAO again")
  void cacheExpiration_after8Days() {
    cache.recordSeen(tenantId, ACTIVITY_ID, ImmutableSet.of(target1));
    ticker.advance(Duration.ofDays(8).plusSeconds(1));

    Set<UUID> candidates = ImmutableSet.of(target1);
    when(dao.filterUnseenTargets(eq(tenantId), eq(ACTIVITY_ID), eq(candidates), any()))
        .thenReturn(ImmutableSet.of(target1));

    var unseen = cache.filterUnseen(tenantId, ACTIVITY_ID, candidates, now);

    assertThat(unseen).containsExactly(target1);
    verify(dao, times(1)).filterUnseenTargets(eq(tenantId), eq(ACTIVITY_ID), eq(candidates), any());
  }

  @Test
  @DisplayName("filterUnseen with empty candidate targets returns immediately without DAO call")
  void filterUnseen_emptyCandidates_returnsEmpty() {
    var defaultCache = new DefaultInboxDeduplicationCache(dao, ticker);
    var unseen = defaultCache.filterUnseen(tenantId, ACTIVITY_ID, ImmutableSet.of(), now);
    assertThat(unseen).isEmpty();
    verify(dao, never()).filterUnseenTargets(any(), any(), any(), any());
  }

  @Test
  @DisplayName(
      "filterUnseenBatch filters L1 hits, batches L1 misses into one DAO call, and caches DB hits")
  void filterUnseenBatch_combinesL1AndSingleDaoCall() {
    assertThat(cache.filterUnseenBatch(tenantId, ImmutableSetMultimap.of(), now).entries())
        .isEmpty();

    cache.recordSeen(tenantId, ACTIVITY_ID, ImmutableSet.of(target1));
    assertThat(
            cache
                .filterUnseenBatch(tenantId, ImmutableSetMultimap.of(ACTIVITY_ID, target1), now)
                .entries())
        .isEmpty();
    verify(dao, never()).filterUnseenTargetsBatch(any(), any(), any());

    var batchCandidates =
        ImmutableSetMultimap.<String, UUID>builder()
            .put(ACTIVITY_ID, target1) // L1 hit -> skipped
            .put(ACTIVITY_ID, target2) // L1 miss -> unseen in DB
            .put(SECOND_ACTIVITY_ID, target1) // L1 miss -> already seen in DB
            .build();
    var expectedMisses = ImmutableSetMultimap.of(ACTIVITY_ID, target2, SECOND_ACTIVITY_ID, target1);
    when(dao.filterUnseenTargetsBatch(tenantId, expectedMisses, now))
        .thenReturn(ImmutableSetMultimap.of(ACTIVITY_ID, target2));

    var result = cache.filterUnseenBatch(tenantId, batchCandidates, now);
    assertThat(result.get(ACTIVITY_ID)).containsExactly(target2);
    assertThat(result.get(SECOND_ACTIVITY_ID)).isEmpty();

    // SECOND_ACTIVITY_ID + target1 was absent from daoUnseen, so it must now be cached in L1
    assertThat(
            cache
                .filterUnseenBatch(
                    tenantId, ImmutableSetMultimap.of(SECOND_ACTIVITY_ID, target1), now)
                .entries())
        .isEmpty();
    verify(dao, times(1)).filterUnseenTargetsBatch(any(), any(), any());
  }
}
