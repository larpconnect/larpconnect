package com.larpconnect.njall.common.verticle;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Closer;
import com.google.protobuf.ByteString;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.proto.Observability;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import jakarta.inject.Provider;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.slf4j.MDC;

abstract class AbstractLcVerticle extends AbstractVerticle {
  private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();
  private static final int SPAN_ID_BYTES = 8;
  private static final int TRACE_ID_BYTES = 16;
  private static final byte DEFAULT_SPAN_ID_BYTE = 0x11;

  private final String channel;
  private final Provider<RandomGenerator> randomProvider;
  private final IdGenerator idGenerator;

  protected AbstractLcVerticle(String channel, IdGenerator idGenerator) {
    this(channel, ThreadLocalRandom::current, idGenerator);
  }

  protected AbstractLcVerticle(
      String channel, Provider<RandomGenerator> randomProvider, IdGenerator idGenerator) {
    this.channel = channel;
    this.randomProvider = randomProvider;
    this.idGenerator = idGenerator;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    vertx
        .eventBus()
        .<Message>consumer(
            channel,
            msg -> {
              Message message = msg.body();

              byte[] newSpanId = new byte[SPAN_ID_BYTES];
              randomProvider.get().nextBytes(newSpanId);

              Message finalMessage = ensureObservability(message);
              String traceIdStr =
                  HEX.encode(finalMessage.getTraceparent().getTraceId().toByteArray());
              String parentSpanIdStr =
                  HEX.encode(finalMessage.getTraceparent().getSpanId().toByteArray());
              String spanIdStr = HEX.encode(newSpanId);

              try (Closer closer = Closer.create()) {
                closer.register(MDC.putCloseable("trace_id", traceIdStr));
                closer.register(MDC.putCloseable("parent_span_id", parentSpanIdStr));
                closer.register(MDC.putCloseable("span_id", spanIdStr));

                MessageResponse response = handleMessage(newSpanId, finalMessage);
                if (response == BasicResponse.SHUTDOWN) {
                  vertx.eventBus().consumer(channel).unregister();
                }
              } catch (IOException e) {
                // Closer does not throw IOException in this context
              }
            });

    startPromise.complete();
  }

  private Message ensureObservability(Message message) {
    Observability.Builder obsBuilder =
        message.hasTraceparent()
            ? message.getTraceparent().toBuilder()
            : Observability.newBuilder();

    if (obsBuilder.getTraceId().isEmpty()) {
      UUID uuid = idGenerator.generate();
      byte[] finalTraceIdBytes = new byte[TRACE_ID_BYTES];
      ByteBuffer bb = ByteBuffer.wrap(finalTraceIdBytes);
      bb.putLong(uuid.getMostSignificantBits());
      bb.putLong(uuid.getLeastSignificantBits());
      obsBuilder.setTraceId(ByteString.copyFrom(finalTraceIdBytes));
    }

    if (obsBuilder.getSpanId().isEmpty()) {
      byte[] finalParentSpanIdBytes =
          new byte[] {
            DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE,
            DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE
          };
      obsBuilder.setSpanId(ByteString.copyFrom(finalParentSpanIdBytes));
    }

    return message.toBuilder().setTraceparent(obsBuilder.build()).build();
  }

  protected abstract MessageResponse handleMessage(byte[] spanId, Message message);
}
