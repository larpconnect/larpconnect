package com.larpconnect.njall.common.verticle;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.proto.Observability;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;

@ExtendWith(VertxExtension.class)
final class AbstractLcVerticleTest {

  private static final String CHANNEL = "test-channel";

  private Vertx vertx;

  @BeforeEach
  void setUp(VertxTestContext testContext) {
    vertx = Vertx.vertx();
    vertx
        .eventBus()
        .registerDefaultCodec(
            Message.class,
            new io.vertx.core.eventbus.MessageCodec<Message, Message>() {
              @Override
              public void encodeToWire(io.vertx.core.buffer.Buffer buffer, Message s) {}

              @Override
              public Message decodeFromWire(int pos, io.vertx.core.buffer.Buffer buffer) {
                return null;
              }

              @Override
              public Message transform(Message s) {
                return s;
              }

              @Override
              public String name() {
                return "test-codec";
              }

              @Override
              public byte systemCodecID() {
                return -1;
              }
            });
    testContext.completeNow();
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
    vertx.close().onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void handleMessage_success(VertxTestContext testContext) {
    AtomicBoolean handled = new AtomicBoolean(false);
    AtomicReference<byte[]> receivedSpanId = new AtomicReference<>();
    AtomicReference<String> mdcTraceId = new AtomicReference<>();
    AtomicReference<String> mdcParentSpanId = new AtomicReference<>();
    AtomicReference<String> mdcSpanId = new AtomicReference<>();

    byte[] expectedSpanId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    Consumer<byte[]> mockRandom =
        bytes -> System.arraycopy(expectedSpanId, 0, bytes, 0, expectedSpanId.length);

    AbstractLcVerticle verticle =
        new AbstractLcVerticle(CHANNEL, mockRandom) {
          @Override
          protected void handleMessage(byte[] spanId, Message message) {
            handled.set(true);
            receivedSpanId.set(spanId);
            mdcTraceId.set(MDC.get("trace_id"));
            mdcParentSpanId.set(MDC.get("parent_span_id"));
            mdcSpanId.set(MDC.get("span_id"));
          }
        };

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  byte[] traceIdBytes = new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
                  byte[] parentSpanIdBytes = new byte[] {2, 2, 2, 2, 2, 2, 2, 2};

                  Observability obs =
                      Observability.newBuilder()
                          .setTraceId(ByteString.copyFrom(traceIdBytes))
                          .setSpanId(ByteString.copyFrom(parentSpanIdBytes))
                          .build();

                  Message message = Message.newBuilder().setTraceparent(obs).build();

                  vertx
                      .eventBus()
                      .<Message>request(CHANNEL, message)
                      .onComplete(
                          ar -> {
                            // request will timeout because we don't reply, but handleMessage is
                            // still called
                          });

                  vertx.setTimer(
                      100,
                      t -> {
                        testContext.verify(
                            () -> {
                              assertThat(handled.get()).isTrue();
                              assertThat(receivedSpanId.get()).isEqualTo(expectedSpanId);
                              assertThat(mdcTraceId.get())
                                  .isEqualTo("01010101010101010101010101010101");
                              assertThat(mdcParentSpanId.get()).isEqualTo("0202020202020202");
                              assertThat(mdcSpanId.get()).isEqualTo("0102030405060708");

                              assertThat(MDC.get("trace_id")).isNull();
                              assertThat(MDC.get("parent_span_id")).isNull();
                              assertThat(MDC.get("span_id")).isNull();

                              testContext.completeNow();
                            });
                      });
                }));
  }

  @Test
  void handleMessage_successWithException(VertxTestContext testContext) {
    AtomicBoolean handled = new AtomicBoolean(false);

    byte[] expectedSpanId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    Consumer<byte[]> mockRandom =
        bytes -> System.arraycopy(expectedSpanId, 0, bytes, 0, expectedSpanId.length);

    AbstractLcVerticle verticle =
        new AbstractLcVerticle(CHANNEL, mockRandom) {
          @Override
          protected void handleMessage(byte[] spanId, Message message) {
            handled.set(true);
            throw new RuntimeException("Test exception");
          }
        };

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  Message message = Message.newBuilder().build();

                  // Send will not return anything. The failure happens in the handler.
                  vertx.eventBus().send(CHANNEL, message);

                  vertx.setTimer(
                      100,
                      t -> {
                        testContext.verify(
                            () -> {
                              assertThat(handled.get()).isTrue();
                              // MDC should be cleared despite the exception
                              assertThat(MDC.get("trace_id")).isNull();
                              assertThat(MDC.get("parent_span_id")).isNull();
                              assertThat(MDC.get("span_id")).isNull();

                              testContext.completeNow();
                            });
                      });
                }));
  }

  @Test
  void handleMessage_successWithoutTraceparent(VertxTestContext testContext) {
    AtomicBoolean handled = new AtomicBoolean(false);

    byte[] expectedSpanId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    Consumer<byte[]> mockRandom =
        bytes -> System.arraycopy(expectedSpanId, 0, bytes, 0, expectedSpanId.length);

    AbstractLcVerticle verticle =
        new AbstractLcVerticle(CHANNEL, mockRandom) {
          @Override
          protected void handleMessage(byte[] spanId, Message message) {
            handled.set(true);
          }
        };

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  Message message = Message.newBuilder().build(); // No traceparent

                  vertx.eventBus().<Message>request(CHANNEL, message).onComplete(ar -> {});

                  vertx.setTimer(
                      100,
                      t -> {
                        testContext.verify(
                            () -> {
                              assertThat(handled.get()).isTrue();
                              testContext.completeNow();
                            });
                      });
                }));
  }

  @Test
  void handleMessage_successWithEmptyObservability(VertxTestContext testContext) {
    AtomicBoolean handled = new AtomicBoolean(false);

    byte[] expectedSpanId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    Consumer<byte[]> mockRandom =
        bytes -> System.arraycopy(expectedSpanId, 0, bytes, 0, expectedSpanId.length);

    AbstractLcVerticle verticle =
        new AbstractLcVerticle(CHANNEL, mockRandom) {
          @Override
          protected void handleMessage(byte[] spanId, Message message) {
            handled.set(true);
          }
        };

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  Observability obs =
                      Observability.newBuilder().build(); // Empty traceId and spanId
                  Message message = Message.newBuilder().setTraceparent(obs).build();

                  vertx.eventBus().<Message>request(CHANNEL, message).onComplete(ar -> {});

                  vertx.setTimer(
                      100,
                      t -> {
                        testContext.verify(
                            () -> {
                              assertThat(handled.get()).isTrue();
                              testContext.completeNow();
                            });
                      });
                }));
  }
}
