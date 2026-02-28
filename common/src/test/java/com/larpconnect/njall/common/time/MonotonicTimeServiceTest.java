package com.larpconnect.njall.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Ticker;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MonotonicTimeServiceTest {

  private TestClock clock;
  private TestTicker ticker;
  private MonotonicTimeService service;

  @BeforeEach
  void setUp() {
    clock = new TestClock();
    ticker = new TestTicker();
    service = new MonotonicTimeService(clock, ticker);
  }

  @Test
  void startup_recordsTime_andStartsStopwatch() {
    clock.setMillis(1000L);

    assertThat(service.state()).isEqualTo(com.google.common.util.concurrent.Service.State.NEW);
    service.startAsync().awaitRunning();
    assertThat(service.state()).isEqualTo(com.google.common.util.concurrent.Service.State.RUNNING);

    // Initial time should just be the base time since ticker hasn't advanced
    assertThat(service.currentTimeMillis()).isEqualTo(1000L);
  }

  @Test
  void currentTimeMillis_advances_withTicker() {
    clock.setMillis(5000L);
    service.startAsync().awaitRunning();

    // Advance ticker by 2 milliseconds (2,000,000 nanos)
    ticker.advance(2_000_000L);
    assertThat(service.currentTimeMillis()).isEqualTo(5002L);

    // Advance ticker by 100 milliseconds
    ticker.advance(100_000_000L);
    assertThat(service.currentTimeMillis()).isEqualTo(5102L);
  }

  @Test
  void shutdown_stops_stopwatch() {
    clock.setMillis(1000L);
    service.startAsync().awaitRunning();
    service.stopAsync().awaitTerminated();
    assertThat(service.state())
        .isEqualTo(com.google.common.util.concurrent.Service.State.TERMINATED);
  }

  private static final class TestClock extends Clock {
    private long millis;

    void setMillis(long millis) {
      this.millis = millis;
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return Instant.ofEpochMilli(millis);
    }
  }

  private static final class TestTicker extends Ticker {
    private final AtomicLong nanos = new AtomicLong(0);

    void advance(long amount) {
      nanos.addAndGet(amount);
    }

    @Override
    public long read() {
      return nanos.get();
    }
  }

  @Test
  void shutDown_whenNotRunning_doesNothing() {
    // Branch coverage for if (stopwatch.isRunning()) { stopwatch.stop(); }
    // The service is not running
    service.shutDown();
    assertThat(service.state()).isEqualTo(com.google.common.util.concurrent.Service.State.NEW);
  }
}
