package com.larpconnect.njall.queue.publisher;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Publisher;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Operational client contract for interacting with AMQP 1.0 broker resources decoupled from Guava
 * {@link com.google.common.util.concurrent.Service} lifecycle management.
 */
public interface AmqpClient {

  /**
   * Returns the underlying AMQP 1.0 client environment.
   *
   * @throws IllegalStateException if the client is not started or queue is disabled
   */
  Environment environment();

  /**
   * Returns the active AMQP 1.0 broker connection.
   *
   * @throws IllegalStateException if the client is not started or queue is disabled
   */
  Connection connection();

  /**
   * Returns the active message publisher on a lock-free fast path when healthy, reconnecting on
   * demand if invalidated, or {@code null} if the service is disabled or stopped.
   */
  @Nullable Publisher publisher();

  /**
   * Invalidates {@code failedPublisher} if it is still the active fast-path publisher so subsequent
   * publish attempts establish a fresh session.
   */
  void invalidatePublisher(Publisher failedPublisher);

  /** Returns {@code true} if the underlying queue service is currently in the running state. */
  boolean isRunning();

  /**
   * Returns {@code true} if the service is running, queue is enabled, and the connection is active.
   */
  boolean isConnected();

  /**
   * Performs an interruptible broker liveness and queue topology check for health reporting,
   * reconnecting if needed.
   *
   * @return Active broker {@link Connection.ConnectionInfo}, or empty if disabled or unavailable.
   * @throws InterruptedException if interrupted while probing or awaiting the connection lock.
   */
  Optional<Connection.ConnectionInfo> probeConnection() throws InterruptedException;

  /**
   * Performs an interruptible broker liveness and queue topology check for health reporting,
   * notifying {@code probedPublisherObserver} when an existing publisher's session enters an
   * out-of-lock broker probe and checking {@code callerCancelled} so external caller cancellation
   * does not invalidate a healthy session.
   *
   * @param probedPublisherObserver Receives the publisher currently being probed before the
   *     blocking broker RPC starts, and {@code null} once that out-of-lock probe finishes.
   * @param callerCancelled Supplies {@code true} when the probe was cancelled due to external
   *     caller interruption rather than a broker failure or probe timeout.
   * @return Active broker {@link Connection.ConnectionInfo}, or empty if disabled or unavailable.
   * @throws InterruptedException if interrupted while probing or awaiting the connection lock.
   */
  default Optional<Connection.ConnectionInfo> probeConnection(
      Consumer<@Nullable Publisher> probedPublisherObserver, BooleanSupplier callerCancelled)
      throws InterruptedException {
    return probeConnection();
  }
}
