package com.larpconnect.njall.queue.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;

/** Guice module providing {@link QueueConfig}. */
public final class QueueConfigModule extends AbstractModule {

  @Override
  protected void configure() {
    // QueueConfig is provided via @Provides method
  }

  @Provides
  @Singleton
  QueueConfig provideQueueConfig(Config config) {
    return new QueueConfig(config);
  }
}
