package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
final class WebServerTest {

  @Test
  void defaultConstructor_usesDefaultPort() {
    assertThat(new WebServerVerticle().actualPort()).isEqualTo(8080);
  }

  @Test
  void start_invokesPortListener(Vertx vertx, VertxTestContext testContext) {
    AtomicInteger capturedPort = new AtomicInteger();
    var verticle =
        new WebServerVerticle(0, "openapi.yaml", Optional.of(port -> capturedPort.set(port)));

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  testContext.verify(
                      () -> {
                        assertThat(capturedPort.get()).isGreaterThan(0);
                        assertThat(capturedPort.get()).isEqualTo(verticle.actualPort());
                        testContext.completeNow();
                      });
                }));
  }

  @Test
  void getMessage_returnsGreeting(Vertx vertx, VertxTestContext testContext) {
    var verticle = new WebServerVerticle(0, "openapi.yaml");
    vertx
        .deployVerticle(verticle)
        .compose(
            id -> {
              WebClient client = WebClient.create(vertx);
              return client.get(verticle.actualPort(), "localhost", "/v1/message").send();
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
            },
            Optional.empty());

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
            },
            Optional.empty());

    verticle.handleGetMessage(ctx);

    verify(ctx).fail(any(RuntimeException.class));
    testContext.completeNow();
  }

  @Test
  void webFinger_returnsPortAndResources(Vertx vertx, VertxTestContext testContext) {
    AtomicInteger capturedPort = new AtomicInteger();
    var serverVerticle =
        new WebServerVerticle(0, "openapi.yaml", Optional.of(port -> capturedPort.set(port)));
    // Note: WebServerVerticle receives port 0, but WebFingerVerticle will receive the bound port
    // However, WebFingerVerticle is deployed separately. For this test to work end-to-end
    // efficiently, we need to know the port.
    // In this test, we can pass the captured port to the WebFingerVerticle, but we must
    // deploy WebServerVerticle first.
    // Actually, WebFingerVerticle just needs *a* port to report, it doesn't need to bind.
    // So we can pass 0 or any number for the test's sake, as long as we verify it's returned.

    // BUT: The WebServerVerticle will fail to start if the OpenAPI spec is valid (which it is),
    // and it will bind.
    // Then we deploy WebFingerVerticle.

    // Correct approach:
    // 1. Deploy WebServerVerticle (binds to ephemeral port)
    // 2. Capture that port.
    // 3. Deploy WebFingerVerticle with that captured port (or any port, really, but consistency is
    // nice)
    // 4. Make request.

    vertx
        .deployVerticle(serverVerticle)
        .compose(id -> vertx.deployVerticle(new WebFingerVerticle(capturedPort.get())))
        .compose(
            id -> {
              WebClient client = WebClient.create(vertx);
              return client
                  .get(capturedPort.get(), "localhost", "/.well-known/webfinger")
                  .addQueryParam("resource", "acct:user@host")
                  .send();
            })
        .onComplete(
            testContext.succeeding(
                response -> {
                  testContext.verify(
                      () -> {
                        assertThat(response.statusCode()).isEqualTo(200);
                        assertThat(response.getHeader("content-type"))
                            .isEqualTo("application/jrd+json");
                        JsonObject body = response.bodyAsJsonObject();
                        assertThat(body.getInteger("port")).isEqualTo(capturedPort.get());
                        assertThat(body.getJsonArray("resources")).isEqualTo(new JsonArray());
                        testContext.completeNow();
                      });
                }));
  }
}
