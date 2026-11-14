package com.larpconnect.njall.queue.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.queue.config.QueueConfig;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.ConnectionBuilder;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Management;
import com.rabbitmq.client.amqp.Publisher;
import com.rabbitmq.client.amqp.PublisherBuilder;
import com.rabbitmq.client.amqp.Resource;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AmqpQueueServiceTest {

  private static final QueueConfig ENABLED_CONFIG =
      new QueueConfig(true, "amqp.node", 5672, "njall_amqp", "secret", "njall.inbox.activities");

  @Test
  @DisplayName("Service startup skips broker connection when disabled")
  void startUp_disabledConfig_skipsConnection() throws Exception {
    var config =
        new QueueConfig(false, "localhost", 5672, "njall_amqp", "", "njall.inbox.activities");
    var service = new DefaultAmqpQueueService(config, mock(AmqpEnvironmentFactory.class));
    service.startAsync().awaitRunning();

    assertThat(service.isRunning()).isTrue();
    assertThat(service.isConnected()).isFalse();
    assertThat(service.publisher()).isNull();
    assertThat(service.probeConnection()).isEmpty();
    assertThatThrownBy(service::environment).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(service::connection).isInstanceOf(IllegalStateException.class);

    service.stopAsync().awaitTerminated();
    assertThat(service.isConnected()).isFalse();
  }

  @Test
  @DisplayName("Service startup declares queue once and serves publisher() lock-free")
  void startUp_enabledConfig_servesFastPathPublisherWithoutPerPublishDeclare() {
    var fixture = createServiceFixture();
    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory);
    service.startAsync().awaitRunning();

    assertThat(service.isRunning()).isTrue();
    assertThat(service.isConnected()).isTrue();
    assertThat(service.environment()).isSameAs(fixture.environment);
    assertThat(service.connection()).isSameAs(fixture.connection);
    for (int i = 0; i < 20; i++) {
      assertThat(service.publisher()).isSameAs(fixture.publisher);
    }

    verify(fixture.connectionBuilder).host("amqp.node");
    verify(fixture.connectionBuilder).port(5672);
    verify(fixture.connectionBuilder).username("njall_amqp");
    verify(fixture.connectionBuilder).password("secret");
    verify(fixture.connectionBuilder)
        .connectionTimeout(DefaultAmqpQueueService.DEFAULT_CONNECTION_TIMEOUT);
    verify(fixture.management, times(1)).queue("njall.inbox.activities");
    verify(fixture.queueSpec, times(1)).declare();
    verify(fixture.publisherBuilder).queue("njall.inbox.activities");
    verify(fixture.publisherBuilder)
        .publishTimeout(DefaultAmqpQueueService.DEFAULT_PUBLISH_TIMEOUT);

    service.stopAsync().awaitTerminated();
    verify(fixture.publisher).close();
    verify(fixture.connection).close();
    verify(fixture.environment).close();
  }

  @Test
  @DisplayName("Service startup fails fast and closes partial resources when declare throws")
  void startUp_brokerFailure_failsServiceStartupAndClosesResources() {
    var fixture = createServiceFixture();
    doThrow(new RuntimeException("Broker queue declaration failed"))
        .when(fixture.queueSpec)
        .declare();

    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory);
    assertThatThrownBy(() -> service.startAsync().awaitRunning())
        .isInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("Broker queue declaration failed");

    assertThat(service.isRunning()).isFalse();
    verify(fixture.connection).close();
    verify(fixture.environment).close();
  }

  @Test
  @DisplayName(
      "Resource.StateListener and invalidatePublisher trigger on-demand reconnect using identity"
          + " CAS")
  void stateListenerAndInvalidatePublisher_triggerReconnectSafely() {
    var fixture = createServiceFixture();
    var secondPublisher = mock(Publisher.class);
    when(fixture.publisherBuilder.build())
        .thenReturn(fixture.publisher)
        .thenReturn(secondPublisher);

    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory);
    service.startAsync().awaitRunning();
    assertThat(service.publisher()).isSameAs(fixture.publisher);
    assertThat(fixture.listeners).isNotEmpty();

    // OPEN transition does not invalidate
    var openCtx = mock(Resource.Context.class);
    when(openCtx.currentState()).thenReturn(Resource.State.OPEN);
    fixture.listeners.getFirst().handle(openCtx);
    assertThat(service.publisher()).isSameAs(fixture.publisher);

    // RECOVERING transition invalidates fastPathPublisher; next publisher() reconnects once
    var recoveringCtx = mock(Resource.Context.class);
    when(recoveringCtx.currentState()).thenReturn(Resource.State.RECOVERING);
    fixture.listeners.getFirst().handle(recoveringCtx);

    assertThat(service.publisher()).isSameAs(secondPublisher);
    verify(fixture.environmentFactory, times(2)).createEnvironment();

    // Stale invalidatePublisher with old publisher instance is a no-op (identity CAS)
    service.invalidatePublisher(fixture.publisher);
    assertThat(service.publisher()).isSameAs(secondPublisher);
    verify(fixture.environmentFactory, times(2)).createEnvironment();

    // Invalidating the active publisher forces reconnect; if broker stays down, degrades cleanly
    when(fixture.queueSpec.declare()).thenThrow(new RuntimeException("Broker offline"));
    service.invalidatePublisher(secondPublisher);
    assertThat(service.publisher()).isNull();
    assertThat(service.isConnected()).isFalse();
    assertThatThrownBy(service::environment).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(service::connection).isInstanceOf(IllegalStateException.class);

    service.stopAsync().awaitTerminated();
  }

  @Test
  @DisplayName(
      "probeConnection verifies topology and reconnects when health probe detects a dead link")
  void probeConnection_verifiesAndReconnects() throws Exception {
    var fixture = createServiceFixture();
    var info = mock(Connection.ConnectionInfo.class);
    when(fixture.connection.connectionInfo()).thenReturn(info);

    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory);
    service.startAsync().awaitRunning();

    assertThat(service.probeConnection()).contains(info);

    // First probe declare fails -> reconnects -> second declare succeeds
    when(fixture.queueSpec.declare())
        .thenThrow(new RuntimeException("Link dropped"))
        .thenReturn(null);
    assertThat(service.probeConnection()).contains(info);
    verify(fixture.environmentFactory, times(2)).createEnvironment();

    // Both probe and reconnect fail -> returns empty Optional
    when(fixture.queueSpec.declare()).thenThrow(new RuntimeException("Broker down"));
    assertThat(service.probeConnection()).isEmpty();

    service.stopAsync().awaitTerminated();
  }

  @Test
  @DisplayName("Service shutdown safely absorbs exceptions when closing resources")
  void shutDown_resourceCloseThrows_absorbsExceptions() {
    var fixture = createServiceFixture();
    doThrow(new RuntimeException("Publisher close failure")).when(fixture.publisher).close();
    doThrow(new RuntimeException("Connection close failure")).when(fixture.connection).close();
    doThrow(new RuntimeException("Environment close failure")).when(fixture.environment).close();

    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory);
    service.startAsync().awaitRunning();

    service.stopAsync().awaitTerminated();
    assertThat(service.isRunning()).isFalse();
    assertThat(service.isConnected()).isFalse();
    assertThat(service.publisher()).isNull();
  }

  @Test
  @DisplayName(
      "isConnected exits promptly and preserves interrupt status when interrupted awaiting lock")
  void isConnected_interruptedAwaitingLock_returnsFalseAndPreservesInterrupt() throws Exception {
    var config =
        new QueueConfig(true, "amqp.node", 5672, "njall_amqp", "secret", "njall.inbox.activities");
    var fixture = createServiceFixture();
    var service = new DefaultAmqpQueueService(config, fixture.environmentFactory);
    service.startAsync().awaitRunning();
    service.invalidatePublisher(fixture.publisher);

    var probeEntered = new CountDownLatch(1);
    var releaseProbe = new CountDownLatch(1);
    when(fixture.queueSpec.declare())
        .thenAnswer(
            _ -> {
              probeEntered.countDown();
              assertThat(releaseProbe.await(5, TimeUnit.SECONDS)).isTrue();
              return null;
            });

    var connectedResult = new AtomicBoolean(true);
    var interruptedObserved = new AtomicBoolean(false);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var holderFuture = executor.submit(service::isConnected);
      assertThat(probeEntered.await(5, TimeUnit.SECONDS)).isTrue();

      var waiterFuture =
          executor.submit(
              () -> {
                Thread.currentThread().interrupt();
                connectedResult.set(service.isConnected());
                interruptedObserved.set(Thread.currentThread().isInterrupted());
              });

      waiterFuture.get(5, TimeUnit.SECONDS);
      releaseProbe.countDown();
      holderFuture.get(5, TimeUnit.SECONDS);
    }

    assertThat(connectedResult.get()).isFalse();
    assertThat(interruptedObserved.get()).isTrue();
    service.stopAsync().awaitTerminated();
  }

  @Test
  @DisplayName(
      "Concurrent publisher() calls after invalidation coalesce into a single reconnect under lock")
  void publisher_concurrentCallsAfterInvalidation_reconnectsOnce() throws Exception {
    var config =
        new QueueConfig(true, "amqp.node", 5672, "njall_amqp", "secret", "njall.inbox.activities");
    var fixture = createServiceFixture();
    var reconnectedPublisher = mock(Publisher.class);
    when(fixture.publisherBuilder.build())
        .thenReturn(fixture.publisher)
        .thenReturn(reconnectedPublisher);

    var service = new DefaultAmqpQueueService(config, fixture.environmentFactory);
    service.startAsync().awaitRunning();
    service.invalidatePublisher(fixture.publisher);

    var declareEntered = new CountDownLatch(1);
    var releaseDeclare = new CountDownLatch(1);
    when(fixture.queueSpec.declare())
        .thenAnswer(
            _ -> {
              declareEntered.countDown();
              assertThat(releaseDeclare.await(5, TimeUnit.SECONDS)).isTrue();
              return null;
            });

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first = executor.submit(service::publisher);
      assertThat(declareEntered.await(5, TimeUnit.SECONDS)).isTrue();
      var second = executor.submit(service::publisher);
      Thread.sleep(25);
      releaseDeclare.countDown();

      assertThat(first.get(5, TimeUnit.SECONDS)).isSameAs(reconnectedPublisher);
      assertThat(second.get(5, TimeUnit.SECONDS)).isSameAs(reconnectedPublisher);
    }

    verify(fixture.environmentFactory, times(2)).createEnvironment();
    service.stopAsync().awaitTerminated();
  }

  @Test
  @DisplayName(
      "StateListener firing during session creation prevents installing an invalidated publisher")
  void stateListenerDuringSessionBuild_preventsInstallingInvalidatedPublisher() {
    var config =
        new QueueConfig(true, "amqp.node", 5672, "njall_amqp", "secret", "njall.inbox.activities");
    var fixture = createServiceFixture();
    var doaPublisher = mock(Publisher.class);
    var healthyPublisher = mock(Publisher.class);
    var fireMidBuild = new AtomicBoolean(true);

    when(fixture.publisherBuilder.build())
        .thenReturn(fixture.publisher)
        .thenAnswer(
            _ -> {
              if (fireMidBuild.compareAndSet(true, false)) {
                var recoveringCtx = mock(Resource.Context.class);
                when(recoveringCtx.currentState()).thenReturn(Resource.State.RECOVERING);
                fixture.listeners.getLast().handle(recoveringCtx);
                return doaPublisher;
              }
              return healthyPublisher;
            });

    var service = new DefaultAmqpQueueService(config, fixture.environmentFactory);
    service.startAsync().awaitRunning();
    service.invalidatePublisher(fixture.publisher);

    // First reconnect sees mid-build invalidation, closes doaPublisher once, returns null
    assertThat(service.publisher()).isNull();
    verify(doaPublisher, times(1)).close();

    // Next publisher() call reconnects cleanly to healthyPublisher
    assertThat(service.publisher()).isSameAs(healthyPublisher);
    service.stopAsync().awaitTerminated();
  }

  @Test
  @DisplayName(
      "Concurrent shutDown during slow reconnect closes newly built session without resource leak")
  void shutDown_concurrentWithSlowReconnect_closesSessionWithoutResourceLeak() throws Exception {
    var config =
        new QueueConfig(true, "amqp.node", 5672, "njall_amqp", "secret", "njall.inbox.activities");
    var fixture = createServiceFixture();
    var latePublisher = mock(Publisher.class);
    var lateConnection = mock(Connection.class);
    var lateEnvironment = mock(Environment.class);

    var service = new DefaultAmqpQueueService(config, fixture.environmentFactory);
    service.startAsync().awaitRunning();
    service.invalidatePublisher(fixture.publisher);

    var buildEntered = new CountDownLatch(1);
    var allowFinish = new CountDownLatch(1);
    when(fixture.environmentFactory.createEnvironment()).thenReturn(lateEnvironment);
    when(lateEnvironment.connectionBuilder()).thenReturn(fixture.connectionBuilder);
    when(fixture.connectionBuilder.build()).thenReturn(lateConnection);
    when(lateConnection.management()).thenReturn(fixture.management);
    when(lateConnection.publisherBuilder()).thenReturn(fixture.publisherBuilder);
    when(fixture.publisherBuilder.build())
        .thenAnswer(
            _ -> {
              buildEntered.countDown();
              assertThat(allowFinish.await(5, TimeUnit.SECONDS)).isTrue();
              return latePublisher;
            });

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var reconnectFuture = executor.submit(service::publisher);
      assertThat(buildEntered.await(5, TimeUnit.SECONDS)).isTrue();

      var shutdownDone =
          executor.submit(
              () -> {
                Thread.currentThread().interrupt();
                service.shutDown();
                return Thread.interrupted();
              });
      assertThat(shutdownDone.get(5, TimeUnit.SECONDS)).isTrue();

      allowFinish.countDown();
      assertThat(reconnectFuture.get(5, TimeUnit.SECONDS)).isNull();
    }

    assertThat(service.publisher()).isNull();
    verify(latePublisher, times(1)).close();
    verify(lateConnection, times(1)).close();
    verify(lateEnvironment, times(1)).close();
  }

  @Test
  @DisplayName("publisher() returns null and preserves interrupt status when interrupted on lock")
  void publisher_interruptedAwaitingLock_returnsNullAndPreservesInterrupt() throws Exception {
    var fixture = createServiceFixture();
    var service = new DefaultAmqpQueueService(ENABLED_CONFIG, fixture.environmentFactory);
    service.startAsync().awaitRunning();
    service.invalidatePublisher(fixture.publisher);

    var declareEntered = new CountDownLatch(1);
    var releaseDeclare = new CountDownLatch(1);
    when(fixture.queueSpec.declare())
        .thenAnswer(
            _ -> {
              declareEntered.countDown();
              assertThat(releaseDeclare.await(5, TimeUnit.SECONDS)).isTrue();
              return null;
            });

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var holder = executor.submit(service::publisher);
      assertThat(declareEntered.await(5, TimeUnit.SECONDS)).isTrue();

      var waiterInterrupted =
          executor.submit(
              () -> {
                Thread.currentThread().interrupt();
                assertThat(service.publisher()).isNull();
                return Thread.interrupted();
              });
      assertThat(waiterInterrupted.get(5, TimeUnit.SECONDS)).isTrue();
      releaseDeclare.countDown();
      holder.get(5, TimeUnit.SECONDS);
    }
    service.stopAsync().awaitTerminated();
  }

  @Test
  @DisplayName("DefaultAmqpEnvironmentFactory creates in-memory Environment cleanly")
  void defaultAmqpEnvironmentFactory_createsEnvironment() {
    var factory = new DefaultAmqpEnvironmentFactory();
    try (var environment = factory.createEnvironment()) {
      assertThat(environment.connectionBuilder()).isInstanceOf(ConnectionBuilder.class);
    }
  }

  static Fixture createServiceFixture() {
    var environmentFactory = mock(AmqpEnvironmentFactory.class);
    var environment = mock(Environment.class);
    var connectionBuilder = mock(ConnectionBuilder.class);
    var connection = mock(Connection.class);
    var management = mock(Management.class);
    var queueSpec = mock(Management.QueueSpecification.class);
    var publisherBuilder = mock(PublisherBuilder.class);
    var publisher = mock(Publisher.class);
    var listeners = new CopyOnWriteArrayList<Resource.StateListener>();

    when(environmentFactory.createEnvironment()).thenReturn(environment);
    when(environment.connectionBuilder()).thenReturn(connectionBuilder);
    stubConnectionBuilder(connectionBuilder, connection, listeners);
    when(connection.management()).thenReturn(management);
    when(management.queue(anyString())).thenReturn(queueSpec);
    stubPublisherBuilder(connection, publisherBuilder, publisher, listeners);

    return new Fixture(
        environmentFactory,
        environment,
        connectionBuilder,
        connection,
        management,
        queueSpec,
        publisherBuilder,
        publisher,
        listeners);
  }

  private static void stubConnectionBuilder(
      ConnectionBuilder builder, Connection connection, List<Resource.StateListener> listeners) {
    when(builder.host(anyString())).thenReturn(builder);
    when(builder.port(anyInt())).thenReturn(builder);
    when(builder.username(anyString())).thenReturn(builder);
    when(builder.password(anyString())).thenReturn(builder);
    when(builder.connectionTimeout(any())).thenReturn(builder);
    doAnswer(
            inv -> {
              listeners.add(inv.getArgument(0));
              return builder;
            })
        .when(builder)
        .listeners(any(Resource.StateListener.class));
    when(builder.build()).thenReturn(connection);
  }

  private static void stubPublisherBuilder(
      Connection conn,
      PublisherBuilder builder,
      Publisher publisher,
      List<Resource.StateListener> listeners) {
    when(conn.publisherBuilder()).thenReturn(builder);
    when(builder.queue(anyString())).thenReturn(builder);
    when(builder.publishTimeout(any())).thenReturn(builder);
    doAnswer(
            inv -> {
              listeners.add(inv.getArgument(0));
              return builder;
            })
        .when(builder)
        .listeners(any(Resource.StateListener.class));
    when(builder.build()).thenReturn(publisher);
  }

  record Fixture(
      AmqpEnvironmentFactory environmentFactory,
      Environment environment,
      ConnectionBuilder connectionBuilder,
      Connection connection,
      Management management,
      Management.QueueSpecification queueSpec,
      PublisherBuilder publisherBuilder,
      Publisher publisher,
      List<Resource.StateListener> listeners) {}
}
