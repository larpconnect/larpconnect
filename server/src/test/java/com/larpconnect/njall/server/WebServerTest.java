package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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
  void start_worksWithoutPortListener(Vertx vertx, VertxTestContext testContext) {
    var verticle = new WebServerVerticle(0, "openapi.yaml", Optional.empty());

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  testContext.verify(
                      () -> {
                        assertThat(verticle.actualPort()).isGreaterThan(0);
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
  void start_fails_whenContractLoaderFails(Vertx vertx, VertxTestContext testContext) {
    WebServerVerticle.ContractLoader failingLoader =
        (vx, p) -> Future.failedFuture("Contract loading failed");

    WebServerVerticle verticle =
        new WebServerVerticle(
            0, "openapi.yaml", m -> "{}", Optional.<Consumer<Integer>>empty(), failingLoader);

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.failing(
                err -> {
                  testContext.verify(
                      () -> {
                        assertThat(err.getMessage()).isEqualTo("Contract loading failed");
                        testContext.completeNow();
                      });
                }));
  }

  @Test
  void start_fails_whenFileSystemFails(Vertx vertx, VertxTestContext testContext)
      throws IOException {
    WebServerVerticle.FileSystemHelper fsHelper = mock(WebServerVerticle.FileSystemHelper.class);
    when(fsHelper.createTempFile(anyString(), anyString())).thenThrow(new IOException("Disk full"));

    WebServerVerticle verticle =
        new WebServerVerticle(
            0,
            "openapi.yaml",
            m -> "{}",
            Optional.<Consumer<Integer>>empty(),
            (vx, p) -> Future.succeededFuture(),
            fsHelper);

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.failing(
                err -> {
                  testContext.verify(
                      () -> {
                        assertThat(err).isInstanceOf(IOException.class);
                        assertThat(err.getMessage()).isEqualTo("Disk full");
                        testContext.completeNow();
                      });
                }));
  }

  @Test
  void handleGetMessage_handlesSerializationFailure(Vertx vertx, VertxTestContext testContext) {
    RoutingContext ctx = mock(RoutingContext.class);

    // Explicitly type the Optional to avoid ambiguity
    Optional<Consumer<Integer>> noListener = Optional.empty();
    WebServerVerticle.ContractLoader noLoader = (vx, p) -> Future.succeededFuture();

    WebServerVerticle verticle =
        new WebServerVerticle(
            8080,
            "openapi.yaml",
            m -> {
              throw new IOException("Serialization failed");
            },
            noListener,
            noLoader);

    verticle.handleGetMessage(ctx);

    verify(ctx).fail(any(IOException.class));
    testContext.completeNow();
  }

  @Test
  void handleGetMessage_handlesRuntimeException(Vertx vertx, VertxTestContext testContext) {
    RoutingContext ctx = mock(RoutingContext.class);

    // Explicitly type the Optional to avoid ambiguity
    Optional<Consumer<Integer>> noListener = Optional.empty();
    WebServerVerticle.ContractLoader noLoader = (vx, p) -> Future.succeededFuture();

    WebServerVerticle verticle =
        new WebServerVerticle(
            8080,
            "openapi.yaml",
            m -> {
              throw new RuntimeException("Unexpected error");
            },
            noListener,
            noLoader);

    verticle.handleGetMessage(ctx);

    verify(ctx).fail(any(RuntimeException.class));
    testContext.completeNow();
  }

  @Test
  void onServerStarted_invokesListener(Vertx vertx, VertxTestContext testContext) {
    // Test the extracted callback logic explicitly
    AtomicInteger captured = new AtomicInteger();
    WebServerVerticle verticle =
        new WebServerVerticle(
            8080,
            "openapi.yaml",
            Optional.of(captured::set));

    HttpServer mockServer = mock(HttpServer.class);
    when(mockServer.actualPort()).thenReturn(9999);

    Promise<Void> promise = Promise.promise();
    verticle.onServerStarted(mockServer, promise);

    assertThat(captured.get()).isEqualTo(9999);
    assertThat(promise.future().succeeded()).isTrue();
    testContext.completeNow();
  }
}
