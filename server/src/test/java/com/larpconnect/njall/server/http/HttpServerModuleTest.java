package com.larpconnect.njall.server.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.larpconnect.njall.api.http.RootRoute;
import com.larpconnect.njall.common.config.ServerConfig;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.Directives;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HttpServerModuleTest {

  private ActorSystem<Void> system;

  @BeforeEach
  void setUp() {
    system = ActorSystem.create(Behaviors.empty(), "test-server-module");
  }

  @AfterEach
  void tearDown() throws Exception {
    system.terminate();
    system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
  }

  @Test
  @DisplayName("configure binds HttpServerService to DefaultHttpServerService")
  void configure_createsInjector_bindsHttpServerService() {
    var injector =
        Guice.createInjector(
            new HttpServerModule(),
            new AbstractModule() {
              @Override
              protected void configure() {
                bind(new Key<ActorSystem<Void>>() {}).toInstance(system);
                bind(ServerConfig.class).toInstance(new ServerConfig("127.0.0.1", 0));
                bind(RootRoute.class).toInstance(() -> Directives.complete(StatusCodes.OK));
              }
            });

    var service1 = injector.getInstance(HttpServerService.class);
    var service2 = injector.getInstance(HttpServerService.class);

    assertThat(service1).isInstanceOf(DefaultHttpServerService.class).isSameAs(service2);
  }
}
