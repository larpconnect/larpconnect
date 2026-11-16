package com.larpconnect.njall.queue.publisher;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.google.inject.Inject;
import com.larpconnect.njall.queue.config.QueueConfig;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Publisher;
import com.rabbitmq.client.amqp.Resource;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default implementation of {@link AmqpQueueService} managing AMQP 1.0 lifecycle via RabbitMQ. */
final class DefaultAmqpQueueService extends AbstractIdleService implements AmqpQueueService {

  static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(5);
  static final Duration DEFAULT_PUBLISH_TIMEOUT = Duration.ofSeconds(5);
  private static final long SHUTDOWN_LOCK_WAIT_SECONDS = 2L;
  private static final ImmutableSet<Resource.State> INVALIDATING_STATES =
      Sets.immutableEnumSet(
          Resource.State.RECOVERING, Resource.State.CLOSING, Resource.State.CLOSED);

  private final Logger logger = LoggerFactory.getLogger(DefaultAmqpQueueService.class);
  private final ReentrantLock connectionLock = new ReentrantLock();
  private final AtomicBoolean acceptingConnections = new AtomicBoolean(false);
  private final AtomicReference<@Nullable BrokerSession> activeSession = new AtomicReference<>();

  private final QueueConfig config;
  private final AmqpEnvironmentFactory environmentFactory;

  private static final class BrokerSession {
    final Environment environment;
    final Connection connection;
    final Publisher publisher;
    final AtomicBoolean invalidated;

    BrokerSession(
        Environment environment,
        Connection connection,
        Publisher publisher,
        AtomicBoolean invalidated) {
      this.environment = environment;
      this.connection = connection;
      this.publisher = publisher;
      this.invalidated = invalidated;
    }
  }

  @Inject
  DefaultAmqpQueueService(QueueConfig config, AmqpEnvironmentFactory environmentFactory) {
    this.config = config;
    this.environmentFactory = environmentFactory;
  }

  @Override
  protected void startUp() {
    if (!config.enabled()) {
      logger.info("AMQP queue is disabled; skipping connection startup");
      return;
    }
    logger.info("Initializing AMQP 1.0 connection to {}:{}...", config.host(), config.port());
    acceptingConnections.set(true);
    try {
      acquireConnectionLockOrThrow("startup");
    } catch (RuntimeException ex) {
      acceptingConnections.set(false);
      throw ex;
    }
    try {
      openAndInstallSession();
    } catch (RuntimeException ex) {
      acceptingConnections.set(false);
      closeActiveSession();
      throw ex;
    } finally {
      connectionLock.unlock();
    }
  }

  @Override
  protected void shutDown() {
    logger.info("Shutting down AMQP queue service...");
    acceptingConnections.set(false);
    closeActiveSession();
    boolean locked = acquireLockForShutdown();
    try {
      closeActiveSession();
    } finally {
      if (locked) {
        connectionLock.unlock();
      }
    }
    logger.info("AMQP queue service shutdown complete");
  }

  private void acquireConnectionLockOrThrow(String operation) {
    try {
      connectionLock.lockInterruptibly();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw createLockInterruptedException(operation, ex);
    }
  }

