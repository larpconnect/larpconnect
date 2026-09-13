package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import com.larpconnect.njall.common.config.ServerConfig;
import com.larpconnect.njall.server.http.HttpServerService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.typed.ActorSystem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ServerHealthEndpointTest {

  @Test
  @DisplayName("Server starts with composite routing serving both / and /api/admin/v1/health")
  void server_startsAndServesCompositeRoutes() throws Exception {
    var overrideConfig =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(ServerConfig.class).toInstance(ServerConfig.of("127.0.0.1", 0));
          }
        };

    var injector = Guice.createInjector(Modules.override(new ServerModule()).with(overrideConfig));
    var server = injector.getInstance(HttpServerService.class);
    var system = injector.getInstance(Key.get(new TypeLiteral<ActorSystem<Void>>() {}));

    try {
      server.start().toCompletableFuture().get(10, TimeUnit.SECONDS);
      int port = server.getBoundPort();
      assertThat(port).isPositive();

      var client = HttpClient.newHttpClient();

      // Verify root endpoint
      var rootRequest =
          HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/")).GET().build();
      var rootResponse = client.send(rootRequest, HttpResponse.BodyHandlers.ofString());
      assertThat(rootResponse.statusCode()).isEqualTo(200);
      assertThat(rootResponse.body()).isEmpty();

      // Verify admin health endpoint
      var healthRequest =
          HttpRequest.newBuilder()
              .uri(URI.create("http://127.0.0.1:" + port + "/api/admin/v1/health"))
              .GET()
              .build();
      var healthResponse = client.send(healthRequest, HttpResponse.BodyHandlers.ofString());
      assertThat(healthResponse.statusCode()).isEqualTo(200);
      assertThat(healthResponse.body()).isEmpty();
    } finally {
      server.stop().toCompletableFuture().get(5, TimeUnit.SECONDS);
      system.terminate();
      system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }
  }
}
