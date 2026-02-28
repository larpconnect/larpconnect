package com.larpconnect.njall.common.time;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AbstractIdleService;
import com.larpconnect.njall.common.annotations.AiContract;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.time.Clock;
import java.time.Duration;

final class MonotonicTimeService extends AbstractIdleService implements TimeService {
  private final Clock clock;
  private final Provider<Stopwatch> stopwatchProvider;
  private long baseTimeMillis;
  private Stopwatch stopwatch;

  @Inject
  MonotonicTimeService(Clock clock, Provider<Stopwatch> stopwatchProvider) {
    this.clock = clock;
    this.stopwatchProvider = stopwatchProvider;
  }

  @Override
  @AiContract(
      implementationHint = "Initializes the base time and starts the stopwatch sequentially.")
  protected void startUp() throws Exception {
    baseTimeMillis = clock.millis();
    stopwatch = stopwatchProvider.get().start();
  }

  @Override
  @AiContract(implementationHint = "Stops the stopwatch on shutdown if it is running.")
  protected void shutDown() throws Exception {
    if (stopwatch != null && stopwatch.isRunning()) {
      stopwatch.stop();
    }
  }

  @Override
  @AiContract(
      ensure = "$res \\ge 0",
      implementationHint =
          "Calculates monotonic time by adding elapsed stopwatch millis to the base time.")
  public long monotonicNowMillis() {
    if (stopwatch == null) {
      throw new IllegalStateException("MonotonicTimeService is not started");
    }
    Duration elapsed = stopwatch.elapsed();
    return baseTimeMillis + elapsed.toMillis();
  }
}
