package com.larpconnect.njall.common;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Generates UUIDv7 identifiers. */
@Singleton
final class Uuid7Generator implements UuidGenerator {

  private static final int INITIAL_COUNTER_MAX = 4096;
  private static final int NANOS_PER_MILLIS = 1_000_000;
  private static final long TIMESTAMP_MASK = 0xFFFFFFFFFFFFL;
  private static final int TIMESTAMP_SHIFT = 16;
  private static final long VERSION_7 = 7L;
  private static final int VERSION_SHIFT = 12;
  private static final int COUNTER_MIN_INCREMENT = 3;
  private static final int COUNTER_MAX_INCREMENT = 1025;
  private static final int COUNTER_MASK = 0xFFF;
  private static final long RANDOM_MASK = 0x3FFFFFFFFFFFFFFFL;
  private static final int VARIANT_SHIFT = 62;

  private final TimeProvider timeProvider;
  private final RandomProvider randomProvider;

  private final long startMillis;
  private final long startNanos;
  private final AtomicInteger counter;

  @Inject
  Uuid7Generator(TimeProvider timeProvider, RandomProvider randomProvider) {
    this.timeProvider = timeProvider;
    this.randomProvider = randomProvider;

    this.startMillis = timeProvider.currentTimeMillis();
    this.startNanos = timeProvider.nanoTime();
    this.counter = new AtomicInteger(randomProvider.nextInt(1, INITIAL_COUNTER_MAX));
  }

  @Override
  public UUID generate() {
    long currentNanos = timeProvider.nanoTime();
    long elapsedMillis = (currentNanos - startNanos) / NANOS_PER_MILLIS;
    long timestamp = startMillis + elapsedMillis;

    // 48 bits of timestamp
    long msb = (timestamp & TIMESTAMP_MASK) << TIMESTAMP_SHIFT;

    // 4 bits of version (7)
    msb |= (VERSION_7 << VERSION_SHIFT);

    // 12 bits of counter
    int increment = randomProvider.nextInt(COUNTER_MIN_INCREMENT, COUNTER_MAX_INCREMENT);
    int counterValue = counter.getAndAdd(increment) & COUNTER_MASK;
    msb |= counterValue;

    // 2 bits of variant (10 for RFC 4122)
    // 62 bits of random data
    long randomBits = randomProvider.nextLong() & RANDOM_MASK;
    long lsb = (2L << VARIANT_SHIFT) | randomBits;

    return new UUID(msb, lsb);
  }
}
