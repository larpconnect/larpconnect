package com.larpconnect.njall.common.id;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;

public class UuidV7Benchmark {

    interface TimeService {
        long monotonicNowMillis();
    }

    interface Provider<T> {
        T get();
    }

    private static final long MIN_COUNTER = 3L;
    private static final long MAX_COUNTER = 1024L;
    private static final long COUNTER_MASK = 0xFFFL;
    private static final long COUNTER_INCREMENT = 7L;
    private static final long TIME_MSB_SHIFT = 16L;
    private static final long VERSION_BITS = 0x8000L;
    private static final long VARIANT_BITS = 0x8000000000000000L;
    private static final long RANDOM_MASK = 0x3FFFFFFFFFFFFFFFL;

    private record State(long timeMs, long counter) {}

    static class GeneratorOriginal {
        private final TimeService timeService;
        private final Provider<RandomGenerator> randomProvider;
        private final AtomicReference<State> state;

        GeneratorOriginal(TimeService timeService, Provider<RandomGenerator> randomProvider) {
            this.timeService = timeService;
            this.randomProvider = randomProvider;
            long initialCounter = randomProvider.get().nextLong(MIN_COUNTER, MAX_COUNTER);
            long initialTime = timeService.monotonicNowMillis();
            this.state = new AtomicReference<>(new State(initialTime, initialCounter));
        }

        public UUID generate() {
            long currentTimeMs = timeService.monotonicNowMillis();

            State updatedState = state.accumulateAndGet(
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
            long randomBytes = randomProvider.get().nextLong();
            long lsb = VARIANT_BITS | (randomBytes & RANDOM_MASK);

            return new UUID(msb, lsb);
        }
    }

    static class GeneratorOptimized {
        private final TimeService timeService;
        private final Provider<RandomGenerator> randomProvider;
        private final AtomicReference<State> state;

        GeneratorOptimized(TimeService timeService, Provider<RandomGenerator> randomProvider) {
            this.timeService = timeService;
            this.randomProvider = randomProvider;
            long initialCounter = randomProvider.get().nextLong(MIN_COUNTER, MAX_COUNTER);
            long initialTime = timeService.monotonicNowMillis();
            this.state = new AtomicReference<>(new State(initialTime, initialCounter));
        }

        public UUID generate() {
            long currentTimeMs = timeService.monotonicNowMillis();

            // Generate unconditionally outside the lambda
            long nextRandomCounter = randomProvider.get().nextLong(MIN_COUNTER, MAX_COUNTER);

            State updatedState = state.accumulateAndGet(
                new State(currentTimeMs, nextRandomCounter),
                (current, update) -> {
                    long newTimeMs = update.timeMs();
                    long newCounter;

                    if (newTimeMs > current.timeMs()) {
                        newCounter = update.counter();
                    } else {
                        newTimeMs = current.timeMs();
                        newCounter = (current.counter() + COUNTER_INCREMENT) & COUNTER_MASK;
                    }
                    return new State(newTimeMs, newCounter);
                });

            long msb = (updatedState.timeMs() << TIME_MSB_SHIFT) | VERSION_BITS | updatedState.counter();
            long randomBytes = randomProvider.get().nextLong();
            long lsb = VARIANT_BITS | (randomBytes & RANDOM_MASK);

            return new UUID(msb, lsb);
        }
    }

    static class SlowRandom implements RandomGenerator {
        @Override
        public long nextLong() {
            try {
                Thread.sleep(0, 100);
            } catch (InterruptedException e) {}
            return java.util.concurrent.ThreadLocalRandom.current().nextLong();
        }
        @Override
        public long nextLong(long origin, long bound) {
             try { Thread.sleep(0, 100); } catch (InterruptedException e) {}
             return java.util.concurrent.ThreadLocalRandom.current().nextLong(origin, bound);
        }
    }

    public static void main(String[] args) throws Exception {
        AtomicReference<Long> timeRef = new AtomicReference<>(0L);
        TimeService ts = () -> timeRef.getAndAccumulate(1L, (c, u) -> c + u);

        Provider<RandomGenerator> randomProvider = SlowRandom::new;

        GeneratorOriginal orig = new GeneratorOriginal(ts, randomProvider);
        GeneratorOptimized opt = new GeneratorOptimized(ts, randomProvider);

        System.out.println("Starting Benchmark...");

        long timeOrig = 0;
        long timeOpt = 0;

        int iters = 10;
        for (int i = 0; i < iters; i++) {
             timeOrig += runTest(orig);
             timeOpt += runTest(opt);
        }

        System.out.println("Original avg:  " + (timeOrig / iters) + " ms");
        System.out.println("Optimized avg: " + (timeOpt / iters) + " ms");
    }

    private static long runTest(Object generator) throws InterruptedException {
        int threads = 64;
        int iter = 500;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        Runnable task = () -> {
            try {
                startLatch.await();
                if (generator instanceof GeneratorOriginal g) {
                    for(int i=0; i<iter; i++) g.generate();
                } else if (generator instanceof GeneratorOptimized g) {
                    for(int i=0; i<iter; i++) g.generate();
                }
            } catch (Exception e) {}
            finally {
                endLatch.countDown();
            }
        };

        for(int i=0; i<threads; i++) {
            new Thread(task).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();
        endLatch.await();
        return System.currentTimeMillis() - start;
    }
}
