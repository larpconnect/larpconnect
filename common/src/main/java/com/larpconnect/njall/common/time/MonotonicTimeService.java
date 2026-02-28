package com.larpconnect.njall.common.time;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Duration;

/**
 * A TimeService implementation that relies on a Guava Stopwatch to guarantee monotonic progression
 * relative to its startup epoch time.
 */
@Singleton
final class MonotonicTimeService extends AbstractIdleService implements TimeService {

  private final Clock clock;
  private final Stopwatch stopwatch;
  private long baseTimeMillis;

  /**
   * Constructs a new MonotonicTimeService with the given Clock and Ticker.
   *
   * @param clock the system clock to capture the initial absolute time
   * @param ticker the ticker used by the internal Stopwatch
   */
  @Inject
  MonotonicTimeService(Clock clock, Ticker ticker) {
    this.clock = clock;
    this.stopwatch = Stopwatch.createUnstarted(ticker);
  }

  @Override
  protected void startUp() {
    // It is important to do these two things in this order, back-to-back.
    baseTimeMillis = clock.millis();
    stopwatch.start();
  }

  @Override
  protected void shutDown() {
    if (stopwatch.isRunning()) {
      stopwatch.stop();
    }
  }

  @Override
  public long currentTimeMillis() {
    // Get the number of nanos since the stopwatch started
    long elapsedNanos = stopwatch.elapsed().toNanos();
    // Convert this, using the java.time tools, into milliseconds
    long elapsedMillis = Duration.ofNanos(elapsedNanos).toMillis();
    // Add this to the original value calculated in startUp
    return baseTimeMillis + elapsedMillis;
  }
}
