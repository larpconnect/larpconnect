package com.larpconnect.njall.api.verticle;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.larpconnect.njall.proto.MessageReply;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.Observability;
import com.larpconnect.njall.proto.ProtoDef;
import com.larpconnect.njall.proto.WebfingerResponse;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
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
    testContext.completeNow();
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
    vertx.close().onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void handleMessage_successWithTraceparent(VertxTestContext testContext) {
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

    var verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(
              byte[] spanId, MessageRequest message, Promise<MessageReply> responsePromise) {
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
                  byte[] originalTraceId =
                      new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
                  byte[] originalSpanId = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
                  Observability obs =
                      Observability.newBuilder()
                          .setTraceId(com.google.protobuf.ByteString.copyFrom(originalTraceId))
                          .setSpanId(com.google.protobuf.ByteString.copyFrom(originalSpanId))
                          .build();
                  var message = MessageRequest.newBuilder().setTraceparent(obs).build();

                  vertx.eventBus().send(CHANNEL, message);

                  vertx.setTimer(
                      100,
                      t -> {
                        testContext.verify(
                            () -> {
                              assertThat(handled.get()).isTrue();
                              assertThat(mdcTraceId.get())
                                  .isEqualTo("0102030405060708090a0b0c0d0e0f10");
                              assertThat(mdcParentSpanId.get()).isEqualTo("0102030405060708");
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

    var verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(
              byte[] spanId, MessageRequest message, Promise<MessageReply> responsePromise) {
            handled.set(true);
            throw new IllegalStateException("Test exception");
          }
        };

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  var message = MessageRequest.newBuilder().build();

                  vertx
                      .eventBus()
                      .<MessageRequest>request(CHANNEL, message)
                      .onComplete(
                          testContext.failing(
                              err ->
                                  testContext.verify(
                                      () -> {
                                        ReplyException re = (ReplyException) err;
                                        assertThat(re.failureCode()).isEqualTo(-1);
                                        assertThat(re.getMessage()).isEqualTo("Internal Error");
                                        assertThat(handled.get()).isTrue();
                                        assertThat(MDC.get("trace_id")).isNull();
                                        assertThat(MDC.get("parent_span_id")).isNull();
                                        assertThat(MDC.get("span_id")).isNull();
                                        testContext.completeNow();
                                      })));
                }));
  }

  @Test
  void handleMessage_successWithFailedPromise(VertxTestContext testContext) {
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

    var verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(
              byte[] spanId, MessageRequest message, Promise<MessageReply> responsePromise) {
            handled.set(true);
            responsePromise.fail(new IllegalStateException("Failed promise"));
            return BasicResponse.CONTINUE;
          }
        };

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  var message = MessageRequest.newBuilder().build();

                  vertx
                      .eventBus()
                      .<MessageRequest>request(CHANNEL, message)
                      .onComplete(
                          testContext.failing(
                              err ->
                                  testContext.verify(
                                      () -> {
                                        ReplyException re = (ReplyException) err;
                                        assertThat(re.failureCode()).isEqualTo(-1);
                                        assertThat(re.getMessage()).isEqualTo("Internal Error");
                                        assertThat(handled.get()).isTrue();
                                        testContext.completeNow();
                                      })));
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

    var verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(
              byte[] spanId, MessageRequest message, Promise<MessageReply> responsePromise) {
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
                  var message = MessageRequest.newBuilder().build(); // No traceparent

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

    var verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(
              byte[] spanId, MessageRequest message, Promise<MessageReply> responsePromise) {
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
                  var message = MessageRequest.newBuilder().setTraceparent(obs).build();

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

    var verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(
              byte[] spanId, MessageRequest message, Promise<MessageReply> responsePromise) {
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
                  var message = MessageRequest.newBuilder().build(); // No traceparent

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
  void handleMessage_successWithReply(VertxTestContext testContext) {
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

    var verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(
              byte[] spanId, MessageRequest message, Promise<MessageReply> responsePromise) {
            handled.set(true);
            WebfingerResponse response = WebfingerResponse.newBuilder().setSubject("reply").build();
            responsePromise.complete(
                MessageReply.newBuilder()
                    .setProto(
                        ProtoDef.newBuilder()
                            .setProtobufName("WebfingerResponse")
                            .setMessage(Any.pack(response))
                            .build())
                    .build());
            return BasicResponse.CONTINUE;
          }
        };

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  var message = MessageRequest.newBuilder().build(); // No traceparent

                  vertx
                      .eventBus()
                      .request(CHANNEL, message)
                      .onComplete(
                          testContext.succeeding(
                              reply -> {
                                testContext.verify(
                                    () -> {
                                      assertThat(handled.get()).isTrue();
                                      assertThat(reply.body()).isInstanceOf(MessageReply.class);
                                      MessageReply mr = (MessageReply) reply.body();
                                      assertThat(
                                              mr.getProto()
                                                  .getMessage()
                                                  .is(WebfingerResponse.class))
                                          .isTrue();
                                      testContext.completeNow();
                                    });
                              }));
                }));
  }

  @Test
  void handleMessage_successWithServer(VertxTestContext testContext) {
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

    var mdcServer = new AtomicReference<String>();

    var verticle =
        new AbstractLcVerticle(
            CHANNEL, mockRandom, () -> UUID.fromString("12345678-1234-1234-1234-123456789abc")) {
          @Override
          protected MessageResponse handleMessage(
              byte[] spanId, MessageRequest message, Promise<MessageReply> responsePromise) {
            handled.set(true);
            mdcServer.set(MDC.get("server"));
            responsePromise.complete(MessageReply.newBuilder().build());
            return BasicResponse.CONTINUE;
          }
        };

    vertx
        .deployVerticle(verticle)
        .onComplete(
            testContext.succeeding(
                id -> {
                  var message = MessageRequest.newBuilder().setServer("test-server").build();

                  vertx
                      .eventBus()
                      .request(CHANNEL, message)
                      .onComplete(
                          testContext.succeeding(
                              reply -> {
                                testContext.verify(
                                    () -> {
                                      assertThat(handled.get()).isTrue();
                                      assertThat(mdcServer.get()).isEqualTo("test-server");
                                      assertThat(MDC.get("server")).isNull();
                                      MessageReply mr = (MessageReply) reply.body();
                                      assertThat(mr.getServer()).isEqualTo("test-server");
                                      testContext.completeNow();
                                    });
                              }));
                }));
  }
}
