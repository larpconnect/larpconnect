package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class WebFingerVerticleTest {

  private static final int PORT = 1234;

  @BeforeEach
  void setUp(Vertx vertx, VertxTestContext testContext) {
    vertx
        .deployVerticle(new WebFingerVerticle(PORT))
        .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void handleWebFingerRequest_success(Vertx vertx, VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("resource", "acct:user@example.com");

    vertx
        .eventBus()
        .request(WebFingerVerticle.ADDRESS, request)
        .onComplete(
            testContext.succeeding(
                reply -> {
                  JsonObject body = (JsonObject) reply.body();
                  testContext.verify(
                      () -> {
                        assertThat(body.getString("resource")).isEqualTo("acct:user@example.com");
                        assertThat(body.getInteger("port")).isEqualTo(PORT);
                        assertThat(body.getJsonArray("links")).isEmpty();
                        testContext.completeNow();
                      });
                }));
  }
}
