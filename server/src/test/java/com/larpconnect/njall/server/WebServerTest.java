package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
final class WebServerTest {

  @Test
  void testGetMessage(Vertx vertx, VertxTestContext testContext) {
    vertx
        .deployVerticle(new WebServerVerticle())
        .compose(
            id -> {
              WebClient client = WebClient.create(vertx);
              return client.get(8080, "localhost", "/v1/message").send();
            })
        .onComplete(
            testContext.succeeding(
                response -> {
                  testContext.verify(
                      () -> {
                        assertThat(response.statusCode()).isEqualTo(200);
                        JsonObject body = response.bodyAsJsonObject();
                        assertThat(body.getString("messageType")).isEqualTo("Greeting");
                        testContext.completeNow();
                      });
                }));
  }
}
