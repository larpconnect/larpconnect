package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AdminModuleTest {

  private static ActorSystem<Void> system;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "admin-module-test");
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("AdminModule binds AdminRoute, actor, and PekkoHealthCheck into multibinder")
  void configure_bindsAdminComponents() {
    var testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(new TypeLiteral<ActorSystem<Void>>() {}).toInstance(system);
            bind(HealthCheckRegistry.class).toInstance(new HealthCheckRegistry());
          }
        };

    var injector = Guice.createInjector(new AdminModule(), testModule);

    var adminRoute = injector.getInstance(AdminRoute.class);
    var actorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<HealthCheckCommand>>() {}));
    var healthChecks = injector.getInstance(Key.get(new TypeLiteral<Set<HealthCheck>>() {}));
    var actorFactory = injector.getInstance(HealthCheckActorFactory.class);

    assertThat(adminRoute).isInstanceOf(DefaultAdminRoute.class);
    assertThat(actorRef).isNotNull();
    assertThat(healthChecks).hasAtLeastOneElementOfType(PekkoHealthCheck.class);
    assertThat(actorFactory).isInstanceOf(DefaultHealthCheckActorFactory.class);
  }
}
