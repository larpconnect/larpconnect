package com.larpconnect.njall.queue.publisher;

import com.google.common.util.concurrent.AbstractIdleService;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Publisher;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Test fake implementation of {@link AmqpQueueService} adhering to Guava @DoNotMock. */
public final class FakeAmqpQueueService extends AbstractIdleService implements AmqpQueueService {

  private @Nullable Environment environment;
  private @Nullable Connection connection;
  private @Nullable Publisher publisher;
  private boolean connected = true;

  public FakeAmqpQueueService() {}

  public FakeAmqpQueueService(
      @Nullable Environment environment,
      @Nullable Connection connection,
      @Nullable Publisher publisher) {
    this.environment = environment;
    this.connection = connection;
    this.publisher = publisher;
  }

  public void setEnvironment(@Nullable Environment environment) {
    this.environment = environment;
  }

  public void setConnection(@Nullable Connection connection) {
    this.connection = connection;
  }

  public void setPublisher(@Nullable Publisher publisher) {
    this.publisher = publisher;
  }

  public void setConnected(boolean connected) {
    this.connected = connected;
  }

  @Override
  protected void startUp() {}

  @Override
  protected void shutDown() {}

  @Override
  public Environment environment() {
    if (environment == null) {
      throw new IllegalStateException("Environment not set");
    }
    return environment;
  }

  @Override
  public Connection connection() {
    if (connection == null) {
      throw new IllegalStateException("Connection not set");
    }
    return connection;
  }

  @Override
  public @Nullable Publisher publisher() {
    return publisher;
  }

  @Override
  public void invalidatePublisher(Publisher failedPublisher) {
    if (Objects.equals(this.publisher, failedPublisher)) {
      this.publisher = null;
    }
  }

  @Override
  public boolean isConnected() {
    return isRunning() && connected;
  }

  @Override
  public Optional<Connection.ConnectionInfo> probeConnection() {
    if (!isConnected() || connection == null) {
      return Optional.empty();
    }
    return Optional.of(connection.connectionInfo());
  }
}
