package com.larpconnect.njall.queue.publisher;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/** Guice module binding AMQP 1.0 queue lifecycle service and inbox activity publisher. */
public final class QueuePublisherModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AmqpEnvironmentFactory.class).to(DefaultAmqpEnvironmentFactory.class).in(Scopes.SINGLETON);
    bind(AmqpQueueService.class).to(DefaultAmqpQueueService.class).in(Scopes.SINGLETON);
    bind(AmqpClient.class).to(AmqpQueueService.class);
    bind(InboxActivityPublisher.class).to(DefaultInboxActivityPublisher.class).in(Scopes.SINGLETON);
  }
}
