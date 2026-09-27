package com.larpconnect.njall.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.common.telemetry.ApiCall;
import com.larpconnect.njall.data.cache.StudioLookupCache;
import com.larpconnect.njall.data.dao.AdminRoleDAO;
import com.larpconnect.njall.data.dao.AdminUserDAO;
import com.larpconnect.njall.data.dao.DefaultStudioRoleDAO;
import com.larpconnect.njall.data.dao.ServerDAO;
import com.larpconnect.njall.data.dao.StudioLookupDAO;
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

  private static Injector createTestInjector() {
    var serverDao = mock(ServerDAO.class);
    var studioLookupDao = mock(StudioLookupDAO.class);
    var roleDao = mock(AdminRoleDAO.class);
    var userDao = mock(AdminUserDAO.class);
    var studioRoleDao = mock(DefaultStudioRoleDAO.class);
    var studioLookupCache = mock(StudioLookupCache.class);
    var testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(new TypeLiteral<ActorSystem<Void>>() {}).toInstance(system);
            bind(HealthCheckRegistry.class).toInstance(new HealthCheckRegistry());
            bind(ServerDAO.class).toInstance(serverDao);
            bind(StudioLookupDAO.class).toInstance(studioLookupDao);
            bind(StudioLookupCache.class).toInstance(studioLookupCache);
            bind(AdminRoleDAO.class).toInstance(roleDao);
            bind(AdminUserDAO.class).toInstance(userDao);
            bind(DefaultStudioRoleDAO.class).toInstance(studioRoleDao);
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
    var studioRoleActorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<StudioRoleAdminCommand>>() {}));
    var roleActorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<RoleAdminCommand>>() {}));
    var userActorRef =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<UserAdminCommand>>() {}));
    var healthChecks = injector.getInstance(Key.get(new TypeLiteral<Set<HealthCheck>>() {}));
    var healthActorFactory = injector.getInstance(HealthCheckActorFactory.class);
    var serverActorFactory = injector.getInstance(ServerAdminActorFactory.class);
    var studioActorFactory = injector.getInstance(StudioAdminActorFactory.class);
    var studioRoleActorFactory = injector.getInstance(StudioRoleAdminActorFactory.class);
    var roleActorFactory = injector.getInstance(RoleAdminActorFactory.class);
    var userActorFactory = injector.getInstance(UserAdminActorFactory.class);
    var routeProviders = injector.getInstance(Key.get(new TypeLiteral<Set<RouteProvider>>() {}));
    var objectMapper = injector.getInstance(ObjectMapper.class);

    assertThat(adminRoute).isInstanceOf(DefaultAdminRoute.class);
    assertThat(healthActorRef).isInstanceOf(ActorRef.class);
    assertThat(serverActorRef).isInstanceOf(ActorRef.class);
    assertThat(studioActorRef).isInstanceOf(ActorRef.class);
    assertThat(studioRoleActorRef).isInstanceOf(ActorRef.class);
    assertThat(roleActorRef).isInstanceOf(ActorRef.class);
    assertThat(userActorRef).isInstanceOf(ActorRef.class);
    assertThat(healthChecks).hasAtLeastOneElementOfType(PekkoHealthCheck.class);
    assertThat(healthActorFactory).isInstanceOf(DefaultHealthCheckActorFactory.class);
    assertThat(serverActorFactory).isInstanceOf(DefaultServerAdminActorFactory.class);
    assertThat(studioActorFactory).isInstanceOf(DefaultStudioAdminActorFactory.class);
    assertThat(studioRoleActorFactory).isInstanceOf(DefaultStudioRoleAdminActorFactory.class);
    assertThat(roleActorFactory).isInstanceOf(DefaultRoleAdminActorFactory.class);
    assertThat(userActorFactory).isInstanceOf(DefaultUserAdminActorFactory.class);
    assertThat(routeProviders).hasAtLeastOneElementOfType(DefaultAdminRoute.class);
    assertThat(objectMapper).isInstanceOf(ObjectMapper.class);
  }
}
