package com.larpconnect.njall.queue;

import com.google.inject.AbstractModule;
import com.larpconnect.njall.queue.config.QueueConfigModule;
import com.larpconnect.njall.queue.health.QueueHealthModule;
import com.larpconnect.njall.queue.publisher.QueuePublisherModule;

/** Public Guice module for the queue library module. */
public final class QueueModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new QueueConfigModule());
    install(new QueuePublisherModule());
    install(new QueueHealthModule());
  }
}
