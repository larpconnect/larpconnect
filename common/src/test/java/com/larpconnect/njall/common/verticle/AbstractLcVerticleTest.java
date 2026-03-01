package com.larpconnect.njall.common.verticle;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import com.larpconnect.njall.common.codec.ProtoCodecRegistry;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.proto.Observability;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.inject.Provider;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;
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
    vertx.eventBus().registerDefaultCodec(Message.class, new ProtoCodecRegistry());
    testContext.completeNow();
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
    vertx.close().onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void handleMessage_successWithOnlyTraceId(VertxTestContext testContext) {
    var handled = new AtomicBoolean(false);
    Provider<RandomGenerator> mockRandom =
        () ->
            new RandomGenerator() {
              @Override
              public long nextLong() {
                return 0;
              }

              @Override
              public void nextBytes(byte[] bytes) {}
            };
    AbstractLcVerticle verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(byte[] spanId, Message message) {
            handled.set(true);
            return BasicResponse.CONTINUE;
          }
        };
    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  Observability obs =
                      Observability.newBuilder()
                          .setTraceId(
                              com.google.protobuf.ByteString.copyFrom(
                                  new byte[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}))
                          .build();
                  Message message = Message.newBuilder().setTraceparent(obs).build();
                  vertx.eventBus().send(CHANNEL, message);
                  vertx.setTimer(
                      100,
                      t ->
                          testContext.verify(
                              () -> {
                                assertThat(handled.get()).isTrue();
                                testContext.completeNow();
                              }));
                }));
  }

  @Test
  void handleMessage_successWithOnlySpanId(VertxTestContext testContext) {
    var handled = new AtomicBoolean(false);
    Provider<RandomGenerator> mockRandom =
        () ->
            new RandomGenerator() {
              @Override
              public long nextLong() {
                return 0;
              }

              @Override
              public void nextBytes(byte[] bytes) {}
            };
    AbstractLcVerticle verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(byte[] spanId, Message message) {
            handled.set(true);
            return BasicResponse.CONTINUE;
          }
        };
    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  Observability obs =
                      Observability.newBuilder()
                          .setSpanId(
                              com.google.protobuf.ByteString.copyFrom(
                                  new byte[] {2, 2, 2, 2, 2, 2, 2, 2}))
                          .build();
                  Message message = Message.newBuilder().setTraceparent(obs).build();
                  vertx.eventBus().send(CHANNEL, message);
                  vertx.setTimer(
                      100,
                      t ->
                          testContext.verify(
                              () -> {
                                assertThat(handled.get()).isTrue();
                                testContext.completeNow();
                              }));
                }));
  }

  @Test
  void constructor_twoArgs_compilesAndWorks() {
    IdGenerator mockIdGenerator = () -> UUID.fromString("12345678-1234-1234-1234-123456789abc");
    AbstractLcVerticle verticle =
        new AbstractLcVerticle("test-channel", mockIdGenerator) {
          @Override
          protected MessageResponse handleMessage(byte[] spanId, Message message) {
            return BasicResponse.CONTINUE;
          }
        };
    assertThat(verticle).isNotNull();
  }

  @Test
  void handleMessage_success(VertxTestContext testContext) {
    var handled = new AtomicBoolean(false);
    var receivedSpanId = new AtomicReference<byte[]>();
    var mdcTraceId = new AtomicReference<String>();
    var mdcParentSpanId = new AtomicReference<String>();
    var mdcSpanId = new AtomicReference<String>();

    byte[] expectedSpanId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    Provider<RandomGenerator> mockRandom =
        () ->
            new RandomGenerator() {
              @Override
              public long nextLong() {
                return 0;
              }

              @Override
              public void nextBytes(byte[] bytes) {
                System.arraycopy(expectedSpanId, 0, bytes, 0, expectedSpanId.length);
              }
            };

    AbstractLcVerticle verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(byte[] spanId, Message message) {
            handled.set(true);
            receivedSpanId.set(spanId);
            mdcTraceId.set(MDC.get("trace_id"));
            mdcParentSpanId.set(MDC.get("parent_span_id"));
            mdcSpanId.set(MDC.get("span_id"));
            return BasicResponse.CONTINUE;
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

                  vertx.eventBus().<Message>request(CHANNEL, message).onComplete(ar -> {});

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
    var handled = new AtomicBoolean(false);

    byte[] expectedSpanId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    Provider<RandomGenerator> mockRandom =
        () ->
            new RandomGenerator() {
              @Override
              public long nextLong() {
                return 0;
              }

              @Override
              public void nextBytes(byte[] bytes) {
                System.arraycopy(expectedSpanId, 0, bytes, 0, expectedSpanId.length);
              }
            };

    AbstractLcVerticle verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(byte[] spanId, Message message) {
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
    var handled = new AtomicBoolean(false);

    byte[] expectedSpanId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    Provider<RandomGenerator> mockRandom =
        () ->
            new RandomGenerator() {
              @Override
              public long nextLong() {
                return 0;
              }

              @Override
              public void nextBytes(byte[] bytes) {
                System.arraycopy(expectedSpanId, 0, bytes, 0, expectedSpanId.length);
              }
            };

    var mdcTraceId = new AtomicReference<String>();
    var mdcParentSpanId = new AtomicReference<String>();
    var mdcSpanId = new AtomicReference<String>();

    AbstractLcVerticle verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(byte[] spanId, Message message) {
            handled.set(true);
            mdcTraceId.set(MDC.get("trace_id"));
            mdcParentSpanId.set(MDC.get("parent_span_id"));
            mdcSpanId.set(MDC.get("span_id"));
            return BasicResponse.CONTINUE;
          }
        };

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  Message message = Message.newBuilder().build(); // No traceparent

                  vertx.eventBus().send(CHANNEL, message);

                  vertx.setTimer(
                      100,
                      t -> {
                        testContext.verify(
                            () -> {
                              assertThat(handled.get()).isTrue();
                              assertThat(mdcTraceId.get())
                                  .isEqualTo("12345678123412341234123456789abc");
                              assertThat(mdcParentSpanId.get()).isEqualTo("1111111111111111");
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
  void handleMessage_successWithEmptyObservability(VertxTestContext testContext) {
    var handled = new AtomicBoolean(false);

    byte[] expectedSpanId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    Provider<RandomGenerator> mockRandom =
        () ->
            new RandomGenerator() {
              @Override
              public long nextLong() {
                return 0;
              }

              @Override
              public void nextBytes(byte[] bytes) {
                System.arraycopy(expectedSpanId, 0, bytes, 0, expectedSpanId.length);
              }
            };

    var mdcTraceId = new AtomicReference<String>();
    var mdcParentSpanId = new AtomicReference<String>();
    var mdcSpanId = new AtomicReference<String>();

    AbstractLcVerticle verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(byte[] spanId, Message message) {
            handled.set(true);
            mdcTraceId.set(MDC.get("trace_id"));
            mdcParentSpanId.set(MDC.get("parent_span_id"));
            mdcSpanId.set(MDC.get("span_id"));
            return BasicResponse.CONTINUE;
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

                  vertx.eventBus().send(CHANNEL, message);

                  vertx.setTimer(
                      100,
                      t -> {
                        testContext.verify(
                            () -> {
                              assertThat(handled.get()).isTrue();
                              assertThat(mdcTraceId.get())
                                  .isEqualTo("12345678123412341234123456789abc");
                              assertThat(mdcParentSpanId.get()).isEqualTo("1111111111111111");
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
  void handleMessage_successWithShutdown(VertxTestContext testContext) {
    var handled = new AtomicBoolean(false);

    byte[] expectedSpanId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    Provider<RandomGenerator> mockRandom =
        () ->
            new RandomGenerator() {
              @Override
              public long nextLong() {
                return 0;
              }

              @Override
              public void nextBytes(byte[] bytes) {
                System.arraycopy(expectedSpanId, 0, bytes, 0, expectedSpanId.length);
              }
            };

    var mdcTraceId = new AtomicReference<String>();
    var mdcParentSpanId = new AtomicReference<String>();
    var mdcSpanId = new AtomicReference<String>();

    AbstractLcVerticle verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(byte[] spanId, Message message) {
            handled.set(true);
            mdcTraceId.set(MDC.get("trace_id"));
            mdcParentSpanId.set(MDC.get("parent_span_id"));
            mdcSpanId.set(MDC.get("span_id"));
            return BasicResponse.SHUTDOWN;
          }
        };

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  Message message = Message.newBuilder().build(); // No traceparent

                  vertx.eventBus().send(CHANNEL, message);

                  vertx.setTimer(
                      100,
                      t -> {
                        testContext.verify(
                            () -> {
                              assertThat(handled.get()).isTrue();
                              assertThat(mdcTraceId.get())
                                  .isEqualTo("12345678123412341234123456789abc");
                              assertThat(mdcParentSpanId.get()).isEqualTo("1111111111111111");
                              assertThat(mdcSpanId.get()).isEqualTo("0102030405060708");
                              assertThat(MDC.get("trace_id")).isNull();
                              assertThat(MDC.get("parent_span_id")).isNull();
                              assertThat(MDC.get("span_id")).isNull();
                              testContext.completeNow();
                            });
                      });
                }));
  }
}
