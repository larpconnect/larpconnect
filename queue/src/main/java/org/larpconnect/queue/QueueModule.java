package org.larpconnect.queue;

import com.google.inject.AbstractModule;

/** Exposes bindings for queue/AMQP messaging integrations. */
public final class QueueModule extends AbstractModule {
  @Override
  protected void configure() {
    // Bindings for RabbitMQ connection factories, consumers, and producers
  }
}
