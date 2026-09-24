package com.larpconnect.njall.common.health;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.dropwizard.metrics5.health.HealthCheck;
import io.dropwizard.metrics5.health.HealthCheckRegistry;
import java.util.Set;

/**
 * Guice module configuring the Dropwizard {@link HealthCheckRegistry} and health check multibinder.
 */
public final class HealthModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), HealthCheck.class);
  }

  @Provides
  @Singleton
  HealthCheckRegistry provideHealthCheckRegistry(Set<HealthCheck> healthChecks) {
    var registry = newRegistry();
    registerAll(registry, healthChecks);
    return registry;
  }

  private HealthCheckRegistry newRegistry() {
    return new HealthCheckRegistry();
  }

  private void registerAll(HealthCheckRegistry registry, Set<HealthCheck> healthChecks) {
    for (var healthCheck : healthChecks) {
      var name = resolveName(healthCheck);
      registry.register(name, healthCheck);
    }
  }

  private static String resolveName(HealthCheck healthCheck) {
    return healthCheck.getClass().getName();
  }
}
