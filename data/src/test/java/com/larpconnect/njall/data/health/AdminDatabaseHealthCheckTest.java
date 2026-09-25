package com.larpconnect.njall.data.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.inject.Provider;
import java.time.Duration;
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
  @DisplayName("inject constructor initializes with ticker and default cache TTL")
  void constructor_injectParameters_createsHealthyInstance() {
    var fixture = createFixture();
    var healthCheck =
        new AdminDatabaseHealthCheck(fixture.sessionFactoryProvider, Ticker.systemTicker());

    var result = healthCheck.check();

    assertThat(result.isHealthy()).isTrue();
  }

  @Test
  @DisplayName("constructor throws NullPointerException when sessionFactoryProvider is null")
  void constructor_nullProvider_throwsNullPointerException() {
    var ticker = Ticker.systemTicker();
    assertThatNullPointerException()
        .isThrownBy(() -> new AdminDatabaseHealthCheck(null, Duration.ofSeconds(10), ticker))
        .withMessage("sessionFactoryProvider cannot be null");
  }

  @Test
  @DisplayName("constructor throws NullPointerException when cacheTtl is null")
  void constructor_nullTtl_throwsNullPointerException() {
    var fixture = createFixture();
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new AdminDatabaseHealthCheck(
                    fixture.sessionFactoryProvider, null, fixture.fakeTicker))
        .withMessage("cacheTtl cannot be null");
  }

  @Test
  @DisplayName("constructor throws NullPointerException when ticker is null")
  void constructor_nullTicker_throwsNullPointerException() {
    var fixture = createFixture();
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new AdminDatabaseHealthCheck(
                    fixture.sessionFactoryProvider, Duration.ofSeconds(10), null))
        .withMessage("ticker cannot be null");
  }

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
      return new AdminDatabaseHealthCheck(
          sessionFactoryProvider, Duration.ofSeconds(10), fakeTicker);
    }
  }

  private static final class FakeTicker implements Ticker {
    private long nanos;

    @Override
    public long read() {
      return nanos;
    }

    void advance(Duration duration) {
      this.nanos += duration.toNanos();
    }
  }
}
