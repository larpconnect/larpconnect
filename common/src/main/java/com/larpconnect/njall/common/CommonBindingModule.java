package com.larpconnect.njall.common;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.larpconnect.njall.common.annotation.Blocking;
import java.time.Clock;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Package-private binding module providing common utility instances. */
final class CommonBindingModule extends AbstractModule {

  @Provides
  Clock provideClock() {
    return Clock.systemUTC();
  }

  @Provides
  Ticker provideTicker() {
    return Ticker.systemTicker();
  }

  @Provides
  @Singleton
  @Blocking
  Executor provideBlockingExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
