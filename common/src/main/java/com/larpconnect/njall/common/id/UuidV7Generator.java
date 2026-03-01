package com.larpconnect.njall.common.id;

import com.larpconnect.njall.common.time.TimeService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;

@Singleton
final class UuidV7Generator implements IdGenerator {

  private static final long MIN_COUNTER = 3L;
  private static final long MAX_COUNTER = 1024L;
  private static final long COUNTER_MASK = 0xFFFL;
  private static final long COUNTER_INCREMENT = 7L;
  private static final long TIME_MSB_SHIFT = 16L;
  private static final long VERSION_BITS = 0x8000L;
  private static final long VARIANT_BITS = 0x8000000000000000L;
  private static final long RANDOM_MASK = 0x3FFFFFFFFFFFFFFFL;

  private final TimeService timeService;
  private final Provider<RandomGenerator> randomProvider;
  private final AtomicReference<State> state;

  private record State(long timeMs, long counter) {}

  @Inject
  UuidV7Generator(TimeService timeService, Provider<RandomGenerator> randomProvider) {
    this.timeService = timeService;
    this.randomProvider = randomProvider;
    long initialCounter = randomProvider.get().nextLong(MIN_COUNTER, MAX_COUNTER);
    long initialTime = timeService.monotonicNowMillis();
    this.state = new AtomicReference<>(new State(initialTime, initialCounter));
  }

  @Override
  public UUID generate() {
    long currentTimeMs = timeService.monotonicNowMillis();

    State updatedState =
        state.accumulateAndGet(
            new State(currentTimeMs, 0),
            (current, update) -> {
              long newTimeMs = update.timeMs();
              long newCounter;

              if (newTimeMs > current.timeMs()) {
                newCounter = randomProvider.get().nextLong(MIN_COUNTER, MAX_COUNTER);
              } else {
                newTimeMs = current.timeMs();
                newCounter = (current.counter() + COUNTER_INCREMENT) & COUNTER_MASK;
              }
              return new State(newTimeMs, newCounter);
            });

    long msb = (updatedState.timeMs() << TIME_MSB_SHIFT) | VERSION_BITS | updatedState.counter();

    RandomGenerator random = randomProvider.get();
    long randomBytes = random.nextLong();
    long lsb = VARIANT_BITS | (randomBytes & RANDOM_MASK);

    return new UUID(msb, lsb);
  }
}
