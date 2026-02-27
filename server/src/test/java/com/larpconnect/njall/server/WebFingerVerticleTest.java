package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
final class WebFingerVerticleTest {

  @Test
  void start_registersConsumerAndResponds(Vertx vertx, VertxTestContext testContext) {
    int port = 9090;
    WebFingerVerticle verticle = new WebFingerVerticle(port);

    vertx
        .deployVerticle(verticle)
        .compose(
            id ->
                vertx
                    .eventBus()
                    .<JsonObject>request(
                        "webfinger.service", new JsonObject().put("resource", "acct:user@host")))
        .onComplete(
            testContext.succeeding(
                reply -> {
                  testContext.verify(
                      () -> {
                        JsonObject body = reply.body();
                        assertThat(body.getInteger("port")).isEqualTo(port);
                        assertThat(body.getJsonArray("resources")).isEqualTo(new JsonArray());
                        testContext.completeNow();
                      });
                }));
  }

  @Test
  void start_handlesMessageWithoutResource(Vertx vertx, VertxTestContext testContext) {
    int port = 9090;
    WebFingerVerticle verticle = new WebFingerVerticle(port);

    vertx
        .deployVerticle(verticle)
        .compose(id -> vertx.eventBus().<JsonObject>request("webfinger.service", new JsonObject()))
        .onComplete(
            testContext.succeeding(
                reply -> {
                  testContext.verify(
                      () -> {
                        JsonObject body = reply.body();
                        assertThat(body.getInteger("port")).isEqualTo(port);
                        assertThat(body.getJsonArray("resources")).isEqualTo(new JsonArray());
                        testContext.completeNow();
                      });
                }));
  }
}
