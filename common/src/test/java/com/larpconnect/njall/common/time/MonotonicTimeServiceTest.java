package com.larpconnect.njall.common.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.inject.Guice;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

final class MonotonicTimeServiceTest {

  private static final class FakeClock extends Clock {
    private Instant instant;

    FakeClock(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }

  private static final class FakeTicker extends Ticker {
    private long nanos;

    @Override
    public long read() {
      return nanos;
    }

    void advanceNanos(long time) {
      nanos += time;
    }
  }

  @Test
  void startUp_setsBaseTimeAndStartsStopwatch_success() throws Exception {
    FakeClock clock = new FakeClock(Instant.ofEpochMilli(1000));
    FakeTicker ticker = new FakeTicker();

    MonotonicTimeService service =
        new MonotonicTimeService(clock, () -> Stopwatch.createUnstarted(ticker));

    service.startUp();

    ticker.advanceNanos(5_000_000L); // 5 ms

    assertThat(service.monotonicNowMillis()).isEqualTo(1005L);
  }

  @Test
  void monotonicNowMillis_notStarted_throwsException() {
    FakeClock clock = new FakeClock(Instant.ofEpochMilli(1000));
    FakeTicker ticker = new FakeTicker();
    MonotonicTimeService service =
        new MonotonicTimeService(clock, () -> Stopwatch.createUnstarted(ticker));

    assertThatThrownBy(service::monotonicNowMillis)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("MonotonicTimeService is not started");
  }

  @Test
  void shutDown_stopsStopwatch_success() throws Exception {
    FakeClock clock = new FakeClock(Instant.ofEpochMilli(1000));
    FakeTicker ticker = new FakeTicker();
    MonotonicTimeService service =
        new MonotonicTimeService(clock, () -> Stopwatch.createUnstarted(ticker));
    service.startUp();

    service.shutDown();
    // Re-shutDown shouldn't fail
    service.shutDown();
  }

  @Test
  void configure_bindsService_success() {
    var injector = Guice.createInjector(new TimeModule());
    var service = injector.getInstance(TimeService.class);
    assertThat(service).isInstanceOf(MonotonicTimeService.class);

    var clock = injector.getInstance(Clock.class);
    assertThat(clock).isNotNull();

    var stopwatch = injector.getInstance(Stopwatch.class);
    assertThat(stopwatch).isNotNull();
  }
}
