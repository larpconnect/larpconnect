package com.larpconnect.njall.server;

import com.google.common.util.concurrent.AbstractIdleService;
import com.larpconnect.njall.queue.publisher.AmqpQueueService;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.Publisher;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.Nullable;

final class TestAmqpQueueService extends AbstractIdleService implements AmqpQueueService {

  private final boolean failStart;
  private final boolean failStop;
  private final List<String> eventLog;

  TestAmqpQueueService() {
    this(false, false, new CopyOnWriteArrayList<>());
  }

  TestAmqpQueueService(List<String> eventLog) {
    this(false, false, eventLog);
  }

  TestAmqpQueueService(boolean failStart, boolean failStop) {
    this(failStart, failStop, new CopyOnWriteArrayList<>());
  }

  TestAmqpQueueService(boolean failStart, boolean failStop, List<String> eventLog) {
    this.failStart = failStart;
    this.failStop = failStop;
    this.eventLog = eventLog;
  }

  static TestAmqpQueueService failingOnStart() {
    return new TestAmqpQueueService(true, false);
  }

  static TestAmqpQueueService failingOnStop() {
    return new TestAmqpQueueService(false, true);
  }

  @Override
  protected void startUp() throws Exception {
    eventLog.add("amqp.startUp");
    if (failStart) {
      throw new IllegalStateException("Forced AMQP start failure");
    }
  }

  @Override
  protected void shutDown() throws Exception {
    eventLog.add("amqp.shutDown");
    if (failStop) {
      throw new IllegalStateException("Forced AMQP stop failure");
    }
  }

  @Override
  public Environment environment() {
    throw new UnsupportedOperationException("Not implemented for test");
  }

  @Override
  public Connection connection() {
    throw new UnsupportedOperationException("Not implemented for test");
  }

  @Override
  public @Nullable Publisher publisher() {
    return null;
  }

  @Override
  public void invalidatePublisher(Publisher failedPublisher) {}

  @Override
  public boolean isConnected() {
    return isRunning();
  }

  @Override
  public Optional<Connection.ConnectionInfo> probeConnection() {
    return Optional.empty();
  }

  List<String> eventLog() {
    return eventLog;
  }
}
