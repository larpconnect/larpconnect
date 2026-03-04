package com.larpconnect.njall.common.id;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.util.concurrent.AbstractIdleService;
import com.larpconnect.njall.common.time.TimeService;
import jakarta.inject.Provider;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
    var id = generator.generate();

    assertThat(id.version()).isEqualTo(7);
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
    var id1 = generator.generate(); // time = 1000, counter = 17
    var id2 = generator.generate(); // time = 1000, counter = 24
    var id3 = generator.generate(); // time = 1000, counter = 31

    assertThat(id1.getMostSignificantBits() & 0xFFF).isEqualTo(17L);
    assertThat(id2.getMostSignificantBits() & 0xFFF).isEqualTo(24L);
    assertThat(id3.getMostSignificantBits() & 0xFFF).isEqualTo(31L);
  }

  @Test
  void generate_many_wrapsCounter() {
    // Generate until we wrap 4096
    long lastCounter = 0;
    for (int i = 0; i < 600; i++) {
      var id = generator.generate();
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
    var id1 = generator.generate();
    assertThat(id1.getMostSignificantBits() & 0xFFF).isEqualTo(17L);

    // Advance time
    fakeTimeService.timeMs = 1001L;
    fakeRandom.nextBoundedLongVal = 50L;

    // Time has increased, counter should be re-initialized from random (50L)
    var id2 = generator.generate();
    assertThat(id2.getMostSignificantBits() >>> 16).isEqualTo(1001L);
    assertThat(id2.getMostSignificantBits() & 0xFFF).isEqualTo(50L);

    // Same time, increment by 7
    var id3 = generator.generate();
    assertThat(id3.getMostSignificantBits() >>> 16).isEqualTo(1001L);
    assertThat(id3.getMostSignificantBits() & 0xFFF).isEqualTo(57L);
  }

  @Test
  void generate_timeGoesBackwards_usesLastTime() {
    var id1 = generator.generate(); // time = 1000, counter = 17
    assertThat(id1.getMostSignificantBits() >>> 16).isEqualTo(1000L);
    assertThat(id1.getMostSignificantBits() & 0xFFF).isEqualTo(17L);

    // Time goes backward (simulate clock drift)
    fakeTimeService.timeMs = 999L;

    // Should use previous time and increment counter
    var id2 = generator.generate();
    assertThat(id2.getMostSignificantBits() >>> 16).isEqualTo(1000L); // Time preserved
    assertThat(id2.getMostSignificantBits() & 0xFFF).isEqualTo(24L); // Counter incremented
  }

  @Test
  void generate_concurrent_success() throws InterruptedException {
    fakeTimeService.timeMs = 2000L;

    generator = new UuidV7Generator(fakeTimeService, ThreadLocalRandom::current);

    int numThreads = 10;
    int numIdsPerThread = 1000;

    var latch = new CountDownLatch(1);
    var doneLatch = new CountDownLatch(numThreads);
    var ids = ConcurrentHashMap.<UUID>newKeySet();

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
    boolean done = doneLatch.await(5, TimeUnit.SECONDS);
    assertThat(done).isTrue();

    assertThat(ids).hasSize(numThreads * numIdsPerThread);
  }

  @Test
  void generate_validatesBitStructure() {
    fakeTimeService.timeMs = 0x1234567890ABL; // Use an explicit hex time to easily see in bits
    fakeRandom.nextBoundedLongVal = 0xBCDL; // counter
    fakeRandom.nextLongVal = 0xAAAAAAAAAAAAAAAAL; // 101010... pattern

    /*
     * UUIDv7 bits:
     * MSB: 48 bit timestamp + 4 bit version + 12 bit counter/random
     * LSB: 2 bit variant + 62 bit random
     */
    var id = generator.generate();

    long msb = id.getMostSignificantBits();
    long lsb = id.getLeastSignificantBits();

    // Validate timestamp (top 48 bits of MSB)
    assertThat(msb >>> 16).isEqualTo(0x1234567890ABL);

    // Validate version (4 bits in MSB)
    assertThat((msb >>> 12) & 0xF).isEqualTo(7L);

    // Validate counter (lowest 12 bits of MSB)
    assertThat(msb & 0xFFF).isEqualTo(0xBCDL);

    // Validate variant (top 2 bits of LSB)
    assertThat(lsb >>> 62).isEqualTo(2L);

    // Validate random bits (lower 62 bits of LSB)
    assertThat(lsb & 0x3FFFFFFFFFFFFFFFL).isEqualTo(0xAAAAAAAAAAAAAAAAL & 0x3FFFFFFFFFFFFFFFL);
  }
}
