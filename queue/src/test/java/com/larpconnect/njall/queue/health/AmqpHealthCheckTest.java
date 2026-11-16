package com.larpconnect.njall.queue.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Ticker;
import com.larpconnect.njall.queue.config.QueueConfig;
import com.larpconnect.njall.queue.publisher.AmqpClient;
import com.rabbitmq.client.amqp.Connection;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AmqpHealthCheckTest {

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

  static AmqpClient createMockAmqpClient() throws Exception {
    var amqpClient = mock(AmqpClient.class);
    when(amqpClient.probeConnection(any(), any())).thenCallRealMethod();
    return amqpClient;
  }

  @Test
  @DisplayName("check returns healthy when queue is disabled")
  void check_disabledQueue_returnsHealthy() {
    var amqpClient = mock(AmqpClient.class);
    var config =
        new QueueConfig(false, "localhost", 5672, "njall_amqp", "", "njall.inbox.activities");

    var healthCheck = createHealthCheck(amqpClient, config);
    var result = healthCheck.check();

    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("AMQP queue disabled");
  }

  @Test
  @DisplayName("check returns unhealthy when queue service is not running")
  void check_serviceNotRunning_returnsUnhealthy() {
    var amqpClient = mock(AmqpClient.class);
    when(amqpClient.isRunning()).thenReturn(false);
    var config =
        new QueueConfig(true, "localhost", 5672, "njall_amqp", "secret", "njall.inbox.activities");

    var healthCheck = createHealthCheck(amqpClient, config);
    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("AMQP queue service is not running");
  }

  @Test
  @DisplayName("check returns unhealthy when queue service is running but not connected")
  void check_serviceRunningNotConnected_returnsUnhealthy() throws Exception {
    var amqpClient = createMockAmqpClient();
    when(amqpClient.isRunning()).thenReturn(true);
    when(amqpClient.probeConnection()).thenReturn(Optional.empty());

    var config =
        new QueueConfig(true, "localhost", 5672, "njall_amqp", "secret", "njall.inbox.activities");

    var healthCheck = createHealthCheck(amqpClient, config);
    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("AMQP broker connection is not active");
  }

  @Test
  @DisplayName("check dispatches connection probe on the injected Executor")
  void check_serviceConnected_dispatchesOnInjectedExecutor() throws Exception {
    var fixture = createHealthyFixture();
    var submissions = new AtomicInteger();
    var insideInjectedExecutor = ThreadLocal.withInitial(() -> false);
    var probedOnInjectedExecutor = new AtomicBoolean();
    when(fixture.amqpClient().probeConnection())
        .thenAnswer(
            _ -> {
              probedOnInjectedExecutor.set(insideInjectedExecutor.get());
              return Optional.of(fixture.connectionInfo());
            });
    Executor countingExecutor =
        task -> {
          submissions.incrementAndGet();
          insideInjectedExecutor.set(true);
          try {
            task.run();
          } finally {
            insideInjectedExecutor.remove();
          }
        };
    var healthCheck =
        new DefaultAmqpHealthCheck(
            fixture.amqpClient(), fixture.config(), fixture.fakeTicker(), countingExecutor);

    var result = healthCheck.check();

    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("AMQP connected to 127.0.0.1:5672 (RabbitMQ)");
    assertThat(submissions.get()).isPositive();
    assertThat(probedOnInjectedExecutor.get()).isTrue();
  }

  @Test
  @DisplayName("check caches within 10s TTL and invalidateCache forces immediate re-probe")
  void check_repeatedCallsWithinTtlAndInvalidateCache_behavesCorrectly() throws Exception {
    var fixture = createHealthyFixture();
    var healthCheck =
        new DefaultAmqpHealthCheck(
            fixture.amqpClient(), fixture.config(), fixture.fakeTicker(), probeExecutor);

    var result1 = healthCheck.check();
    var result2 = healthCheck.check();

    assertThat(result1.isHealthy()).isTrue();
    assertThat(result2.isHealthy()).isTrue();
    verify(fixture.amqpClient(), times(1)).probeConnection();

    healthCheck.invalidateCache();
    assertThat(healthCheck.check().isHealthy()).isTrue();
    verify(fixture.amqpClient(), times(2)).probeConnection();

    fixture.fakeTicker().advance(Duration.ofSeconds(11));
    assertThat(healthCheck.check().isHealthy()).isTrue();
    verify(fixture.amqpClient(), times(3)).probeConnection();
  }

  @Test
  @DisplayName("check returns unhealthy when querying connection info throws exception")
  void check_connectionInfoThrows_returnsUnhealthy() throws Exception {
    var amqpClient = createMockAmqpClient();
    when(amqpClient.isRunning()).thenReturn(true);
    when(amqpClient.probeConnection())
        .thenThrow(new RuntimeException("Socket closed during probe"));

    var config =
        new QueueConfig(true, "localhost", 5672, "njall_amqp", "secret", "njall.inbox.activities");

    var healthCheck = createHealthCheck(amqpClient, config);
    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("Socket closed during probe");
  }

  @Test
  @DisplayName("check returns unhealthy when executor rejects probe task")
  void check_executorRejectsProbe_returnsUnhealthy() {
    var amqpClient = mock(AmqpClient.class);
    when(amqpClient.isRunning()).thenReturn(true);
    var config =
        new QueueConfig(true, "localhost", 5672, "njall_amqp", "secret", "njall.inbox.activities");
    Executor rejectingExecutor =
        _ -> {
          throw new RejectedExecutionException("Executor terminated");
        };

    var healthCheck =
        new DefaultAmqpHealthCheck(amqpClient, config, Ticker.systemTicker(), rejectingExecutor);
    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("AMQP broker probe rejected by executor");
  }

  @Test
  @DisplayName("check handles unexpected probe exception and extractFailureMessage variants")
  void check_probeThrowsUnexpectedException_handlesCauseSafely() throws Exception {
    var amqpClient = createMockAmqpClient();
    when(amqpClient.isRunning()).thenReturn(true);
    when(amqpClient.probeConnection()).thenThrow(new RuntimeException("Unexpected lock error"));

    var config =
        new QueueConfig(true, "localhost", 5672, "njall_amqp", "secret", "njall.inbox.activities");
    var healthCheck = createHealthCheck(amqpClient, config);
    var result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("AMQP broker probe failed: Unexpected lock error");

    assertThat(
            DefaultAmqpHealthCheck.extractFailureMessage(
                new ExecutionException("Wrapper", new IllegalStateException("Root cause"))))
        .isEqualTo("Root cause");
    assertThat(
            DefaultAmqpHealthCheck.extractFailureMessage(
                new ExecutionException("Null cause msg", null)))
        .isEqualTo("Null cause msg");
  }

  static Fixture createHealthyFixture() throws Exception {
    var amqpClient = createMockAmqpClient();
    var connectionInfo = mock(Connection.ConnectionInfo.class);

    when(amqpClient.isRunning()).thenReturn(true);
    when(amqpClient.probeConnection()).thenReturn(Optional.of(connectionInfo));
    when(connectionInfo.host()).thenReturn("127.0.0.1");
    when(connectionInfo.port()).thenReturn(5672);
    when(connectionInfo.brokerProductName()).thenReturn("RabbitMQ");

    var config =
        new QueueConfig(true, "127.0.0.1", 5672, "njall_amqp", "secret", "njall.inbox.activities");
    var fakeTicker = new FakeTicker();

    return new Fixture(amqpClient, connectionInfo, config, fakeTicker);
  }

  record Fixture(
      AmqpClient amqpClient,
      Connection.ConnectionInfo connectionInfo,
      QueueConfig config,
      FakeTicker fakeTicker) {}

  static final class FakeTicker implements Ticker {
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
