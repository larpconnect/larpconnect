package com.larpconnect.njall.data.health;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import io.dropwizard.metrics5.health.HealthCheck;

/** Guice module binding health checks for data persistence components. */
public final class DataHealthModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AdminDatabaseHealthCheck.class).in(Scopes.SINGLETON);
    Multibinder.newSetBinder(binder(), HealthCheck.class)
        .addBinding()
        .to(AdminDatabaseHealthCheck.class);
  }
}