  private boolean acquireLockForShutdown() {
    try {
      return connectionLock.tryLock(SHUTDOWN_LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private boolean isReadyForConnections() {
    return config.enabled() && acceptingConnections.get();
  }

  private @Nullable BrokerSession currentHealthySessionSnapshot() {
    var session = activeSession.get();
    if (session == null || session.invalidated.get()) {
      return null;
    }
    return session;
  }

  @GuardedBy("connectionLock")
  private @Nullable BrokerSession ensureOrReconnectUnderLock() {
    if (!isReadyForConnections()) {
      return null;
    }
    var existing = currentHealthySessionSnapshot();
    if (existing != null) {
      return existing;
    }
    logger.info("Reconnecting AMQP 1.0 connection to {}:{}...", config.host(), config.port());
    closeActiveSession();
    try {
      return openAndInstallSession();
    } catch (RuntimeException ex) {
      logger.warn("Failed to connect/reconnect AMQP queue service: {}", ex.getMessage());
      closeActiveSession();
      return null;
    }
  }

  @GuardedBy("connectionLock")
  private BrokerSession openAndInstallSession() {
    var session = buildBrokerSession();
    installBrokerSession(session);
    logger.info(
        "AMQP queue service connected and queue '{}' declared successfully",
        config.inboxQueueName());
    return session;
  }

  @GuardedBy("connectionLock")
  private BrokerSession buildBrokerSession() {
    var invalidated = createInvalidatedFlag();
    var listener = createStateListener(invalidated);
    Environment env = null;
    Connection conn = null;
    Publisher pub = null;
    try {
      ensureBuildCanProceed(invalidated);
      env = openEnvironment();
      ensureBuildCanProceed(invalidated);
      conn = buildConnection(env, listener);
      ensureBuildCanProceed(invalidated);
      declareInboxQueue(conn);
      ensureBuildCanProceed(invalidated);
      pub = buildPublisher(conn, listener);
      return createBrokerSession(env, conn, pub, invalidated);
    } catch (RuntimeException ex) {
      closeResourceQuietly("publisher", pub);
      closeResourceQuietly("connection", conn);
      closeResourceQuietly("environment", env);
      throw ex;
    }
  }

  private void ensureBuildCanProceed(AtomicBoolean invalidated) {
    if (Thread.currentThread().isInterrupted()) {
      throw createUninitializedException("session build interrupted");
    }
    if (!acceptingConnections.get() || invalidated.get()) {
      throw createUninitializedException("service");
    }
  }

  private Environment openEnvironment() {
    return environmentFactory.createEnvironment();
  }

  private void declareInboxQueue(Connection conn) {
    conn.management().queue(config.inboxQueueName()).declare();
  }

  private Connection buildConnection(Environment env, Resource.StateListener listener) {
    var connBuilder = env.connectionBuilder();
    connBuilder.host(config.host());
    connBuilder.port(config.port());
    connBuilder.username(config.username());
    connBuilder.password(config.password());
    connBuilder.connectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
    connBuilder.listeners(listener);
    return connBuilder.build();
  }

  private Publisher buildPublisher(Connection conn, Resource.StateListener listener) {
    var pubBuilder = conn.publisherBuilder();
    pubBuilder.queue(config.inboxQueueName());
    pubBuilder.publishTimeout(DEFAULT_PUBLISH_TIMEOUT);
    pubBuilder.listeners(listener);
    return pubBuilder.build();
  }

  @GuardedBy("connectionLock")
  private void installBrokerSession(BrokerSession session) {
    if (cannotInstallSession(session)) {
      session.invalidated.set(true);
      closeSession(session);
      throw createUninitializedException("service");
    }
    activeSession.set(session);
    if (!acceptingConnections.get() || session.invalidated.get()) {
      if (activeSession.compareAndSet(session, null)) {
        session.invalidated.set(true);
        closeSession(session);
      }
      throw createUninitializedException("service");
    }
  }

  private boolean cannotInstallSession(BrokerSession session) {
    return Thread.currentThread().isInterrupted()
        || !acceptingConnections.get()
        || session.invalidated.get();
  }

  private Resource.StateListener createStateListener(AtomicBoolean invalidated) {
    return context -> {
      if (INVALIDATING_STATES.contains(context.currentState())
          && invalidated.compareAndSet(false, true)) {
        logger.info("AMQP resource entered {}; invalidated active session", context.currentState());
      }
    };
  }

  private void closeActiveSession() {
    var session = activeSession.getAndSet(null);
    if (session != null) {
      session.invalidated.set(true);
      closeSession(session);
    }
  }

  private void closeSession(BrokerSession session) {
    closeResourceQuietly("publisher", session.publisher);
    closeResourceQuietly("connection", session.connection);
    closeResourceQuietly("environment", session.environment);
  }

  private void closeResourceQuietly(String name, @Nullable AutoCloseable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (Exception ex) {
      logger.warn("Failed to close AMQP {} cleanly: {}", name, ex.getMessage(), ex);
    }
  }

  @Override
  public Environment environment() {
    acquireConnectionLockOrThrow("environment");
    try {
      var session = ensureOrReconnectUnderLock();
      if (session == null) {
        throw createUninitializedException("environment");
      }
      return session.environment;
    } finally {
      connectionLock.unlock();
    }
  }

  @Override
  public Connection connection() {
    acquireConnectionLockOrThrow("connection");
    try {
      var session = ensureOrReconnectUnderLock();
      if (session == null) {
        throw createUninitializedException("connection");
      }
      return session.connection;
    } finally {
      connectionLock.unlock();
    }
  }

  @Override
  public @Nullable Publisher publisher() {
    if (!isReadyForConnections()) {
      return null;
    }
    var healthy = currentHealthySessionSnapshot();
    if (healthy != null) {
      return healthy.publisher;
    }
    try {
      connectionLock.lockInterruptibly();
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      return null;
    }
    try {
      var session = ensureOrReconnectUnderLock();
      return session != null ? session.publisher : null;
    } finally {
      connectionLock.unlock();
    }
  }

  @Override
  public void invalidatePublisher(Publisher failedPublisher) {
    var session = activeSession.get();
    if (session != null
        && Objects.equals(session.publisher, failedPublisher)
        && session.invalidated.compareAndSet(false, true)) {
      logger.info("Invalidated failed AMQP publisher; next publish will reconnect");
    }
  }

  @Override
  public boolean isConnected() {
    if (!isReadyForConnections()) {
      return false;
    }
    if (currentHealthySessionSnapshot() != null) {
      return true;
    }
    try {
      connectionLock.lockInterruptibly();
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      return false;
    }
    try {
      return ensureOrReconnectUnderLock() != null;
    } finally {
      connectionLock.unlock();
    }
  }

  @Override
  public Optional<Connection.ConnectionInfo> probeConnection() throws InterruptedException {
    return probeConnection(_ -> {}, () -> false);
  }

  @Override
  public Optional<Connection.ConnectionInfo> probeConnection(
      Consumer<@Nullable Publisher> probedPublisherObserver, BooleanSupplier callerCancelled)
      throws InterruptedException {
    if (!isReadyForConnections()) {
      return Optional.empty();
    }
    var healthy = currentHealthySessionSnapshot();
    if (healthy != null) {
      var probed = probeSessionOutsideLock(healthy, probedPublisherObserver, callerCancelled);
      if (probed.isPresent() || !isReadyForConnections() || callerCancelled.getAsBoolean()) {
        return probed;
      }
    }
    return reconnectAndReadInfo();
  }

  private Optional<Connection.ConnectionInfo> probeSessionOutsideLock(
      BrokerSession session,
      Consumer<@Nullable Publisher> probedPublisherObserver,
      BooleanSupplier callerCancelled)
      throws InterruptedException {
    probedPublisherObserver.accept(session.publisher);
    try {
      declareInboxQueue(session.connection);
      if (callerCancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
        throw createInterruptedException("AMQP health probe interrupted");
      }
      if (!isReadyForConnections() || session.invalidated.get()) {
        return Optional.empty();
      }
      return Optional.of(session.connection.connectionInfo());
    } catch (RuntimeException ex) {
      if (isInterruptedFailure(ex, callerCancelled)) {
        throw createInterruptedException("AMQP health probe interrupted: " + ex.getMessage());
      }
      logger.info("AMQP health probe failed; reconnecting: {}", ex.getMessage());
      invalidatePublisher(session.publisher);
      return Optional.empty();
    } finally {
      probedPublisherObserver.accept(null);
    }
  }

  private static boolean isInterruptedFailure(
      Throwable throwable, BooleanSupplier callerCancelled) {
    if (callerCancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
      return true;
    }
    Throwable cause = throwable;
    for (int depth = 0; cause != null && depth < 16; depth++) {
      if (cause instanceof InterruptedException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  private Optional<Connection.ConnectionInfo> reconnectAndReadInfo() throws InterruptedException {
    connectionLock.lockInterruptibly();
    try {
      var reconnected = ensureOrReconnectUnderLock();
      if (reconnected == null) {
        return Optional.empty();
      }
      return Optional.of(reconnected.connection.connectionInfo());
    } finally {
      connectionLock.unlock();
    }
  }

  private static AtomicBoolean createInvalidatedFlag() {
    return new AtomicBoolean(false);
  }

  private static BrokerSession createBrokerSession(
      Environment environment,
      Connection connection,
      Publisher publisher,
      AtomicBoolean invalidated) {
    return new BrokerSession(environment, connection, publisher, invalidated);
  }

  private static IllegalStateException createUninitializedException(String resourceName) {
    return new IllegalStateException(
        "AMQP " + resourceName + " is not initialized or queue is disabled");
  }

  private static IllegalStateException createLockInterruptedException(
      String operation, InterruptedException cause) {
    return new IllegalStateException(
        "Interrupted while acquiring AMQP " + operation + " lock", cause);
  }

  private static InterruptedException createInterruptedException(String message) {
    return new InterruptedException(message);
  }
}
