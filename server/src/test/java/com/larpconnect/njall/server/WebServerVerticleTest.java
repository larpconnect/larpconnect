package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Closeables;
import com.larpconnect.njall.proto.MessageServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class WebServerVerticleTest {

  private final ResourceLoader defaultLoader =
      path -> getClass().getClassLoader().getResourceAsStream(path);

  private static int getFreePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  @Test
  void start_server(Vertx vertx, VertxTestContext testContext) {
    int grpcPort = getFreePort();
    int webPort = getFreePort();
    JsonObject config = new JsonObject().put("grpc.port", grpcPort).put("web.port", webPort);
    vertx
        .deployVerticle(
            new WebServerVerticle(defaultLoader), new DeploymentOptions().setConfig(config))
        .onSuccess(id -> testContext.completeNow())
        .onFailure(testContext::failNow);
  }

  @Test
  void openapiEndpoint_returnsYaml(Vertx vertx, VertxTestContext testContext) {
    int grpcPort = getFreePort();
    int webPort = getFreePort();
    JsonObject config = new JsonObject().put("grpc.port", grpcPort).put("web.port", webPort);
    vertx
        .deployVerticle(
            new WebServerVerticle(defaultLoader), new DeploymentOptions().setConfig(config))
        .compose(id -> WebClient.create(vertx).get(webPort, "localhost", "/openapi.yaml").send())
        .onSuccess(
            response -> {
              testContext.verify(
                  () -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.getHeader("content-type")).isEqualTo("text/yaml");
                    assertThat(response.bodyAsString()).contains("openapi: 3.0.3");
                    testContext.completeNow();
                  });
            })
        .onFailure(testContext::failNow);
  }

  @Test
  void grpcEndpoint_returnsMessage(Vertx vertx, VertxTestContext testContext) {
    int grpcPort = getFreePort();
    int webPort = getFreePort();
    JsonObject config = new JsonObject().put("grpc.port", grpcPort).put("web.port", webPort);
    vertx
        .deployVerticle(
            new WebServerVerticle(defaultLoader), new DeploymentOptions().setConfig(config))
        .onSuccess(
            id -> {
              ManagedChannel channel =
                  ManagedChannelBuilder.forAddress("127.0.0.1", grpcPort).usePlaintext().build();
              MessageServiceGrpc.MessageServiceStub stub = MessageServiceGrpc.newStub(channel);
              stub.getMessage(
                  com.google.protobuf.Empty.getDefaultInstance(),
                  new io.grpc.stub.StreamObserver<>() {
                    @Override
                    public void onNext(com.larpconnect.njall.proto.Message value) {
                      testContext.verify(
                          () -> {
                            assertThat(value.getMessageType()).isEqualTo("Hello World");
                          });
                    }

                    @Override
                    public void onError(Throwable t) {
                      testContext.failNow(t);
                    }

                    @Override
                    public void onCompleted() {
                      testContext.completeNow();
                    }
                  });
            })
        .onFailure(testContext::failNow);
  }

  @Test
  void start_fails_whenGrpcPortInUse(Vertx vertx, VertxTestContext testContext) throws Exception {
    int grpcPort = getFreePort();
    int webPort = getFreePort();
    ServerSocket serverSocket = new ServerSocket(grpcPort);

    JsonObject config = new JsonObject().put("grpc.port", grpcPort).put("web.port", webPort);
    vertx
        .deployVerticle(
            new WebServerVerticle(defaultLoader), new DeploymentOptions().setConfig(config))
        .onSuccess(
            id -> {
              try {
                Closeables.close(serverSocket, true);
              } catch (IOException e) {
                // ignore
              }
              testContext.failNow(
                  new AssertionError("Should have failed to bind gRPC port " + grpcPort));
            })
        .onFailure(
            err -> {
              try {
                Closeables.close(serverSocket, true);
              } catch (IOException e) {
                // ignore
              }
              testContext.completeNow();
            });
  }

  @Test
  void start_fails_whenWebPortInUse(Vertx vertx, VertxTestContext testContext) throws Exception {
    int grpcPort = getFreePort();
    int webPort = getFreePort();
    ServerSocket serverSocket = new ServerSocket(webPort);

    JsonObject config = new JsonObject().put("grpc.port", grpcPort).put("web.port", webPort);
    vertx
        .deployVerticle(
            new WebServerVerticle(defaultLoader), new DeploymentOptions().setConfig(config))
        .onSuccess(
            id -> {
              try {
                Closeables.close(serverSocket, true);
              } catch (IOException e) {
                // ignore
              }
              testContext.failNow(
                  new AssertionError("Should have failed to bind web port " + webPort));
            })
        .onFailure(
            err -> {
              try {
                Closeables.close(serverSocket, true);
              } catch (IOException e) {
                // ignore
              }
              testContext.completeNow();
            });
  }

  @Test
  void start_fails_whenOpenApiMissing(Vertx vertx, VertxTestContext testContext) {
    int grpcPort = getFreePort();
    int webPort = getFreePort();
    JsonObject config =
        new JsonObject()
            .put("grpc.port", grpcPort)
            .put("web.port", webPort)
            .put("openapi.path", "nonexistent.yaml");

    vertx
        .deployVerticle(
            new WebServerVerticle(defaultLoader), new DeploymentOptions().setConfig(config))
        .onSuccess(id -> testContext.failNow(new AssertionError("Should have failed to start")))
        .onFailure(err -> testContext.completeNow());
  }

  @Test
  void start_fails_whenOpenApiReadFails(Vertx vertx, VertxTestContext testContext) {
    int grpcPort = getFreePort();
    int webPort = getFreePort();
    JsonObject config = new JsonObject().put("grpc.port", grpcPort).put("web.port", webPort);

    ResourceLoader throwingLoader =
        path ->
            new InputStream() {
              @Override
              public int read() throws IOException {
                throw new IOException("Read failure");
              }

              @Override
              public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("Read failure");
              }
            };

    vertx
        .deployVerticle(
            new WebServerVerticle(throwingLoader), new DeploymentOptions().setConfig(config))
        .onSuccess(id -> testContext.failNow(new AssertionError("Should have failed to start")))
        .onFailure(err -> testContext.completeNow());
  }

}
