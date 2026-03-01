package com.larpconnect.njall.common.id;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.util.concurrent.AbstractIdleService;
import com.larpconnect.njall.common.time.TimeService;
import jakarta.inject.Provider;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class UuidV7GeneratorTest {

  private static final class FakeTimeService extends AbstractIdleService implements TimeService {
    long timeMs = 1000L;

    @Override
    public long monotonicNowMillis() {
      return timeMs;
    }

    @Override
    protected void startUp() throws Exception {}

    @Override
    protected void shutDown() throws Exception {}
  }

  private static final class FakeRandomGenerator implements RandomGenerator {
    long nextLongVal = 10L;
    long nextBoundedLongVal = 10L;

    @Override
    public long nextLong() {
      return nextLongVal;
    }

    @Override
    public long nextLong(long origin, long bound) {
      return nextBoundedLongVal;
    }
  }

  private FakeTimeService fakeTimeService;
  private FakeRandomGenerator fakeRandom;
  private UuidV7Generator generator;

  @BeforeEach
  void setUp() {
    fakeTimeService = new FakeTimeService();
    fakeRandom = new FakeRandomGenerator();
    Provider<RandomGenerator> randomProvider = () -> fakeRandom;
    generator = new UuidV7Generator(fakeTimeService, randomProvider);
  }

  @Test
  void generate_validId_success() {
    // Check initial generator state: time = 1000, counter = 10.
    // Call generate: time is still 1000, so counter increments by 7 to 17.
    UUID id = generator.generate();

    assertThat(id.version()).isEqualTo(8);
    assertThat(id.variant()).isEqualTo(2);

    long timestamp = id.getMostSignificantBits() >>> 16;
    assertThat(timestamp).isEqualTo(1000L);

    long counter = id.getMostSignificantBits() & 0xFFF;
    assertThat(counter).isEqualTo(17L);

    long randomBytes = id.getLeastSignificantBits() & 0x3FFFFFFFFFFFFFFFL;
    assertThat(randomBytes).isEqualTo(10L & 0x3FFFFFFFFFFFFFFFL);
  }

  @Test
  void generate_sameTime_incrementsCounter() {
    UUID id1 = generator.generate(); // time = 1000, counter = 17
    UUID id2 = generator.generate(); // time = 1000, counter = 24
    UUID id3 = generator.generate(); // time = 1000, counter = 31

    assertThat(id1.getMostSignificantBits() & 0xFFF).isEqualTo(17L);
    assertThat(id2.getMostSignificantBits() & 0xFFF).isEqualTo(24L);
    assertThat(id3.getMostSignificantBits() & 0xFFF).isEqualTo(31L);
  }

  @Test
  void generate_many_wrapsCounter() {
    // Generate until we wrap 4096
    long lastCounter = 0;
    for (int i = 0; i < 600; i++) {
      UUID id = generator.generate();
      long counter = id.getMostSignificantBits() & 0xFFF;
      if (i > 0) {
        assertThat(counter).isEqualTo((lastCounter + 7) & 0xFFF);
      }
      lastCounter = counter;
    }
  }

  @Test
  void generate_timeIncreases_reinitializesCounter() {
    // First generation (time = 1000, counter = 17)
    UUID id1 = generator.generate();
    assertThat(id1.getMostSignificantBits() & 0xFFF).isEqualTo(17L);

    // Advance time
    fakeTimeService.timeMs = 1001L;
    fakeRandom.nextBoundedLongVal = 50L;

    // Time has increased, counter should be re-initialized from random (50L)
    UUID id2 = generator.generate();
    assertThat(id2.getMostSignificantBits() >>> 16).isEqualTo(1001L);
    assertThat(id2.getMostSignificantBits() & 0xFFF).isEqualTo(50L);

    // Same time, increment by 7
    UUID id3 = generator.generate();
    assertThat(id3.getMostSignificantBits() >>> 16).isEqualTo(1001L);
    assertThat(id3.getMostSignificantBits() & 0xFFF).isEqualTo(57L);
  }

  @Test
  void generate_timeGoesBackwards_usesLastTime() {
    UUID id1 = generator.generate(); // time = 1000, counter = 17
    assertThat(id1.getMostSignificantBits() >>> 16).isEqualTo(1000L);
    assertThat(id1.getMostSignificantBits() & 0xFFF).isEqualTo(17L);

    // Time goes backward (simulate clock drift)
    fakeTimeService.timeMs = 999L;

    // Should use previous time and increment counter
    UUID id2 = generator.generate();
    assertThat(id2.getMostSignificantBits() >>> 16).isEqualTo(1000L); // Time preserved
    assertThat(id2.getMostSignificantBits() & 0xFFF).isEqualTo(24L); // Counter incremented
  }

  @Test
  void generate_concurrent_success() throws InterruptedException {
    fakeTimeService.timeMs = 2000L;
    /*
     * With 12 bits we can only have 4096 unique values if time is constant and random is constant.
     * The test was failing with 4096 unique items because fakeRandom always returns 10,
     * so `UUID` only varies by the 12-bit counter (4096 values). The random bits are 10.
     * Let's advance time or let it fail?
     * In our test fake random returns 10L always.
     * So the UUID is completely deterministic based on time and counter.
     * If we want more than 4096 unique values, we need time to advance or fake random to give new
     * numbers.
     * Since we are generating 10,000 UUIDs and time is frozen, the counter wraps around after 4096
     * times
     * and produces duplicate UUIDs since time isn't advancing and fakeRandom is static!
     * Let's use ThreadLocalRandom for the randomProvider in this specific test.
     */

    generator = new UuidV7Generator(fakeTimeService, ThreadLocalRandom::current);

    int numThreads = 10;
    int numIdsPerThread = 1000;

    CountDownLatch latch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(numThreads);
    var ids = java.util.concurrent.ConcurrentHashMap.<UUID>newKeySet();

    for (int i = 0; i < numThreads; i++) {
      new Thread(
              () -> {
                try {
                  latch.await();
                  for (int j = 0; j < numIdsPerThread; j++) {
                    ids.add(generator.generate());
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                } finally {
                  doneLatch.countDown();
                }
              })
          .start();
    }

    latch.countDown();
    boolean done = doneLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
    assertThat(done).isTrue();

    assertThat(ids).hasSize(numThreads * numIdsPerThread);
  }
}
