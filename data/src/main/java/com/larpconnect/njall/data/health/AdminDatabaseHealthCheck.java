package com.larpconnect.njall.data.health;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import io.dropwizard.metrics5.health.HealthCheck;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jspecify.annotations.Nullable;

/** Health check probe validating PostgreSQL connectivity for the njall_admin role. */
public final class AdminDatabaseHealthCheck implements HealthCheck {

  static final Duration DEFAULT_CACHE_TTL = Duration.ofSeconds(10);
  static final Duration PROBE_TIMEOUT = Duration.ofSeconds(1);
  private static final Executor DEFAULT_PROBE_EXECUTOR =
      Executors.newVirtualThreadPerTaskExecutor();
  private static final int QUERY_TIMEOUT_SECONDS = 1;
  private static final String CACHE_KEY = "admin_db_ping";

  private final Provider<SessionFactory> sessionFactoryProvider;
  private final Executor probeExecutor;
  private final Cache<String, Result> cache;
  private final ReentrantLock pingLock = new ReentrantLock();

  private static final class ProbeOutcome {
    final Result result;
    final boolean cacheable;

    ProbeOutcome(Result result, boolean cacheable) {
      this.result = result;
      this.cacheable = cacheable;
    }
  }

  @Inject
  AdminDatabaseHealthCheck(
      @NjallAdmin Provider<SessionFactory> sessionFactoryProvider,
      Ticker ticker,
      @Blocking Executor probeExecutor) {
    this(sessionFactoryProvider, ticker, probeExecutor, DEFAULT_CACHE_TTL);
  }

  AdminDatabaseHealthCheck(Provider<SessionFactory> sessionFactoryProvider, Ticker ticker) {
    this(sessionFactoryProvider, ticker, DEFAULT_CACHE_TTL);
  }

  AdminDatabaseHealthCheck(
      Provider<SessionFactory> sessionFactoryProvider, Ticker ticker, Duration cacheTtl) {
    this(sessionFactoryProvider, ticker, DEFAULT_PROBE_EXECUTOR, cacheTtl);
  }

  AdminDatabaseHealthCheck(
      Provider<SessionFactory> sessionFactoryProvider,
      Ticker ticker,
      Executor probeExecutor,
      Duration cacheTtl) {
    this.sessionFactoryProvider = sessionFactoryProvider;
    this.probeExecutor = probeExecutor;
    this.cache = buildCache(cacheTtl, ticker);
  }

  private static Cache<String, Result> buildCache(Duration cacheTtl, Ticker ticker) {
    return Caffeine.newBuilder().expireAfterWrite(cacheTtl).ticker(ticker).build();
  }

  @Override
  public Result check() {
    var cached = cache.getIfPresent(CACHE_KEY);
    if (cached != null) {
      return cached;
    }
    try {
      pingLock.lockInterruptibly();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return Result.unhealthy("Database health check interrupted: " + ex.getMessage());
    }
    try {
      var rechecked = cache.getIfPresent(CACHE_KEY);
      if (rechecked != null) {
        return rechecked;
      }
      var outcome = runBoundedPing();
      if (outcome.cacheable && !Thread.currentThread().isInterrupted()) {
        cache.put(CACHE_KEY, outcome.result);
      }
      return outcome.result;
    } finally {
      pingLock.unlock();
    }
  }

  private ProbeOutcome runBoundedPing() {
    var probeTask = createProbeTask();
    if (!dispatchProbeTask(probeTask)) {
      return createOutcome(Result.unhealthy("Database health check rejected by executor"), false);
    }
    return awaitProbeResult(probeTask);
  }

  private FutureTask<ProbeOutcome> createProbeTask() {
    return new FutureTask<>(this::pingDatabase);
  }

  private static ProbeOutcome createOutcome(Result result, boolean cacheable) {
    return new ProbeOutcome(result, cacheable);
  }

  private boolean dispatchProbeTask(FutureTask<ProbeOutcome> probeTask) {
    try {
      probeExecutor.execute(probeTask);
      return true;
    } catch (RuntimeException _) {
      return false;
    }
  }

  private static ProbeOutcome awaitProbeResult(FutureTask<ProbeOutcome> probeTask) {
    try {
      return probeTask.get(PROBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException _) {
      probeTask.cancel(true);
      return createOutcome(
          Result.unhealthy("Database health check timed out after 1 second"), true);
    } catch (ExecutionException ex) {
      var cause = ex.getCause();
      return createOutcome(Result.unhealthy(cause != null ? cause : ex), true);
    } catch (InterruptedException ex) {
      probeTask.cancel(true);
      Thread.currentThread().interrupt();
      return createOutcome(
          Result.unhealthy("Database health check interrupted: " + ex.getMessage()), false);
    }
  }

  private ProbeOutcome pingDatabase() {
    Result result;
    try (var session = openAdminSession()) {
      var pingResult = executePing(session);
      result = evaluatePingResult(pingResult);
    } catch (Exception e) {
      result = Result.unhealthy(e);
    }
    return createOutcome(result, !Thread.currentThread().isInterrupted());
  }

  private Session openAdminSession() {
    return sessionFactoryProvider.get().openSession();
  }

  private static @Nullable Integer executePing(Session session) {
    return session
        .createNativeQuery("SELECT 1", Integer.class)
        .setTimeout(QUERY_TIMEOUT_SECONDS)
        .getSingleResult();
  }

  private static Result evaluatePingResult(@Nullable Integer result) {
    if (result != null && result.intValue() == 1) {
      return Result.healthy();
    }
    return Result.unhealthy("Unexpected database ping response: " + result);
  }
}
