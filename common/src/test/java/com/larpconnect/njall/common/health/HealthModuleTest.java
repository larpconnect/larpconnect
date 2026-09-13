package com.larpconnect.njall.common.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HealthModuleTest {

  static final class SampleHealthCheck extends HealthCheck {
    @Override
    protected Result check() {
      return Result.healthy();
    }
  }

  @Test
  @DisplayName("HealthModule provides an empty registry when no checks are registered")
  void provideHealthCheckRegistry_noChecks_returnsEmptyRegistry() {
    var injector = Guice.createInjector(new HealthModule());
    var registry = injector.getInstance(HealthCheckRegistry.class);

    assertThat(registry).isNotNull();
    assertThat(registry.runHealthChecks()).isEmpty();
  }

  @Test
  @DisplayName("HealthModule registers multibound health checks by full class name")
  void provideHealthCheckRegistry_withNamedCheck_registersCorrectly() {
    var injector =
        Guice.createInjector(
            new HealthModule(),
            new AbstractModule() {
              @Override
              protected void configure() {
                Multibinder.newSetBinder(binder(), HealthCheck.class)
                    .addBinding()
                    .to(SampleHealthCheck.class);
              }
            });

    var registry = injector.getInstance(HealthCheckRegistry.class);
    var results = registry.runHealthChecks();

    var expectedName = SampleHealthCheck.class.getName();
    assertThat(results).containsKey(expectedName);
    assertThat(results.get(expectedName).isHealthy()).isTrue();
  }

  @Test
  @DisplayName("HealthModule registers anonymous health check using full class name")
  void provideHealthCheckRegistry_withAnonymousCheck_registersWithFullName() {
    var anonymousCheck =
        new HealthCheck() {
          @Override
          protected Result check() {
            return Result.unhealthy("down");
          }
        };

    var injector =
        Guice.createInjector(
            new HealthModule(),
            new AbstractModule() {
              @Override
              protected void configure() {
                Multibinder.newSetBinder(binder(), HealthCheck.class)
                    .addBinding()
                    .toInstance(anonymousCheck);
              }
            });

    var registry = injector.getInstance(HealthCheckRegistry.class);
    var results = registry.runHealthChecks();

    assertThat(results).containsKey(anonymousCheck.getClass().getName());
    assertThat(results.get(anonymousCheck.getClass().getName()).isHealthy()).isFalse();
  }

  @Test
  @DisplayName("provideHealthCheckRegistry throws NullPointerException when healthChecks is null")
  void provideHealthCheckRegistry_nullSet_throwsNullPointerException() {
    var module = new HealthModule();
    assertThatNullPointerException()
        .isThrownBy(() -> module.provideHealthCheckRegistry(null))
        .withMessage("healthChecks must not be null");
  }
}
