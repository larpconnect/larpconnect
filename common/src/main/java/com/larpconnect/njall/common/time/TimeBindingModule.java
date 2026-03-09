package com.larpconnect.njall.common.time;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.common.annotations.InstallInstead;
import jakarta.inject.Singleton;

@InstallInstead(TimeModule.class)
final class TimeBindingModule extends AbstractModule {
  TimeBindingModule() {}

  @Override
  protected void configure() {
    bind(MonotonicClock.class).to(TimeService.class);
    bind(TimeService.class).to(MonotonicTimeService.class).in(Singleton.class);
  }
}
