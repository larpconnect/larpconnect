package com.larpconnect.njall.queue.publisher;

import com.google.inject.Inject;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import java.time.Duration;

/** Default factory constructing {@link Environment} via {@link AmqpEnvironmentBuilder}. */
final class DefaultAmqpEnvironmentFactory implements AmqpEnvironmentFactory {

  static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(5);

  @Inject
  DefaultAmqpEnvironmentFactory() {}

  @Override
  public Environment createEnvironment() {
    return buildEnvironment(createEnvironmentBuilder());
  }

  private static Environment buildEnvironment(AmqpEnvironmentBuilder builder) {
    builder.connectionSettings().connectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
    return builder.build();
  }

  private static AmqpEnvironmentBuilder createEnvironmentBuilder() {
    return new AmqpEnvironmentBuilder();
  }
}
