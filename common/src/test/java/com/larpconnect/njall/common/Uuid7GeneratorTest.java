package com.larpconnect.njall.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class Uuid7GeneratorTest {

  private static final int INITIAL_COUNTER_MAX = 4096;
  private static final int COUNTER_MIN_INCREMENT = 3;
  private static final int COUNTER_MAX_INCREMENT = 1025;
  private static final int EXPECTED_INITIAL_COUNTER = 1000;
  private static final int EXPECTED_INCREMENT = 10;
  private static final long START_MILLIS = 1672531200000L;
  private static final long START_NANOS = 1000000000L;
  private static final long RANDOM_BITS = 0x123456789ABCDEFL;
  private static final int VERSION_7 = 7;
  private static final int VARIANT_2 = 2;
  private static final int TIMESTAMP_SHIFT = 16;
  private static final int COUNTER_MASK = 0xFFF;
  private static final long RANDOM_MASK = 0x3FFFFFFFFFFFFFFFL;

  @Test
  void generates_uuid_correctly() {
    // Arrange
    Uuid7Generator.RandomIntProvider randomIntProvider =
        (origin, bound) -> {
          if (origin == 1 && bound == INITIAL_COUNTER_MAX) {
            return EXPECTED_INITIAL_COUNTER;
          }
          if (origin == COUNTER_MIN_INCREMENT && bound == COUNTER_MAX_INCREMENT) {
            return EXPECTED_INCREMENT;
          }
          return 0;
        };

    Uuid7Generator generator =
        new Uuid7Generator(
            () -> START_MILLIS, () -> START_NANOS, () -> RANDOM_BITS, randomIntProvider);

    // Act
    UUID uuid1 = generator.generate();

    assertThat(uuid1.version()).isEqualTo(VERSION_7);
    assertThat(uuid1.variant()).isEqualTo(VARIANT_2);

    long timestamp1 = uuid1.getMostSignificantBits() >>> TIMESTAMP_SHIFT;
    assertThat(timestamp1).isEqualTo(START_MILLIS);

    long counter1 = uuid1.getMostSignificantBits() & COUNTER_MASK;
    assertThat(counter1).isEqualTo(EXPECTED_INITIAL_COUNTER);

    long randomPart1 = uuid1.getLeastSignificantBits() & RANDOM_MASK;
    assertThat(randomPart1).isEqualTo(RANDOM_BITS & RANDOM_MASK);
  }

  @Test
  void generates_uuid_withElapsedTime() {
    // Arrange
    Uuid7Generator.RandomIntProvider randomIntProvider =
        (origin, bound) -> {
          if (origin == 1 && bound == INITIAL_COUNTER_MAX) return EXPECTED_INITIAL_COUNTER;
          if (origin == COUNTER_MIN_INCREMENT && bound == COUNTER_MAX_INCREMENT) {
            return EXPECTED_INCREMENT;
          }
          return 0;
        };

    long[] nanos = {1000000000L, 2000000000L, 2500000000L}; // Starts at 1s, then 2s, then 2.5s
    int[] nanosIndex = {0};

    Uuid7Generator generator =
        new Uuid7Generator(
            () -> START_MILLIS,
            () -> nanos[nanosIndex[0]++], // Return next nanoTime
            () -> RANDOM_BITS,
            randomIntProvider);

    // First generation, elapsed is (2000000000 - 1000000000) = 1,000,000,000 ns = 1000 ms
    UUID uuid1 = generator.generate();

    long timestamp1 = uuid1.getMostSignificantBits() >>> TIMESTAMP_SHIFT;
    assertThat(timestamp1).isEqualTo(START_MILLIS + 1000);

    long counter1 = uuid1.getMostSignificantBits() & COUNTER_MASK;
    assertThat(counter1).isEqualTo(EXPECTED_INITIAL_COUNTER);

    // Second generation, elapsed is (2500000000 - 1000000000) = 1,500,000,000 ns = 1500 ms
    UUID uuid2 = generator.generate();

    long timestamp2 = uuid2.getMostSignificantBits() >>> TIMESTAMP_SHIFT;
    assertThat(timestamp2).isEqualTo(START_MILLIS + 1500);

    long counter2 = uuid2.getMostSignificantBits() & COUNTER_MASK;
    assertThat(counter2).isEqualTo(EXPECTED_INITIAL_COUNTER + EXPECTED_INCREMENT);
  }
}
