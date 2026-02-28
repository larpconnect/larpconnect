package com.larpconnect.njall.common.time;

import com.google.common.base.Stopwatch;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jakarta.inject.Singleton;
import java.time.Clock;

final class TimeBindingModule extends AbstractModule {
  TimeBindingModule() {}

  @Override
  protected void configure() {
    bind(TimeService.class).to(MonotonicTimeService.class);
  }

  @Provides
  @Singleton
  Clock provideClock() {
    return Clock.systemUTC();
  }

  @Provides
  Stopwatch provideStopwatch() {
    return Stopwatch.createUnstarted();
  }
}
