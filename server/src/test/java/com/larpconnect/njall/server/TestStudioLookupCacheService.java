package com.larpconnect.njall.server;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractIdleService;
import com.larpconnect.njall.data.cache.StudioLookupCacheService;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

final class TestStudioLookupCacheService extends AbstractIdleService
    implements StudioLookupCacheService {

  private final boolean failStart;
  private final boolean failStop;
  private final List<String> eventLog;

  TestStudioLookupCacheService() {
    this(false, false, new CopyOnWriteArrayList<>());
  }

  TestStudioLookupCacheService(List<String> eventLog) {
    this(false, false, eventLog);
  }

  TestStudioLookupCacheService(boolean failStart, boolean failStop) {
    this(failStart, failStop, new CopyOnWriteArrayList<>());
  }

  TestStudioLookupCacheService(boolean failStart, boolean failStop, List<String> eventLog) {
    this.failStart = failStart;
    this.failStop = failStop;
    this.eventLog = eventLog;
  }

  static TestStudioLookupCacheService failingOnStart() {
    return new TestStudioLookupCacheService(true, false);
  }

  static TestStudioLookupCacheService failingOnStop() {
    return new TestStudioLookupCacheService(false, true);
  }

  @Override
  protected void startUp() throws Exception {
    eventLog.add("cache.startUp");
    if (failStart) {
      throw new RuntimeException("Forced cache start failure");
    }
  }

  @Override
  protected void shutDown() throws Exception {
    eventLog.add("cache.shutDown");
    if (failStop) {
      throw new RuntimeException("Forced cache stop failure");
    }
  }

  @Override
  public Optional<StudioLookup> findById(UUID studioId) {
    return Optional.empty();
  }

  @Override
  public Optional<StudioLookup> findByAlias(String alias) {
    return Optional.empty();
  }

  @Override
  public Optional<StudioLookup> findByIdOrAlias(String idOrAlias) {
    return Optional.empty();
  }

  @Override
  public ImmutableList<StudioLookup> listActive() {
    return ImmutableList.of();
  }

  @Override
  public void refresh() {
    eventLog.add("cache.refresh");
  }

  List<String> eventLog() {
    return eventLog;
  }
}
