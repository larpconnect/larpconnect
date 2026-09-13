package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.larpconnect.njall.server.http.HttpServerService;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerModuleTest {

  @Test
  @DisplayName("configure installs submodules and provides ActorSystem and HttpServerService")
  void configure_createsInjector_providesRequiredBindings() throws Exception {
    var injector = Guice.createInjector(new ServerModule());
    var serverService = injector.getInstance(HttpServerService.class);
    var system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));

    assertThat(serverService).isNotNull();
    assertThat(system).isNotNull();

    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }
}
