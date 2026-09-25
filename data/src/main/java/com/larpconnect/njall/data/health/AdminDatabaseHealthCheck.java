package com.larpconnect.njall.data.health;

import static java.util.Objects.requireNonNull;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import io.dropwizard.metrics5.health.HealthCheck;
import io.dropwizard.metrics5.health.HealthCheck.Result;
import java.time.Duration;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jspecify.annotations.Nullable;

/** Health check probe validating PostgreSQL connectivity for the njall_admin role. */
public final class AdminDatabaseHealthCheck implements HealthCheck {

  static final Duration DEFAULT_CACHE_TTL = Duration.ofSeconds(10);
  private static final int QUERY_TIMEOUT_SECONDS = 1;
  private static final String CACHE_KEY = "admin_db_ping";

  private final Provider<SessionFactory> sessionFactoryProvider;
  private final LoadingCache<String, Result> cache;

  @Inject
  AdminDatabaseHealthCheck(
      @NjallAdmin Provider<SessionFactory> sessionFactoryProvider, Ticker ticker) {
    this(sessionFactoryProvider, DEFAULT_CACHE_TTL, ticker);
  }

  AdminDatabaseHealthCheck(
      Provider<SessionFactory> sessionFactoryProvider, Duration cacheTtl, Ticker ticker) {
    this.sessionFactoryProvider =
        requireNonNull(sessionFactoryProvider, "sessionFactoryProvider cannot be null");
    requireNonNull(cacheTtl, "cacheTtl cannot be null");
    requireNonNull(ticker, "ticker cannot be null");
    this.cache = buildCache(cacheTtl, ticker);
  }

  private LoadingCache<String, Result> buildCache(Duration cacheTtl, Ticker ticker) {
    return Caffeine.newBuilder()
        .expireAfterWrite(cacheTtl)
        .ticker(ticker)
        .build(key -> pingDatabase());
  }

  @Override
  public Result check() {
    return cache.get(CACHE_KEY);
  }

  private Result pingDatabase() {
    try (var session = openAdminSession()) {
      var pingResult = executePing(session);
      return evaluatePingResult(pingResult);
    } catch (Exception e) {
      return Result.unhealthy(e);
    }
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
