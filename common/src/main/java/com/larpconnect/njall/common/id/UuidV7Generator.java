package com.larpconnect.njall.common.id;

import com.larpconnect.njall.common.time.TimeService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;

@Singleton
final class UuidV7Generator implements IdGenerator {

  private static final long MIN_COUNTER = 3L;
  private static final long MAX_COUNTER = 1024L;
  private static final long TIME_SHIFT = 12L;
  private static final long COUNTER_MASK = 0xFFFL;
  private static final long COUNTER_INCREMENT = 7L;
  private static final long TIME_MSB_SHIFT = 16L;
  private static final long VERSION_BITS = 0x8000L;
  private static final long VARIANT_BITS = 0x8000000000000000L;
  private static final long RANDOM_MASK = 0x3FFFFFFFFFFFFFFFL;

  private final TimeService timeService;
  private final Provider<RandomGenerator> randomProvider;
  private final AtomicLong state;

  @Inject
  UuidV7Generator(TimeService timeService, Provider<RandomGenerator> randomProvider) {
    this.timeService = timeService;
    this.randomProvider = randomProvider;
    long initialCounter = randomProvider.get().nextLong(MIN_COUNTER, MAX_COUNTER);
    long initialTime = timeService.monotonicNowMillis();
    this.state = new AtomicLong((initialTime << TIME_SHIFT) | initialCounter);
  }

  @Override
  public UUID generate() {
    long currentTimeMs;
    long counter;

    while (true) {
      long currentState = state.get();
      long lastTimeMs = currentState >>> TIME_SHIFT;
      long lastCounter = currentState & COUNTER_MASK;
      currentTimeMs = timeService.monotonicNowMillis();

      if (currentTimeMs > lastTimeMs) {
        counter = randomProvider.get().nextLong(MIN_COUNTER, MAX_COUNTER);
      } else {
        currentTimeMs = lastTimeMs;
        counter = (lastCounter + COUNTER_INCREMENT) & COUNTER_MASK;
      }

      long newState = (currentTimeMs << TIME_SHIFT) | counter;
      if (state.compareAndSet(currentState, newState)) {
        break;
      }
    }

    // Spec:
    // 48 bit big endian unsigned number representing the number of ms since unix epoch.
    // Four bits (1000). This is the version.
    // 12 bits representing an incrementing counter specified below.
    long msb = (currentTimeMs << TIME_MSB_SHIFT) | VERSION_BITS | counter;

    // Two bits (10). This is the variant field.
    // 62 pseudorandom bits.
    RandomGenerator random = randomProvider.get();
    long randomBytes = random.nextLong();
    long lsb = (VARIANT_BITS | (randomBytes & RANDOM_MASK));

    return new UUID(msb, lsb);
  }
}
