package com.larpconnect.njall.api.verticle;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.Parameter;
import com.larpconnect.njall.proto.WebfingerResponse;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
final class WebfingerVerticleTest {

  private Vertx vertx;
  private WebfingerVerticle verticle;

  @BeforeEach
  void setUp(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    IdGenerator idGen = () -> UUID.fromString("12345678-1234-1234-1234-123456789abc");
    verticle = new WebfingerVerticle(idGen);
    vertx.deployVerticle(verticle).onComplete(testContext.succeedingThenComplete());
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
    vertx.close().onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void webfinger_withResource_returnsResourceAsSubject(VertxTestContext testContext) {
    MessageRequest request =
        MessageRequest.newBuilder()
            .addParameters(
                Parameter.newBuilder()
                    .setKey("resource")
                    .setStringValue("acct:alice@example.com")
                    .build())
            .build();

    vertx
        .eventBus()
        .<WebfingerResponse>request(WebfingerVerticle.CHANNEL, request)
        .onComplete(
            testContext.succeeding(
                reply -> {
                  testContext.verify(
                      () -> {
                        WebfingerResponse response = reply.body();
                        assertThat(response.getSubject()).isEqualTo("acct:alice@example.com");
                        testContext.completeNow();
                      });
                }));
  }

  @Test
  void webfinger_withoutResource_returnsEmptyResponse(VertxTestContext testContext) {
    MessageRequest request = MessageRequest.newBuilder().build();

    vertx
        .eventBus()
        .<WebfingerResponse>request(WebfingerVerticle.CHANNEL, request)
        .onComplete(
            testContext.succeeding(
                reply -> {
                  testContext.verify(
                      () -> {
                        WebfingerResponse response = reply.body();
                        assertThat(response.getSubject()).isEmpty();
                        testContext.completeNow();
                      });
                }));
  }
}
