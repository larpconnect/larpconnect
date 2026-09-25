package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.common.telemetry.ApiCall;
import com.larpconnect.njall.data.dao.ServerDAO;
import io.dropwizard.metrics5.health.HealthCheck;
import io.dropwizard.metrics5.health.HealthCheckRegistry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
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

  private static com.google.inject.Injector createTestInjector() {
    var serverDao = mock(ServerDAO.class);
    var studioDao = mock(com.larpconnect.njall.data.dao.StudioDAO.class);
    var roleDao = mock(com.larpconnect.njall.data.dao.AdminRoleDAO.class);
    var userDao = mock(com.larpconnect.njall.data.dao.AdminUserDAO.class);
    var testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(new TypeLiteral<ActorSystem<Void>>() {}).toInstance(system);
            bind(HealthCheckRegistry.class).toInstance(new HealthCheckRegistry());
            bind(ServerDAO.class).toInstance(serverDao);
            bind(com.larpconnect.njall.data.dao.StudioDAO.class).toInstance(studioDao);
            bind(com.larpconnect.njall.data.dao.AdminRoleDAO.class).toInstance(roleDao);
            bind(com.larpconnect.njall.data.dao.AdminUserDAO.class).toInstance(userDao);
            bind(Props.class).annotatedWith(Blocking.class).toInstance(Props.empty());
          }
        };
    return Guice.createInjector(new AdminModule(), testModule);
  }

  @Test
  @DisplayName(
      "AdminModule binds AdminRoute, RouteProvider, actors, and PekkoHealthCheck into multibinder")
  void configure_bindsAdminComponents() {
    var injector = createTestInjector();

    var adminRoute = injector.getInstance(AdminRoute.class);
    var healthActorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<ApiCall<HealthCheckCommand>>>() {}));
    var serverActorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<ServerAdminCommand>>() {}));
    var studioActorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<StudioAdminCommand>>() {}));
    var roleActorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<RoleAdminCommand>>() {}));
    var userActorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<UserAdminCommand>>() {}));
    var healthChecks = injector.getInstance(Key.get(new TypeLiteral<Set<HealthCheck>>() {}));
    var healthActorFactory = injector.getInstance(HealthCheckActorFactory.class);
    var serverActorFactory = injector.getInstance(ServerAdminActorFactory.class);
    var studioActorFactory = injector.getInstance(StudioAdminActorFactory.class);
    var roleActorFactory = injector.getInstance(RoleAdminActorFactory.class);
    var userActorFactory = injector.getInstance(UserAdminActorFactory.class);
    var routeProviders = injector.getInstance(Key.get(new TypeLiteral<Set<RouteProvider>>() {}));
    var objectMapper = injector.getInstance(ObjectMapper.class);

    assertThat(adminRoute).isInstanceOf(DefaultAdminRoute.class);
    assertThat(healthActorRef).isNotNull();
    assertThat(serverActorRef).isNotNull();
    assertThat(studioActorRef).isNotNull();
    assertThat(roleActorRef).isNotNull();
    assertThat(userActorRef).isNotNull();
    assertThat(healthChecks).hasAtLeastOneElementOfType(PekkoHealthCheck.class);
    assertThat(healthActorFactory).isInstanceOf(DefaultHealthCheckActorFactory.class);
    assertThat(serverActorFactory).isInstanceOf(DefaultServerAdminActorFactory.class);
    assertThat(studioActorFactory).isInstanceOf(DefaultStudioAdminActorFactory.class);
    assertThat(roleActorFactory).isInstanceOf(DefaultRoleAdminActorFactory.class);
    assertThat(userActorFactory).isInstanceOf(DefaultUserAdminActorFactory.class);
    assertThat(routeProviders).hasAtLeastOneElementOfType(DefaultAdminRoute.class);
    assertThat(objectMapper).isNotNull();
  }
}
