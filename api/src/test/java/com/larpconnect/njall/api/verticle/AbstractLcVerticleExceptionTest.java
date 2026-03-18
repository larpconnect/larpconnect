package com.larpconnect.njall.api.verticle;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.proto.MessageReply;
import com.larpconnect.njall.proto.MessageRequest;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.inject.Provider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
final class AbstractLcVerticleExceptionTest {

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

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
    throw (E) e;
  }

  @Test
  void handleMessage_successWithIOException() throws InterruptedException {
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
            sneakyThrow(new IOException("Test IOException"));
            return BasicResponse.CONTINUE;
          }
        };

    CountDownLatch latch = new CountDownLatch(1);

    vertx.exceptionHandler(
        err -> {
          if (err instanceof UncheckedIOException) {
            assertThat(err.getCause().getMessage()).isEqualTo("Test IOException");
            assertThat(handled.get()).isTrue();
            latch.countDown();
          }
        });

    vertx
        .deployVerticle(verticle)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                var message = MessageRequest.newBuilder().build();
                vertx.eventBus().send(CHANNEL, message);
              }
            });

    boolean success = latch.await(5, TimeUnit.SECONDS);
    assertThat(success).isTrue();
  }

  @Test
  void handleMessage_successWithRuntimeException(VertxTestContext testContext) {
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
            throw new RuntimeException("Test RuntimeException inside Closer block");
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
                                        io.vertx.core.eventbus.ReplyException re =
                                            (io.vertx.core.eventbus.ReplyException) err;
                                        assertThat(re.failureCode()).isEqualTo(-1);
                                        assertThat(re.getMessage()).isEqualTo("Internal Error");
                                        assertThat(handled.get()).isTrue();
                                        testContext.completeNow();
                                      })));
                }));
  }
}
