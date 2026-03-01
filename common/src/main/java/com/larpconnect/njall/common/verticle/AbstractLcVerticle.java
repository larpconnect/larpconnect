package com.larpconnect.njall.common.verticle;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Closer;
import com.google.errorprone.annotations.Var;
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

              @Var String traceIdStr = null;
              @Var String parentSpanIdStr = null;

              if (message.hasTraceparent()) {
                Observability obs = message.getTraceparent();
                if (!obs.getTraceId().isEmpty()) {
                  traceIdStr = HEX.encode(obs.getTraceId().toByteArray());
                }
                if (!obs.getSpanId().isEmpty()) {
                  parentSpanIdStr = HEX.encode(obs.getSpanId().toByteArray());
                }
              }

              byte[] finalTraceIdBytes = null;
              byte[] finalParentSpanIdBytes = null;

              if (traceIdStr == null) {
                UUID uuid = idGenerator.generate();
                finalTraceIdBytes = new byte[16];
                ByteBuffer bb = ByteBuffer.wrap(finalTraceIdBytes);
                bb.putLong(uuid.getMostSignificantBits());
                bb.putLong(uuid.getLeastSignificantBits());
                traceIdStr = HEX.encode(finalTraceIdBytes);
              } else {
                finalTraceIdBytes = message.getTraceparent().getTraceId().toByteArray();
              }

              if (parentSpanIdStr == null) {
                finalParentSpanIdBytes =
                    new byte[] {0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11};
                parentSpanIdStr = "1111111111111111";
              } else {
                finalParentSpanIdBytes = message.getTraceparent().getSpanId().toByteArray();
              }

              String spanIdStr = HEX.encode(newSpanId);

              Observability.Builder obsBuilder =
                  message.hasTraceparent()
                      ? message.getTraceparent().toBuilder()
                      : Observability.newBuilder();
              obsBuilder.setTraceId(ByteString.copyFrom(finalTraceIdBytes));
              obsBuilder.setSpanId(ByteString.copyFrom(finalParentSpanIdBytes));

              Message finalMessage = message.toBuilder().setTraceparent(obsBuilder.build()).build();

              try (Closer closer = Closer.create()) {
                if (traceIdStr != null) {
                  closer.register(MDC.putCloseable("trace_id", traceIdStr));
                }
                if (parentSpanIdStr != null) {
                  closer.register(MDC.putCloseable("parent_span_id", parentSpanIdStr));
                }
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

  protected abstract MessageResponse handleMessage(byte[] spanId, Message message);
}
