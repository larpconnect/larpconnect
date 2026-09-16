package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import com.larpconnect.njall.api.admin.AdminRoute;
import com.larpconnect.njall.api.admin.ServerAdminCommand;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.session.SessionFactoryFactory;
import com.larpconnect.njall.server.http.HttpServerService;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerModuleTest {

  @Test
  @DisplayName("configure installs submodules and provides ActorSystem, ServerService, and Health")
  void configure_createsInjector_providesRequiredBindings() throws Exception {
    var mockSessionFactory = mock(SessionFactory.class);
    when(mockSessionFactory.isClosed()).thenReturn(false);
    var mockFactory = mock(SessionFactoryFactory.class);
    when(mockFactory.create(any(), any())).thenReturn(mockSessionFactory);

    var testOverride =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(SessionFactoryFactory.class).toInstance(mockFactory);
          }
        };

    var injector = Guice.createInjector(Modules.override(new ServerModule()).with(testOverride));
    var serverService = injector.getInstance(HttpServerService.class);
    var system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));
    var registry = injector.getInstance(HealthCheckRegistry.class);
    var adminRoute = injector.getInstance(AdminRoute.class);
    var blockingProps = injector.getInstance(Key.get(Props.class, Blocking.class));
    var serverAdminActor =
        injector.getInstance(Key.get(new TypeLiteral<ActorRef<ServerAdminCommand>>() {}));
    var serverManager = injector.getInstance(ServerManager.class);
    var serverManagerService = injector.getInstance(ServerManagerService.class);
    var hookRegistrar = injector.getInstance(ShutdownHookRegistrar.class);

    assertThat(serverService).isNotNull();
    assertThat(system).isNotNull();
    assertThat(registry).isNotNull();
    assertThat(adminRoute).isNotNull();
    assertThat(blockingProps).isNotNull();
    assertThat(serverAdminActor).isNotNull();
    assertThat(serverManager).isNotNull();
    assertThat(serverManagerService).isNotNull();
    assertThat(serverManager).isSameAs(serverManagerService);
    assertThat(hookRegistrar).isNotNull();

    CoordinatedShutdown.get(system)
        .runAll(CoordinatedShutdown.jvmExitReason())
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
    verify(mockSessionFactory).close();
  }
}
