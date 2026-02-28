package com.larpconnect.njall.common.time;

import com.google.common.base.Ticker;
import com.google.inject.AbstractModule;
import java.time.Clock;

/** Guice module for time-related utilities. */
public final class TimeModule extends AbstractModule {

  /** Constructs a new TimeModule. */
  public TimeModule() {}

  @Override
  protected void configure() {
    bind(TimeService.class).to(MonotonicTimeService.class);
    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(Ticker.class).toInstance(Ticker.systemTicker());
  }
}
