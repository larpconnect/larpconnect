package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
final class WebServerTest {

  @Test
  void getMessage_returnsGreeting(Vertx vertx, VertxTestContext testContext) {
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

  @Test
  void start_fails_whenSpecMissing(Vertx vertx, VertxTestContext testContext) {
    vertx
        .deployVerticle(new WebServerVerticle("missing.yaml"))
        .onComplete(
            testContext.failing(
                err -> {
                  testContext.verify(
                      () -> {
                        assertThat(err.getMessage()).contains("missing.yaml not found");
                        testContext.completeNow();
                      });
                }));
  }

  @Test
  void start_fails_whenSpecInvalid(Vertx vertx, VertxTestContext testContext) {
    vertx
        .deployVerticle(new WebServerVerticle("invalid.yaml"))
        .onComplete(testContext.failing(err -> testContext.completeNow()));
  }

  @Test
  void start_fails_whenPortInUse(Vertx vertx, VertxTestContext testContext) {
    vertx
        .createHttpServer()
        .requestHandler(r -> {})
        .listen(0)
        .onComplete(
            testContext.succeeding(
                server -> {
                  int port = server.actualPort();
                  vertx
                      .deployVerticle(new WebServerVerticle(port, "openapi.yaml"))
                      .onComplete(testContext.failing(err -> testContext.completeNow()));
                }));
  }

  @Test
  void handleGetMessage_handlesSerializationFailure(Vertx vertx, VertxTestContext testContext) {
    RoutingContext ctx = mock(RoutingContext.class);

    WebServerVerticle verticle =
        new WebServerVerticle(
            8080,
            "openapi.yaml",
            m -> {
              throw new IOException("Serialization failed");
            });

    verticle.handleGetMessage(ctx);

    verify(ctx).fail(any(IOException.class));
    testContext.completeNow();
  }

  @Test
  void handleGetMessage_handlesRuntimeException(Vertx vertx, VertxTestContext testContext) {
    RoutingContext ctx = mock(RoutingContext.class);

    WebServerVerticle verticle =
        new WebServerVerticle(
            8080,
            "openapi.yaml",
            m -> {
              throw new RuntimeException("Unexpected error");
            });

    verticle.handleGetMessage(ctx);

    verify(ctx).fail(any(RuntimeException.class));
    testContext.completeNow();
  }
}
