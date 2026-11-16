package com.larpconnect.njall.queue.publisher;

import com.rabbitmq.client.amqp.Environment;

/** Factory interface producing AMQP 1.0 {@link Environment} instances. */
@FunctionalInterface
interface AmqpEnvironmentFactory {

  /**
   * Creates a new AMQP 1.0 client environment.
   *
   * @return A newly initialized {@link Environment}.
   */
  Environment createEnvironment();
}
