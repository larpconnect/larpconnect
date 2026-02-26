package com.larpconnect.njall.server;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class WebServerVerticleTest {

  @Test
  void start_server(Vertx vertx, VertxTestContext testContext) {
    vertx
        .deployVerticle(new WebServerVerticle())
        .onSuccess(id -> testContext.completeNow())
        .onFailure(testContext::failNow);
  }
}
