package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.common.annotation.Blocking;
import com.larpconnect.njall.data.session.ActiveSessionFactories;
import com.larpconnect.njall.server.http.HttpServerService;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerBindingModuleTest {

  @Test
  @DisplayName("configure provides ServerManager, ServerManagerService, ActorSystem, and Props")
  void configure_providesRequiredBindings() throws Exception {
    var mockFactories = mock(ActiveSessionFactories.class);
    var mockHttpServerService = mock(HttpServerService.class);

    var mockDependenciesModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(ActiveSessionFactories.class).toInstance(mockFactories);
            bind(HttpServerService.class).toInstance(mockHttpServerService);
          }
        };

    var injector = Guice.createInjector(new ServerBindingModule(), mockDependenciesModule);

    var serverManager = injector.getInstance(ServerManager.class);
    var serverManagerService = injector.getInstance(ServerManagerService.class);
    var hookRegistrar = injector.getInstance(ShutdownHookRegistrar.class);
    var system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));
    var blockingProps = injector.getInstance(Key.get(Props.class, Blocking.class));

    assertThat(serverManager).isNotNull();
    assertThat(serverManagerService).isNotNull();
    assertThat(serverManager).isSameAs(serverManagerService);
    assertThat(hookRegistrar).isNotNull();
    assertThat(hookRegistrar.removeShutdownHook(new Thread())).isFalse();
    assertThat(system).isNotNull();
    assertThat(blockingProps).isNotNull();

    CoordinatedShutdown.get(system)
        .runAll(CoordinatedShutdown.jvmExitReason())
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
  }
}
