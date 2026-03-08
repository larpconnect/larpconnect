package com.larpconnect.njall.common.time;

import com.google.common.base.Stopwatch;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.larpconnect.njall.common.annotations.InstallInstead;
import jakarta.inject.Singleton;
import java.time.Clock;

@InstallInstead(TimeModule.class)
final class TimeBindingModule extends AbstractModule {
  TimeBindingModule() {}

  @Override
  protected void configure() {
    bind(Time.class).to(TimeService.class);
    bind(TimeService.class).to(MonotonicTimeService.class).in(Singleton.class);
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
