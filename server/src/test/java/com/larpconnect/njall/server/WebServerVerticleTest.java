package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
final class WebServerVerticleTest {

  private WebClient webClient;
  private int port;
  private WebServerVerticle verticle;

  @BeforeEach
  void setUp(Vertx vertx, VertxTestContext testContext) {
    AtomicInteger actualPort = new AtomicInteger();
    verticle =
        new WebServerVerticle(
            0, "openapi.yaml", m -> "{\"test\":\"json\"}", Optional.of(actualPort::set));

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  this.port = actualPort.get();
                  this.webClient =
                      WebClient.create(
                          vertx,
                          new WebClientOptions().setDefaultPort(port).setDefaultHost("localhost"));
                  testContext.completeNow();
                }));
  }

  @AfterEach
  void tearDown() {
    if (webClient != null) {
      webClient.close();
    }
  }

  @Test
  void webfingerEndpoint_succeeds_withSubject(VertxTestContext testContext) {
    webClient
        .get("/.well-known/webfinger")
        .send()
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertThat(response.statusCode()).isEqualTo(200);
                          assertThat(response.bodyAsJsonObject().containsKey("subject")).isTrue();
                          testContext.completeNow();
                        })));
  }

  @Test
  void getMessageEndpoint_succeeds(VertxTestContext testContext) {
    webClient
        .get("/v1/message")
        .send()
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertThat(response.statusCode()).isEqualTo(200);
                          assertThat(response.bodyAsJsonObject().getString("test"))
                              .isEqualTo("json");
                          testContext.completeNow();
                        })));
  }

  @Test
  void missingOpenApiSpec_failsDeployment(Vertx vertx, VertxTestContext testContext) {
    WebServerVerticle badVerticle = new WebServerVerticle(0, "missing.yaml");
    vertx
        .deployVerticle(badVerticle)
        .onComplete(
            testContext.failing(
                err ->
                    testContext.verify(
                        () -> {
                          assertThat(err.getMessage())
                              .contains("missing.yaml not found on classpath");
                          testContext.completeNow();
                        })));
  }

  @Test
  void serializationException_returns500(Vertx vertx, VertxTestContext testContext) {
    WebServerVerticle.Serializer badSerializer =
        m -> {
          throw new IOException("test io exception");
        };
    AtomicInteger actualPort = new AtomicInteger();
    WebServerVerticle badVerticle =
        new WebServerVerticle(0, "openapi.yaml", badSerializer, Optional.of(actualPort::set));

    vertx
        .deployVerticle(badVerticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  WebClient client =
                      WebClient.create(
                          vertx,
                          new WebClientOptions()
                              .setDefaultPort(actualPort.get())
                              .setDefaultHost("localhost"));
                  client
                      .get("/v1/message")
                      .send()
                      .onComplete(
                          testContext.succeeding(
                              response ->
                                  testContext.verify(
                                      () -> {
                                        assertThat(response.statusCode()).isEqualTo(500);
                                        client.close();
                                        testContext.completeNow();
                                      })));
                }));
  }

  @Test
  void serializationRuntimeException_returns500(Vertx vertx, VertxTestContext testContext) {
    WebServerVerticle.Serializer badSerializer =
        m -> {
          throw new RuntimeException("test runtime exception");
        };
    AtomicInteger actualPort = new AtomicInteger();
    WebServerVerticle badVerticle =
        new WebServerVerticle(0, "openapi.yaml", badSerializer, Optional.of(actualPort::set));

    vertx
        .deployVerticle(badVerticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  WebClient client =
                      WebClient.create(
                          vertx,
                          new WebClientOptions()
                              .setDefaultPort(actualPort.get())
                              .setDefaultHost("localhost"));
                  client
                      .get("/v1/message")
                      .send()
                      .onComplete(
                          testContext.succeeding(
                              response ->
                                  testContext.verify(
                                      () -> {
                                        assertThat(response.statusCode()).isEqualTo(500);
                                        client.close();
                                        testContext.completeNow();
                                      })));
                }));
  }

  @Test
  void actualPort_returnsConfiguredPort(Vertx vertx, VertxTestContext testContext) {
    assertThat(verticle.actualPort()).isEqualTo(port);
    WebServerVerticle unstarted = new WebServerVerticle();
    assertThat(unstarted.actualPort()).isEqualTo(8080); // default
    testContext.completeNow();
  }

  @Test
  void healthcheckEndpoint_succeeds(VertxTestContext testContext) {
    webClient
        .get("/healthz")
        .send()
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertThat(response.statusCode()).isEqualTo(200);
                          testContext.completeNow();
                        })));
  }
}
