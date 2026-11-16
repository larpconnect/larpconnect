package com.larpconnect.njall.queue.health;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.dropwizard.metrics5.health.HealthCheck;

/** Guice module binding {@link AmqpHealthCheck} into the health check multibinder set. */
public final class QueueHealthModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AmqpHealthCheck.class).to(DefaultAmqpHealthCheck.class).in(Scopes.SINGLETON);
    Multibinder.newSetBinder(binder(), HealthCheck.class).addBinding().to(AmqpHealthCheck.class);
  }
}
