package com.larpconnect.njall.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.larpconnect.njall.proto.MessageReply;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.Nodeinfo22;
import com.larpconnect.njall.proto.NodeinfoJrd;
import com.larpconnect.njall.proto.NodeinfoJrdLink;
import com.larpconnect.njall.proto.NodeinfoSoftware;
import com.larpconnect.njall.proto.ProtoDef;
import com.larpconnect.njall.proto.WebfingerResponse;
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
    var actualPort = new AtomicInteger();
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
  void webfingerEndpoint_succeeds_withSubject(Vertx vertx, VertxTestContext testContext) {
    vertx
        .eventBus()
        .<MessageRequest>consumer(
            "http.well-known.webfinger.request",
            msg -> {
              msg.reply(
                  MessageReply.newBuilder()
                      .setProto(
                          ProtoDef.newBuilder()
                              .setProtobufName("WebfingerResponse")
                              .setMessage(
                                  Any.pack(
                                      WebfingerResponse.newBuilder()
                                          .setSubject("acct:system@localhost")
                                          .build())))
                      .build());
            });

    webClient
        .get("/.well-known/webfinger")
        .send()
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertThat(response.statusCode()).isEqualTo(200);
                          assertThat(response.bodyAsJsonObject().getString("subject"))
                              .isEqualTo("acct:system@localhost");
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
    var badVerticle = new WebServerVerticle(0, "missing.yaml");
    vertx
        .deployVerticle(badVerticle)
        .onComplete(
            testContext.failing(
                err ->
                    testContext.verify(
                        () -> {
                          assertThat(err.getMessage()).contains("resource missing.yaml not found.");
                          testContext.completeNow();
                        })));
  }

  @Test
  void serializationException_returns500(Vertx vertx, VertxTestContext testContext) {
    WebServerVerticle.Serializer badSerializer =
        m -> {
          throw new IOException("test io exception");
        };
    var actualPort = new AtomicInteger();
    var badVerticle =
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
          throw new IllegalStateException("test runtime exception");
        };
    var actualPort = new AtomicInteger();
    var badVerticle =
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
    testContext.verify(
        () -> {
          assertThat(verticle.actualPort()).isEqualTo(port);
          var unstarted = new WebServerVerticle();
          assertThat(unstarted.actualPort()).isEqualTo(8080);
          testContext.completeNow();
        });
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
                          assertThat(response.statusCode()).isEqualTo(204);
                          testContext.completeNow();
                        })));
  }

  @Test
  void handleNodeinfoWellKnown_succeeds(Vertx vertx, VertxTestContext testContext) {
    vertx
        .eventBus()
        .<MessageRequest>consumer(
            "http.well-known.nodeinfo.request",
            msg -> {
              msg.reply(
                  MessageReply.newBuilder()
                      .setProto(
                          ProtoDef.newBuilder()
                              .setProtobufName("NodeinfoJrd")
                              .setMessage(
                                  Any.pack(
                                      NodeinfoJrd.newBuilder()
                                          .addLinks(
                                              NodeinfoJrdLink.newBuilder()
                                                  .setRel(
                                                      "http://nodeinfo.diaspora.software/ns/schema/2.2")
                                                  .setHref("http://localhost:8080/admin/nodeinfo")
                                                  .build())
                                          .build())))
                      .build());
            });

    webClient
        .get("/.well-known/nodeinfo")
        .send()
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertThat(response.statusCode()).isEqualTo(200);
                          assertThat(response.bodyAsJsonObject().containsKey("links")).isTrue();
                          testContext.completeNow();
                        })));
  }

  @Test
  void handleNodeinfoAdmin_succeeds(Vertx vertx, VertxTestContext testContext) {
    vertx
        .eventBus()
        .<MessageRequest>consumer(
            "http.admin.nodeinfo.request",
            msg -> {
              msg.reply(
                  MessageReply.newBuilder()
                      .setProto(
                          ProtoDef.newBuilder()
                              .setProtobufName("Nodeinfo22")
                              .setMessage(
                                  Any.pack(
                                      Nodeinfo22.newBuilder()
                                          .setVersion("2.2")
                                          .setSoftware(
                                              NodeinfoSoftware.newBuilder()
                                                  .setName("larpconnect")
                                                  .setVersion("0.0.1")
                                                  .build())
                                          .build())))
                      .build());
            });

    webClient
        .get("/admin/nodeinfo")
        .send()
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertThat(response.statusCode()).isEqualTo(200);
                          assertThat(response.bodyAsJsonObject().getString("version"))
                              .isEqualTo("2.2");
                          testContext.completeNow();
                        })));
  }
}
