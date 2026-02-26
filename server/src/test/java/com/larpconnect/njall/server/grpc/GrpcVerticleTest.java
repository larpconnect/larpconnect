package com.larpconnect.njall.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
final class GrpcVerticleTest {

  @Test
  void start_validConfig_startsServer(Vertx vertx, VertxTestContext testContext) {
    var service = new GrpcMessageService();
    var verticle = new GrpcVerticle(service);

    int port;
    try (var serverSocket = new ServerSocket(0)) {
      port = serverSocket.getLocalPort();
    } catch (IOException e) {
      testContext.failNow(e);
      return;
    }

    var options =
        new io.vertx.core.DeploymentOptions()
            .setConfig(new JsonObject().put("grpc.port", port));

    vertx
        .deployVerticle(verticle, options)
        .onComplete(testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void start_portInUse_failsStartup(Vertx vertx, VertxTestContext testContext) throws IOException {
    try (var serverSocket = new ServerSocket(0)) {
      int port = serverSocket.getLocalPort();
      var service = new GrpcMessageService();
      var verticle = new GrpcVerticle(service);

      var options =
          new io.vertx.core.DeploymentOptions()
              .setConfig(new JsonObject().put("grpc.port", port));

      vertx
          .deployVerticle(verticle, options)
          .onComplete(
              testContext.failing(
                  err -> {
                    assertThat(err).isNotNull();
                    // We expect an IOException or similar due to port conflict
                    testContext.completeNow();
                  }));
    }
  }
}
