package com.larpconnect.njall.queue.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.larpconnect.njall.common.CommonModule;
import com.larpconnect.njall.queue.QueueModule;
import com.larpconnect.njall.queue.config.QueueConfig;
import com.larpconnect.njall.queue.health.AmqpHealthCheck;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Publisher;
import com.rabbitmq.client.amqp.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AmqpQueueServiceConcurrencyTest {

  private static final QueueConfig ENABLED_CONFIG =
      new QueueConfig(true, "amqp.node", 5672, "njall_amqp", "secret", "njall.inbox.activities");

  @Test
  @DisplayName("startUp aborts cleanly and preserves interrupt flag when caller is pre-interrupted")
  void startUp_preInterruptedCaller_abortsAndPreservesInterrupt() {
    var fixture = AmqpQueueServiceTest.createServiceFixture();
    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory());

    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(service::startUp)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Interrupted while acquiring AMQP startup lock");
      assertThat(Thread.interrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }

    assertThat(service.publisher()).isNull();
    assertThat(service.isConnected()).isFalse();
    verify(fixture.environmentFactory(), never()).createEnvironment();
  }

  @Test
  @DisplayName(
      "environment() and connection() exit promptly and preserve interrupt when awaiting lock")
  void environmentAndConnection_interruptedAwaitingLock_throwAndPreserveInterrupt()
      throws Exception {
    var fixture = AmqpQueueServiceTest.createServiceFixture();
    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory());
    service.startAsync().awaitRunning();
    service.invalidatePublisher(fixture.publisher());

    var declareEntered = new CountDownLatch(1);
    var releaseDeclare = new CountDownLatch(1);
    when(fixture.queueSpec().declare())
        .thenAnswer(
            _ -> {
              declareEntered.countDown();
              assertThat(releaseDeclare.await(5, TimeUnit.SECONDS)).isTrue();
              return null;
            });

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var holder = executor.submit(service::publisher);
      assertThat(declareEntered.await(5, TimeUnit.SECONDS)).isTrue();

      var envInterrupted =
          executor.submit(
              () -> {
                Thread.currentThread().interrupt();
                assertThatThrownBy(service::environment)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Interrupted while acquiring AMQP environment lock");
                return Thread.interrupted();
              });
      var connInterrupted =
          executor.submit(
              () -> {
                Thread.currentThread().interrupt();
                assertThatThrownBy(service::connection)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Interrupted while acquiring AMQP connection lock");
                return Thread.interrupted();
              });

      assertThat(envInterrupted.get(5, TimeUnit.SECONDS)).isTrue();
      assertThat(connInterrupted.get(5, TimeUnit.SECONDS)).isTrue();
      releaseDeclare.countDown();
      holder.get(5, TimeUnit.SECONDS);
    }

    service.stopAsync().awaitTerminated();
  }

  @Test
  @DisplayName(
      "buildBrokerSession aborts between stages when interrupted and closes partial resources")
  void buildBrokerSession_interruptedMidBuild_closesPartialResourcesAndPreservesInterrupt() {
    var fixture = AmqpQueueServiceTest.createServiceFixture();
    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory());
    service.startAsync().awaitRunning();
    service.invalidatePublisher(fixture.publisher());

    var interruptDuringConnBuild = new AtomicBoolean(true);
    var interruptDuringPubBuild = new AtomicBoolean(false);
    var reconnectedPublisher = mock(Publisher.class);

    when(fixture.connectionBuilder().build())
        .thenAnswer(
            _ -> {
              if (interruptDuringConnBuild.compareAndSet(true, false)) {
                Thread.currentThread().interrupt();
              }
              return fixture.connection();
            });
    when(fixture.publisherBuilder().build())
        .thenAnswer(
            _ -> {
              if (interruptDuringPubBuild.compareAndSet(true, false)) {
                Thread.currentThread().interrupt();
              }
              return reconnectedPublisher;
            });

    try {
      assertThat(service.publisher()).isNull();
      assertThat(Thread.interrupted()).isTrue();
      verify(fixture.queueSpec(), times(1)).declare();

      interruptDuringPubBuild.set(true);
      assertThat(service.publisher()).isNull();
      assertThat(Thread.interrupted()).isTrue();
      verify(reconnectedPublisher, times(1)).close();

      assertThat(service.publisher()).isSameAs(reconnectedPublisher);
    } finally {
      Thread.interrupted();
      service.stopAsync().awaitTerminated();
    }
  }

  @Test
  @DisplayName(
      "1-second health probe timeout invalidates unresponsive session even while declare blocks")
  void healthProbeTimeout_hungDeclare_immediatelyInvalidatesSessionForReconnect() {
    var fixture = AmqpQueueServiceTest.createServiceFixture();
    var secondPublisher = mock(Publisher.class);
    stubConnectionInfo(fixture.connection());
    when(fixture.publisherBuilder().build())
        .thenReturn(fixture.publisher())
        .thenReturn(secondPublisher);

    var injector = createQueueInjector(fixture.environmentFactory());
    var service = injector.getInstance(AmqpQueueService.class);
    var healthCheck = injector.getInstance(AmqpHealthCheck.class);
    service.startAsync().awaitRunning();
    assertThat(service.publisher()).isSameAs(fixture.publisher());

    var unblockHungDeclare = new CountDownLatch(1);
    var hangNextDeclare = new AtomicBoolean(true);
    when(fixture.queueSpec().declare())
        .thenAnswer(
            _ -> {
              if (hangNextDeclare.compareAndSet(true, false)) {
                awaitIgnoringFirstInterrupt(unblockHungDeclare);
              }
              return null;
            });

    try {
      var result = healthCheck.check();
      assertThat(result.isHealthy()).isFalse();
      assertThat(result.getMessage()).contains("AMQP broker probe timed out after 1 second");

      unblockHungDeclare.countDown();
      assertThat(service.publisher()).isSameAs(secondPublisher);
      verify(fixture.environmentFactory(), times(2)).createEnvironment();
    } finally {
      unblockHungDeclare.countDown();
      service.stopAsync().awaitTerminated();
    }
  }

  @Test
  @DisplayName(
      "External interruption of health check caller does not invalidate a healthy AMQP session")
  void healthCheck_externalCallerInterruption_preservesHealthySession() throws Exception {
    var fixture = AmqpQueueServiceTest.createServiceFixture();
    stubConnectionInfo(fixture.connection());

    var injector = createQueueInjector(fixture.environmentFactory());
    var service = injector.getInstance(AmqpQueueService.class);
    var healthCheck = injector.getInstance(AmqpHealthCheck.class);
    service.startAsync().awaitRunning();

    var declareStarted = new CountDownLatch(1);
    var releaseDeclare = new CountDownLatch(1);
    var pauseFirstProbe = new AtomicBoolean(true);
    when(fixture.queueSpec().declare())
        .thenAnswer(
            _ -> {
              if (pauseFirstProbe.compareAndSet(true, false)) {
                declareStarted.countDown();
                try {
                  assertThat(releaseDeclare.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException _) {
                  // Simulate transport catching InterruptedException without restoring flag
                  throw new RuntimeException("Transport aborted without interrupt flag");
                }
              }
              return null;
            });

    try (var callerExec = Executors.newVirtualThreadPerTaskExecutor()) {
      var future =
          callerExec.submit(
              () -> {
                interruptOnceStarted(declareStarted, Thread.currentThread());
                var res = healthCheck.check();
                return Thread.interrupted() && !res.isHealthy();
              });
      assertThat(future.get(3, TimeUnit.SECONDS)).isTrue();
    } finally {
      releaseDeclare.countDown();
    }

    assertThat(service.publisher()).isSameAs(fixture.publisher());
    assertThat(healthCheck.check().isHealthy()).isTrue();
    verify(fixture.environmentFactory(), times(1)).createEnvironment();
    service.stopAsync().awaitTerminated();
  }

  @Test
  @DisplayName("Overlapping timed-out probe never invalidates a newly established healthy session")
  void healthProbeTimeout_overlappingAfterSessionReplacement_preservesNewSession()
      throws Exception {
    var fixture = AmqpQueueServiceTest.createServiceFixture();
    var secondPublisher = mock(Publisher.class);
    stubConnectionInfo(fixture.connection());
    when(fixture.publisherBuilder().build())
        .thenReturn(fixture.publisher())
        .thenReturn(secondPublisher);

    var injector = createQueueInjector(fixture.environmentFactory());
    var service = injector.getInstance(AmqpQueueService.class);
    var healthCheck = injector.getInstance(AmqpHealthCheck.class);
    service.startAsync().awaitRunning();

    var unblockStuckProbe = new CountDownLatch(1);
    var hangFirstProbe = new AtomicBoolean(true);
    when(fixture.queueSpec().declare())
        .thenAnswer(
            _ -> {
              if (hangFirstProbe.compareAndSet(true, false)) {
                awaitIgnoringFirstInterrupt(unblockStuckProbe);
                throw new RuntimeException("Stale probe eventually failed");
              }
              return null;
            });

    try {
      assertThat(healthCheck.check().isHealthy()).isFalse();
      assertThat(service.publisher()).isSameAs(secondPublisher);

      healthCheck.invalidateCache();
      assertThat(healthCheck.check().isHealthy()).isTrue();

      unblockStuckProbe.countDown();
      Thread.sleep(25);
      assertThat(service.publisher()).isSameAs(secondPublisher);
      verify(fixture.environmentFactory(), times(2)).createEnvironment();
    } finally {
      unblockStuckProbe.countDown();
      service.stopAsync().awaitTerminated();
    }
  }

  @Test
  @DisplayName("Duplicate StateListener transitions and mid-connection-build invalidation abort")
  void stateListener_duplicateAndMidConnectionBuild_abortsCleanly() {
    var fixture = AmqpQueueServiceTest.createServiceFixture();
    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory());

    service.invalidatePublisher(fixture.publisher());
    service.startAsync().awaitRunning();

    var closingCtx = mock(Resource.Context.class);
    when(closingCtx.currentState()).thenReturn(Resource.State.CLOSING);
    fixture.listeners().getFirst().handle(closingCtx);
    fixture.listeners().getFirst().handle(closingCtx);
    service.invalidatePublisher(fixture.publisher());

    var fireInConnBuild = new AtomicBoolean(true);
    when(fixture.connectionBuilder().build())
        .thenAnswer(
            _ -> {
              if (fireInConnBuild.compareAndSet(true, false)) {
                fixture.listeners().getLast().handle(closingCtx);
              }
              return fixture.connection();
            });
    assertThat(service.publisher()).isNull();
    service.stopAsync().awaitTerminated();
  }

  @Test
  @DisplayName(
      "probeConnection handles pre-invalidated session, mid-declare invalidation, and interrupt")
  void probeConnection_preInvalidatedMidDeclareAndInterruptedThrow_handledCleanly()
      throws Exception {
    var fixture = AmqpQueueServiceTest.createServiceFixture();
    stubConnectionInfo(fixture.connection());
    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory());
    service.startAsync().awaitRunning();

    var closingCtx = mock(Resource.Context.class);
    when(closingCtx.currentState()).thenReturn(Resource.State.CLOSING);
    service.invalidatePublisher(fixture.publisher());
    assertThat(service.probeConnection()).isPresent();

    var invalidateInDeclare = new AtomicBoolean(true);
    var interruptAndThrowInDeclare = new AtomicBoolean(false);
    when(fixture.queueSpec().declare())
        .thenAnswer(
            _ -> {
              if (invalidateInDeclare.compareAndSet(true, false)) {
                fixture.listeners().getLast().handle(closingCtx);
                return null;
              }
              if (interruptAndThrowInDeclare.compareAndSet(true, false)) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted Proton RPC");
              }
              return null;
            });
    assertThat(service.probeConnection()).isPresent();

    interruptAndThrowInDeclare.set(true);
    try {
      assertThatThrownBy(service::probeConnection).isInstanceOf(InterruptedException.class);
      when(fixture.queueSpec().declare())
          .thenThrow(new RuntimeException(new InterruptedException("wrapped proton interrupt")));
      Thread.interrupted();
      assertThatThrownBy(service::probeConnection).isInstanceOf(InterruptedException.class);
    } finally {
      Thread.interrupted();
      service.stopAsync().awaitTerminated();
    }
  }

  @Test
  @DisplayName("Virtual-thread contention across reconnect and health probes completes healthy")
  void virtualThreadContention_reconnectAndHealthProbe_completesHealthy() throws Exception {
    var fixture = AmqpQueueServiceTest.createServiceFixture();
    stubConnectionInfo(fixture.connection());
    when(fixture.queueSpec().declare())
        .thenAnswer(
            _ -> {
              Thread.sleep(15);
              return null;
            });
    var injector = createQueueInjector(fixture.environmentFactory());
    var service = injector.getInstance(AmqpQueueService.class);
    var healthCheck = injector.getInstance(AmqpHealthCheck.class);
    service.startAsync().awaitRunning();

    try {
      runContendedVirtualThreadProbes(service, healthCheck, fixture.publisher());
    } finally {
      service.stopAsync().awaitTerminated();
    }
  }

  private static void runContendedVirtualThreadProbes(
      AmqpQueueService service, AmqpHealthCheck healthCheck, Publisher initialPublisher)
      throws Exception {
    service.invalidatePublisher(initialPublisher);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures =
          IntStream.range(0, 12)
              .mapToObj(
                  i ->
                      executor.submit(
                          () -> {
                            healthCheck.invalidateCache();
                            return (i % 2 == 0)
                                ? service.publisher() != null
                                : healthCheck.check().isHealthy();
                          }))
              .toList();
      for (var f : futures) {
        assertThat(f.get(5, TimeUnit.SECONDS)).isTrue();
      }
    }
  }

  private static void stubConnectionInfo(Connection connection) {
    var info = mock(Connection.ConnectionInfo.class);
    when(info.host()).thenReturn("amqp.node");
    when(info.port()).thenReturn(5672);
    when(info.brokerProductName()).thenReturn("RabbitMQ");
    when(connection.connectionInfo()).thenReturn(info);
  }

  private static void awaitIgnoringFirstInterrupt(CountDownLatch latch)
      throws InterruptedException {
    boolean ignored = false;
    try {
      assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    } catch (InterruptedException _) {
      ignored = true;
      assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
    if (ignored) {
      Thread.currentThread().interrupt();
    }
  }

  private static Injector createQueueInjector(AmqpEnvironmentFactory environmentFactory) {
    return Guice.createInjector(
        Modules.override(new CommonModule(), new QueueModule())
            .with(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bind(QueueConfig.class).toInstance(ENABLED_CONFIG);
                    bind(AmqpEnvironmentFactory.class).toInstance(environmentFactory);
                    bind(Ticker.class).toInstance(Ticker.systemTicker());
                  }
                }));
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
}
