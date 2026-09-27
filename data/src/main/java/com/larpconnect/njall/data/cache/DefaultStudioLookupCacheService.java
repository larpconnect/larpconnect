package com.larpconnect.njall.data.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.errorprone.annotations.Immutable;
import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.StudioLookupDAO;
import com.larpconnect.njall.data.domain.DeletionFilter;
import com.larpconnect.njall.data.domain.StudioLookup;
import com.typesafe.config.Config;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-performance Caffeine-backed implementation of {@link StudioLookupCacheService} managed as a
 * Guava {@link AbstractScheduledService}.
 */
public final class DefaultStudioLookupCacheService extends AbstractScheduledService
    implements StudioLookupCacheService {

  private static final String CONFIG_KEY = "larpconnect.data.cache.studio-lookup.refresh-interval";

  private final Logger logger = LoggerFactory.getLogger(DefaultStudioLookupCacheService.class);
  private final StudioLookupDAO studioLookupDao;
  private final Duration refreshInterval;
  private final Ticker ticker;
  private final AtomicReference<CacheSnapshot> snapshot;

  @Inject
  DefaultStudioLookupCacheService(StudioLookupDAO studioLookupDao, Config config, Ticker ticker) {
    this(studioLookupDao, config.getDuration(CONFIG_KEY), ticker);
  }

  DefaultStudioLookupCacheService(
      StudioLookupDAO studioLookupDao, Duration refreshInterval, Ticker ticker) {
    this.studioLookupDao = studioLookupDao;
    this.refreshInterval = refreshInterval;
    this.ticker = ticker;
    this.snapshot = new AtomicReference<>(createInitialSnapshot(ticker));
  }

  @Override
  protected void startUp() throws Exception {
    reloadAll();
  }

  @Override
  protected void runOneIteration() {
    executeSafeReload();
  }

  private void executeSafeReload() {
    try {
      reloadAll();
    } catch (Exception e) {
      logger.error("Failed to refresh studio lookup cache; retaining existing snapshot", e);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(
        refreshInterval.toMillis(), refreshInterval.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void refresh() {
    reloadAll();
  }

  private void reloadAll() {
    var allStudios = queryAllStudios();
    var newSnapshot = buildSnapshot(allStudios);
    this.snapshot.set(newSnapshot);
  }

  private ImmutableList<StudioLookup> queryAllStudios() {
    return studioLookupDao.list(DeletionFilter.INCLUDE_DELETED);
  }

  private CacheSnapshot buildSnapshot(ImmutableList<StudioLookup> studios) {
    var entries = new HashMap<String, StudioLookup>();
    var activeBuilder = ImmutableList.<StudioLookup>builder();
    for (var studio : studios) {
      entries.put(idKey(studio.studioId()), studio);
      entries.put(aliasKey(studio.alias()), studio);
      if (studio.deletedAt().isEmpty()) {
        activeBuilder.add(studio);
      }
    }
    var cache = createCache(entries);
    return createSnapshot(cache, activeBuilder.build());
  }

  @Override
  public Optional<StudioLookup> findById(UUID studioId) {
    var entry = snapshot.get().cache().getIfPresent(idKey(studioId));
    return filterActive(entry);
  }

  @Override
  public Optional<StudioLookup> findByAlias(String alias) {
    var entry = snapshot.get().cache().getIfPresent(aliasKey(alias));
    return filterActive(entry);
  }

  @Override
  public Optional<StudioLookup> findByIdOrAlias(String idOrAlias) {
    return tryParseUuid(idOrAlias).flatMap(this::findById).or(() -> findByAlias(idOrAlias));
  }

  @Override
  public ImmutableList<StudioLookup> listActive() {
    return snapshot.get().activeStudios();
  }

  private static Optional<StudioLookup> filterActive(@Nullable StudioLookup entry) {
    if (entry != null && entry.deletedAt().isEmpty()) {
      return Optional.of(entry);
    }
    return Optional.empty();
  }

  private static Optional<UUID> tryParseUuid(String value) {
    if (!looksLikeUuid(value)) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(value));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static boolean looksLikeUuid(String value) {
    return value.length() == 36
        && value.charAt(8) == '-'
        && value.charAt(13) == '-'
        && value.charAt(18) == '-'
        && value.charAt(23) == '-';
  }

  private static String idKey(UUID id) {
    return "id:" + id;
  }

  private static String aliasKey(String alias) {
    return "alias:" + alias.toLowerCase(Locale.ROOT);
  }

  private Cache<String, StudioLookup> createCache(Map<String, StudioLookup> entries) {
    var cache = newEmptyCache();
    fillCache(cache, entries);
    return cache;
  }

  private Cache<String, StudioLookup> newEmptyCache() {
    return Caffeine.newBuilder().ticker(ticker).build();
  }

  private void fillCache(Cache<String, StudioLookup> cache, Map<String, StudioLookup> entries) {
    cache.putAll(entries);
  }

  private static CacheSnapshot createInitialSnapshot(Ticker ticker) {
    Cache<String, StudioLookup> cache = Caffeine.newBuilder().ticker(ticker).build();
    return new CacheSnapshot(cache, ImmutableList.of());
  }

  private static CacheSnapshot createSnapshot(
      Cache<String, StudioLookup> cache, ImmutableList<StudioLookup> activeStudios) {
    return new CacheSnapshot(cache, activeStudios);
  }

  /**
   * Internal immutable snapshot holding a populated Caffeine cache and active studio list.
   * Suppressed because Caffeine Cache does not declare @Immutable, but the instance is encapsulated
   * within this private record, populated at creation, never modified thereafter, and swapped
   * atomically via snapshot reference.
   */
  @Immutable
  @SuppressWarnings("Immutable")
  private record CacheSnapshot(
      Cache<String, StudioLookup> cache, ImmutableList<StudioLookup> activeStudios) {}
}
