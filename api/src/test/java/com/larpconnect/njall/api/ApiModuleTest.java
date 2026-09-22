package com.larpconnect.njall.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.admin.AdminRoute;
import com.larpconnect.njall.api.http.RootRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ApiModuleTest {

  private static ActorSystem<Void> system;

  @BeforeAll
  static void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "api-module-test");
  }

  @AfterAll
  static void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("configure installs HttpModule and AdminModule binding RootRoute and AdminRoute")
  void configure_createsInjector_bindsRoutes() {
    var testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(new TypeLiteral<ActorSystem<Void>>() {}).toInstance(system);
            bind(HealthCheckRegistry.class).toInstance(new HealthCheckRegistry());
            bind(com.larpconnect.njall.data.dao.ServerDAO.class)
                .toInstance(
                    org.mockito.Mockito.mock(com.larpconnect.njall.data.dao.ServerDAO.class));
            bind(com.larpconnect.njall.data.dao.StudioDAO.class)
                .toInstance(
                    org.mockito.Mockito.mock(com.larpconnect.njall.data.dao.StudioDAO.class));
            bind(com.larpconnect.njall.data.dao.AdminRoleDAO.class)
                .toInstance(
                    org.mockito.Mockito.mock(com.larpconnect.njall.data.dao.AdminRoleDAO.class));
            bind(com.larpconnect.njall.data.dao.AdminUserDAO.class)
                .toInstance(
                    org.mockito.Mockito.mock(com.larpconnect.njall.data.dao.AdminUserDAO.class));
            bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
          }
        };

    var injector = Guice.createInjector(new ApiModule(), testModule);
    var rootRoute = injector.getInstance(RootRoute.class);
    var adminRoute = injector.getInstance(AdminRoute.class);

    assertThat(rootRoute).isNotNull();
    assertThat(adminRoute).isNotNull();
  }
}
