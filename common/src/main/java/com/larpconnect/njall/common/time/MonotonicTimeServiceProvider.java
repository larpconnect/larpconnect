package com.larpconnect.njall.common.time;

import com.google.common.base.Ticker;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.time.Clock;

/** Provider for MonotonicTimeService. */
@Singleton
final class MonotonicTimeServiceProvider implements Provider<TimeService> {

  private final Clock clock;
  private final Ticker ticker;

  /**
   * Constructs the provider.
   *
   * @param clock the clock to inject
   * @param ticker the ticker to inject
   */
  @Inject
  public MonotonicTimeServiceProvider(Clock clock, Ticker ticker) {
    this.clock = clock;
    this.ticker = ticker;
  }

  @Override
  public TimeService get() {
    return new MonotonicTimeService(clock, ticker);
  }
}
