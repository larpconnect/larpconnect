package com.larpconnect.njall.queue.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Ticker;
import com.larpconnect.njall.queue.config.QueueConfig;
import com.larpconnect.njall.queue.publisher.AmqpClient;
import com.rabbitmq.client.amqp.Publisher;
import io.dropwizard.metrics5.health.HealthCheck.Result;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AmqpHealthCheckConcurrencyTest {

  private ExecutorService probeExecutor;

  @BeforeEach
  void setUp() {
    probeExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  @AfterEach
  void tearDown() {
    probeExecutor.close();
  }

  private AmqpHealthCheck createHealthCheck(AmqpClient amqpClient, QueueConfig config) {
    return new DefaultAmqpHealthCheck(amqpClient, config, Ticker.systemTicker(), probeExecutor);
  }

  @Test
  @DisplayName("concurrent cache misses coalesce onto a single broker probe via AsyncCache")
  void check_concurrentCacheMisses_coalesceWithoutDuplicateProbes() throws Exception {
    var fixture = AmqpHealthCheckTest.createHealthyFixture();
    var probeEntered = new CountDownLatch(1);
    var releaseProbe = new CountDownLatch(1);
    when(fixture.amqpClient().probeConnection())
        .thenAnswer(
            _ -> {
              probeEntered.countDown();
              assertThat(releaseProbe.await(2, TimeUnit.SECONDS)).isTrue();
              return Optional.of(fixture.connectionInfo());
            });

    var healthCheck =
        new DefaultAmqpHealthCheck(
            fixture.amqpClient(), fixture.config(), fixture.fakeTicker(), probeExecutor);

    try (var callers = Executors.newVirtualThreadPerTaskExecutor()) {
      var f1 = callers.submit(healthCheck::check);
      assertThat(probeEntered.await(2, TimeUnit.SECONDS)).isTrue();
      var f2 = callers.submit(healthCheck::check);
      Thread.sleep(25);
      releaseProbe.countDown();

      assertThat(f1.get(2, TimeUnit.SECONDS).isHealthy()).isTrue();
      assertThat(f2.get(2, TimeUnit.SECONDS).isHealthy()).isTrue();
    }
    verify(fixture.amqpClient(), times(1)).probeConnection();
  }

  @Test
  @DisplayName("check invalidates probed publisher and interrupts worker when probe times out")
  void check_probeTimesOut_invalidatesProbedPublisherAndInterruptsWorker() throws Exception {
    var amqpClient = mock(AmqpClient.class);
    var probedPublisher = mock(Publisher.class);
    var workerInterrupted = new CountDownLatch(1);
    when(amqpClient.isRunning()).thenReturn(true);
    when(amqpClient.probeConnection(any(), any()))
        .thenAnswer(
            inv -> {
              Consumer<Publisher> observer = inv.getArgument(0);
              observer.accept(probedPublisher);
              try {
                Thread.sleep(10_000);
                return Optional.empty();
              } catch (InterruptedException ex) {
                workerInterrupted.countDown();
                throw ex;
              }
            });

    var config =
        new QueueConfig(true, "localhost", 5672, "njall_amqp", "secret", "njall.inbox.activities");

    var healthCheck = createHealthCheck(amqpClient, config);
    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("AMQP broker probe timed out after 1 second");
    verify(amqpClient).invalidatePublisher(probedPublisher);
    assertThat(workerInterrupted.await(2, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @DisplayName(
      "check caches timeout without calling invalidatePublisher when no publisher was observed")
  void check_probeTimesOutWithoutObservedPublisher_doesNotInvalidateAndCachesTimeout()
      throws Exception {
    var amqpClient = mock(AmqpClient.class);
    var unblockProbe = new CountDownLatch(1);
    when(amqpClient.isRunning()).thenReturn(true);
    when(amqpClient.probeConnection(any(), any()))
        .thenAnswer(
            _ -> {
              if (unblockProbe.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Probe should remain blocked until test finishes");
              }
              return Optional.empty();
            });
    var config =
        new QueueConfig(true, "localhost", 5672, "njall_amqp", "secret", "njall.inbox.activities");
    var fakeTicker = new AmqpHealthCheckTest.FakeTicker();
    var healthCheck =
        new DefaultAmqpHealthCheck(
            amqpClient,
            config,
            fakeTicker,
            probeExecutor,
            Duration.ofSeconds(10),
            Duration.ofMillis(25));

    try {
      var result = healthCheck.check();
      assertThat(result.isHealthy()).isFalse();
      assertThat(result.getMessage()).contains("AMQP broker probe timed out after 1 second");
      verify(amqpClient, never()).invalidatePublisher(any());

      assertThat(healthCheck.check().isHealthy()).isFalse();
      verify(amqpClient, times(1)).probeConnection(any(), any());
    } finally {
      unblockProbe.countDown();
    }
  }

  @Test
  @DisplayName("fast probe completion is unaffected when scheduled timeout fires afterwards")
  void check_fastProbeFollowedByDelayedTimeout_retainsHealthyResult() throws Exception {
    var fixture = AmqpHealthCheckTest.createHealthyFixture();
    var healthCheck =
        new DefaultAmqpHealthCheck(
            fixture.amqpClient(),
            fixture.config(),
            fixture.fakeTicker(),
            probeExecutor,
            Duration.ofSeconds(10),
            Duration.ofMillis(10));

    var first = healthCheck.check();
    assertThat(first.isHealthy()).isTrue();

    Thread.sleep(60);

    var second = healthCheck.check();
    assertThat(second.isHealthy()).isTrue();
    verify(fixture.amqpClient(), times(1)).probeConnection();
  }

  @Test
  @DisplayName("check exits immediately when caller thread is already interrupted")
  void check_preInterruptedCaller_exitsWithoutProbing() throws Exception {
    var fixture = AmqpHealthCheckTest.createHealthyFixture();
    var healthCheck = createHealthCheck(fixture.amqpClient(), fixture.config());

    Thread.currentThread().interrupt();
    var preInterrupted = healthCheck.check();

    assertThat(Thread.interrupted()).isTrue();
    assertThat(preInterrupted.isHealthy()).isFalse();
    assertThat(preInterrupted.getMessage()).contains("AMQP broker probe interrupted");
    verify(fixture.amqpClient(), times(0)).probeConnection();
  }

  @Test
  @DisplayName(
      "check restores interrupt flag and avoids poisoning cache when interrupted mid-probe")
  void check_probeInterrupted_restoresInterruptFlagAndDoesNotPoisonCache() throws Exception {
    var started = new CountDownLatch(1);
    var blockForever = new CountDownLatch(1);
    var fixture = AmqpHealthCheckTest.createHealthyFixture();
    when(fixture.amqpClient().probeConnection())
        .thenAnswer(
            _ -> {
              started.countDown();
              assertThat(blockForever.await(5, TimeUnit.SECONDS)).isFalse();
              return Optional.empty();
            })
        .thenReturn(Optional.of(fixture.connectionInfo()));

    var healthCheck = createHealthCheck(fixture.amqpClient(), fixture.config());
    InterruptedProbeOutcome outcome;
    try (var callerExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
      var future =
          callerExecutor.submit(
              () -> {
                interruptOnceStarted(started, Thread.currentThread());
                var checkResult = healthCheck.check();
                return new InterruptedProbeOutcome(
                    checkResult, Thread.currentThread().isInterrupted());
              });
      outcome = future.get(3, TimeUnit.SECONDS);
    } finally {
      blockForever.countDown();
    }

    assertThat(outcome.result().isHealthy()).isFalse();
    assertThat(outcome.result().getMessage()).contains("AMQP broker probe interrupted");
    assertThat(outcome.callerInterrupted()).isTrue();
    verify(fixture.amqpClient(), never()).invalidatePublisher(any());
    assertThat(healthCheck.check().isHealthy()).isTrue();
  }

  @Test
  @DisplayName(
      "worker InterruptedException from probeConnection neither poisons cache nor interrupts"
          + " caller")
  void check_probeConnectionThrowsInterruptedException_doesNotPoisonCache() throws Exception {
    var fixture = AmqpHealthCheckTest.createHealthyFixture();
    when(fixture.amqpClient().probeConnection())
        .thenThrow(new InterruptedException("lock interrupted"))
        .thenReturn(Optional.of(fixture.connectionInfo()));

    var healthCheck =
        new DefaultAmqpHealthCheck(
            fixture.amqpClient(), fixture.config(), fixture.fakeTicker(), probeExecutor);

    var first = healthCheck.check();
    assertThat(first.isHealthy()).isFalse();
    assertThat(first.getMessage()).contains("AMQP broker probe interrupted");
    assertThat(Thread.currentThread().isInterrupted()).isFalse();

    var second = healthCheck.check();
    assertThat(second.isHealthy()).isTrue();
    verify(fixture.amqpClient(), times(2)).probeConnection();
  }

  @Test
  @DisplayName(
      "interrupting one coalesced caller cancels shared probe without interrupting peer caller")
  void check_concurrentCallersOneInterrupted_coalescedCallerReceivesCancellationAndCacheEvicts()
      throws Exception {
    var fixture = AmqpHealthCheckTest.createHealthyFixture();
    var probeEntered = new CountDownLatch(1);
    var releaseProbe = new CountDownLatch(1);
    when(fixture.amqpClient().probeConnection())
        .thenAnswer(
            _ -> {
              probeEntered.countDown();
              boolean released = releaseProbe.await(5, TimeUnit.SECONDS);
              return released ? Optional.of(fixture.connectionInfo()) : Optional.empty();
            })
        .thenReturn(Optional.of(fixture.connectionInfo()));

    var healthCheck =
        new DefaultAmqpHealthCheck(
            fixture.amqpClient(), fixture.config(), fixture.fakeTicker(), probeExecutor);

    InterruptedProbeOutcome outcome1;
    InterruptedProbeOutcome outcome2;
    try (var callers = Executors.newVirtualThreadPerTaskExecutor()) {
      var caller1Thread = new AtomicReference<Thread>();
      var caller2Thread = new AtomicReference<Thread>();
      var caller2Ready = new CountDownLatch(1);
      var f1 = submitCheck(callers, healthCheck, caller1Thread, null);
      assertThat(probeEntered.await(2, TimeUnit.SECONDS)).isTrue();
      var f2 = submitCheck(callers, healthCheck, caller2Thread, caller2Ready);
      assertThat(caller2Ready.await(2, TimeUnit.SECONDS)).isTrue();
      awaitThreadWaiting(Objects.requireNonNull(caller2Thread.get()));

      Objects.requireNonNull(caller1Thread.get()).interrupt();
      outcome1 = f1.get(2, TimeUnit.SECONDS);
      outcome2 = f2.get(2, TimeUnit.SECONDS);
    } finally {
      releaseProbe.countDown();
    }

    assertThat(outcome1.result().isHealthy()).isFalse();
    assertThat(outcome1.result().getMessage()).contains("AMQP broker probe interrupted");
    assertThat(outcome1.callerInterrupted()).isTrue();
    assertThat(outcome2.result().isHealthy()).isFalse();
    assertThat(outcome2.result().getMessage()).contains("AMQP broker probe interrupted");
    assertThat(outcome2.callerInterrupted()).isFalse();
    assertThat(healthCheck.check().isHealthy()).isTrue();
    verify(fixture.amqpClient(), times(2)).probeConnection();
  }

  private record InterruptedProbeOutcome(Result result, boolean callerInterrupted) {}

  private static Future<InterruptedProbeOutcome> submitCheck(
      ExecutorService callers,
      AmqpHealthCheck healthCheck,
      AtomicReference<Thread> threadRef,
      @Nullable CountDownLatch ready) {
    return callers.submit(
        () -> {
          threadRef.set(Thread.currentThread());
          if (ready != null) {
            ready.countDown();
          }
          return new InterruptedProbeOutcome(
              healthCheck.check(), Thread.currentThread().isInterrupted());
        });
  }

  private static void interruptOnceStarted(CountDownLatch started, Thread target) {
    ForkJoinPool.commonPool()
        .execute(
            () -> {
              try {
                if (started.await(2, TimeUnit.SECONDS)) {
                  target.interrupt();
                }
              } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
              }
            });
  }

  private static void awaitThreadWaiting(Thread thread) throws InterruptedException {
    var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
    while (thread.getState() != Thread.State.WAITING
        && thread.getState() != Thread.State.TIMED_WAITING) {
      if (System.nanoTime() >= deadline) {
        throw new AssertionError("Timed out waiting for caller thread to park on future");
      }
      Thread.sleep(5);
    }
  }
}
