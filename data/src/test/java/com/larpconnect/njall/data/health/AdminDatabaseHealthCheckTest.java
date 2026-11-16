package com.larpconnect.njall.data.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.inject.Provider;
import io.dropwizard.metrics5.health.HealthCheck.Result;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AdminDatabaseHealthCheckTest {

  @Test
  @DisplayName("check returns healthy when SELECT 1 succeeds")
  void check_successfulPing_returnsHealthy() {
    var fixture = createFixture();
    var healthCheck = fixture.createHealthCheck();

    var result = healthCheck.check();

    assertThat(result.isHealthy()).isTrue();
    verify(fixture.sessionFactory).openSession();
    verify(fixture.query).setTimeout(1);
  }

  @Test
  @DisplayName("check returns cached result on subsequent calls within cache TTL")
  void check_repeatedCallsWithinTtl_cachesResultAndQueriesOnce() {
    var fixture = createFixture();
    var healthCheck = fixture.createHealthCheck();

    var result1 = healthCheck.check();
    var result2 = healthCheck.check();

    assertThat(result1.isHealthy()).isTrue();
    assertThat(result2.isHealthy()).isTrue();
    verify(fixture.sessionFactory, times(1)).openSession();
  }

  @Test
  @DisplayName("check re-queries database when cache TTL expires")
  void check_afterTtlExpiration_requeriesDatabase() {
    var fixture = createFixture();
    var healthCheck = fixture.createHealthCheck();

    var result1 = healthCheck.check();
    assertThat(result1.isHealthy()).isTrue();
    verify(fixture.sessionFactory, times(1)).openSession();

    fixture.fakeTicker.advance(Duration.ofSeconds(11));

    var result2 = healthCheck.check();
    assertThat(result2.isHealthy()).isTrue();
    verify(fixture.sessionFactory, times(2)).openSession();
  }

  @Test
  @DisplayName("custom cache TTL constructor expires entries according to configured duration")
  void check_customCacheTtl_expiresAfterCustomDuration() {
    var fixture = createFixture();
    var healthCheck =
        new AdminDatabaseHealthCheck(
            fixture.sessionFactoryProvider, fixture.fakeTicker, Duration.ofSeconds(3));

    assertThat(healthCheck.check().isHealthy()).isTrue();
    fixture.fakeTicker.advance(Duration.ofSeconds(2));
    assertThat(healthCheck.check().isHealthy()).isTrue();
    verify(fixture.sessionFactory, times(1)).openSession();

    fixture.fakeTicker.advance(Duration.ofSeconds(2));
    assertThat(healthCheck.check().isHealthy()).isTrue();
    verify(fixture.sessionFactory, times(2)).openSession();
  }

  @Test
  @DisplayName("check returns unhealthy when database returns unexpected result")
  void check_unexpectedQueryResult_returnsUnhealthy() {
    var fixture = createFixture();
    when(fixture.query.getSingleResult()).thenReturn(0);
    var healthCheck = fixture.createHealthCheck();

    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("Unexpected database ping response: 0");
  }

  @Test
  @DisplayName("check returns unhealthy when database returns null result")
  void check_nullQueryResult_returnsUnhealthy() {
    var fixture = createFixture();
    when(fixture.query.getSingleResult()).thenReturn(null);
    var healthCheck = fixture.createHealthCheck();

    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("Unexpected database ping response: null");
  }

  @Test
  @DisplayName("check returns unhealthy when query execution throws an exception")
  void check_queryThrowsException_returnsUnhealthy() {
    var fixture = createFixture();
    when(fixture.query.getSingleResult()).thenThrow(new RuntimeException("Connection closed"));
    var healthCheck = fixture.createHealthCheck();

    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getError()).isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("check returns unhealthy when session factory openSession fails")
  void check_openSessionFails_returnsUnhealthy() {
    var fixture = createFixture();
    when(fixture.sessionFactory.openSession())
        .thenThrow(new RuntimeException("Connection pool exhausted"));
    var healthCheck = fixture.createHealthCheck();

    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getError()).isInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("inject constructor initializes with ticker, executor, and default cache TTL")
  void constructor_injectParameters_createsHealthyInstance() {
    var fixture = createFixture();
    try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
      var healthCheck =
          new AdminDatabaseHealthCheck(fixture.sessionFactoryProvider, Ticker.systemTicker(), exec);

      var result = healthCheck.check();

      assertThat(result.isHealthy()).isTrue();
    }
  }

  @Test
  @DisplayName("check bounds hung openSession within 1 second and interrupts worker virtual thread")
  void check_hungOpenSession_timesOutWithinOneSecond() throws Exception {
    var fixture = createFixture();
    var workerInterrupted = new CountDownLatch(1);
    when(fixture.sessionFactory.openSession())
        .thenAnswer(
            _ -> {
              try {
                Thread.sleep(10_000);
                return fixture.session;
              } catch (InterruptedException ex) {
                workerInterrupted.countDown();
                throw ex;
              }
            });
    var healthCheck = fixture.createHealthCheck();

    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("Database health check timed out after 1 second");
    assertThat(workerInterrupted.await(2, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @DisplayName("check returns unhealthy when probe executor rejects task")
  void check_executorRejectsTask_returnsUnhealthy() {
    var fixture = createFixture();
    Executor rejectingExecutor =
        _ -> {
          throw new RejectedExecutionException("Shutting down");
        };
    var healthCheck =
        new AdminDatabaseHealthCheck(
            fixture.sessionFactoryProvider, Ticker.systemTicker(), rejectingExecutor);

    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("Database health check rejected by executor");
  }

  @Test
  @DisplayName("check handles Error thrown during ping via ExecutionException safely")
  void check_openSessionThrowsError_returnsUnhealthy() {
    var fixture = createFixture();
    when(fixture.sessionFactory.openSession()).thenThrow(new AssertionError("Fatal driver error"));
    var healthCheck = fixture.createHealthCheck();

    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getError()).isInstanceOf(AssertionError.class);
  }

  @Test
  @DisplayName("check restores interrupt flag and skips caching when caller interrupted mid-ping")
  void check_callerInterruptedMidPing_restoresInterruptAndDoesNotCache() throws Exception {
    var fixture = createFixture();
    var started = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    when(fixture.query.getSingleResult())
        .thenAnswer(
            _ -> {
              started.countDown();
              assertThat(release.await(5, TimeUnit.SECONDS)).isFalse();
              return 1;
            })
        .thenReturn(1);
    var healthCheck = fixture.createHealthCheck();

    InterruptedOutcome outcome;
    try (var callerExec = Executors.newVirtualThreadPerTaskExecutor()) {
      var future =
          callerExec.submit(
              () -> {
                interruptOnceStarted(started, Thread.currentThread());
                var res = healthCheck.check();
                return new InterruptedOutcome(res, Thread.currentThread().isInterrupted());
              });
      outcome = future.get(3, TimeUnit.SECONDS);
    } finally {
      release.countDown();
    }

    assertThat(outcome.result().isHealthy()).isFalse();
    assertThat(outcome.result().getMessage()).contains("Database health check interrupted");
    assertThat(outcome.interrupted()).isTrue();
    assertThat(healthCheck.check().isHealthy()).isTrue();
  }

  @Test
  @DisplayName("check coalesces concurrent cache misses and handles pre-interrupted callers")
  void check_concurrentMissesAndInterruption_handledWithoutPinning() throws Exception {
    var fixture = createFixture();
    var pingEntered = new CountDownLatch(1);
    var releasePing = new CountDownLatch(1);
    when(fixture.query.getSingleResult())
        .thenAnswer(
            _ -> {
              pingEntered.countDown();
              assertThat(releasePing.await(2, TimeUnit.SECONDS)).isTrue();
              return 1;
            });
    var healthCheck = fixture.createHealthCheck();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first = executor.submit(healthCheck::check);
      assertThat(pingEntered.await(2, TimeUnit.SECONDS)).isTrue();

      var interruptedRes =
          executor.submit(
              () -> {
                Thread.currentThread().interrupt();
                var res = healthCheck.check();
                assertThat(Thread.interrupted()).isTrue();
                return res;
              });
      assertThat(interruptedRes.get(2, TimeUnit.SECONDS).isHealthy()).isFalse();

      var second = executor.submit(healthCheck::check);
      Thread.sleep(25);
      releasePing.countDown();

      assertThat(first.get(2, TimeUnit.SECONDS).isHealthy()).isTrue();
      assertThat(second.get(2, TimeUnit.SECONDS).isHealthy()).isTrue();
    }
    verify(fixture.sessionFactory, times(1)).openSession();
  }

  @Test
  @DisplayName(
      "worker interrupt during ping skips caching without polluting un-interrupted caller thread")
  void check_workerInterruptedDuringPing_skipsCachingWithoutInterruptingCaller() {
    var fixture = createFixture();
    when(fixture.query.getSingleResult())
        .thenAnswer(
            _ -> {
              Thread.currentThread().interrupt();
              return 1;
            })
        .thenReturn(1);
    var healthCheck = fixture.createHealthCheck();

    assertThat(healthCheck.check().isHealthy()).isTrue();
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
    assertThat(healthCheck.check().isHealthy()).isTrue();
    verify(fixture.sessionFactory, times(2)).openSession();
  }

  @Test
  @DisplayName("direct-executor ping leaving caller interrupted skips caching and keeps interrupt")
  void check_directExecutorPingInterruptsCaller_skipsCachingAndPreservesInterrupt() {
    var fixture = createFixture();
    when(fixture.query.getSingleResult())
        .thenAnswer(
            _ -> {
              Thread.currentThread().interrupt();
              return 1;
            })
        .thenReturn(1);
    var healthCheck =
        new AdminDatabaseHealthCheck(
            fixture.sessionFactoryProvider,
            fixture.fakeTicker,
            Runnable::run,
            Duration.ofSeconds(10));

    try {
      assertThat(healthCheck.check().isHealthy()).isTrue();
      assertThat(Thread.interrupted()).isTrue();
      assertThat(healthCheck.check().isHealthy()).isTrue();
      verify(fixture.sessionFactory, times(2)).openSession();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  @DisplayName("Concurrent virtual-thread health probes under pingLock all complete healthy")
  void check_virtualThreadLockContention_allProbesCompleteHealthy() throws Exception {
    var fixture = createFixture();
    when(fixture.query.getSingleResult())
        .thenAnswer(
            _ -> {
              Thread.sleep(15);
              return 1;
            });
    var healthCheck =
        new AdminDatabaseHealthCheck(
            fixture.sessionFactoryProvider, fixture.fakeTicker, Duration.ofNanos(1));

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures =
          IntStream.range(0, 10)
              .mapToObj(
                  _ ->
                      executor.submit(
                          () -> {
                            fixture.fakeTicker.advance(Duration.ofMillis(1));
                            return healthCheck.check().isHealthy();
                          }))
              .toList();
      for (var future : futures) {
        assertThat(future.get(5, TimeUnit.SECONDS)).isTrue();
      }
    }
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

  private record InterruptedOutcome(Result result, boolean interrupted) {}

  // Unchecked cast required for mocking generic Provider and NativeQuery types with Mockito.
  @SuppressWarnings("unchecked")
  private static TestFixture createFixture() {
    var sessionFactoryProvider = (Provider<SessionFactory>) mock(Provider.class);
    var sessionFactory = mock(SessionFactory.class);
    var session = mock(Session.class);
    var query = (NativeQuery<Integer>) mock(NativeQuery.class);
    var fakeTicker = new FakeTicker();

    when(sessionFactoryProvider.get()).thenReturn(sessionFactory);
    when(sessionFactory.openSession()).thenReturn(session);
    when(session.createNativeQuery(anyString(), eq(Integer.class))).thenReturn(query);
    when(query.setTimeout(anyInt())).thenReturn(query);
    when(query.getSingleResult()).thenReturn(1);

    return new TestFixture(sessionFactoryProvider, sessionFactory, session, query, fakeTicker);
  }

  private record TestFixture(
      Provider<SessionFactory> sessionFactoryProvider,
      SessionFactory sessionFactory,
      Session session,
      NativeQuery<Integer> query,
      FakeTicker fakeTicker) {

    AdminDatabaseHealthCheck createHealthCheck() {
      return new AdminDatabaseHealthCheck(sessionFactoryProvider, fakeTicker);
    }
  }

  private static final class FakeTicker implements Ticker {
    private final AtomicLong nanos = new AtomicLong();

    @Override
    public long read() {
      return nanos.get();
    }

    void advance(Duration duration) {
      nanos.addAndGet(duration.toNanos());
    }
  }
}
