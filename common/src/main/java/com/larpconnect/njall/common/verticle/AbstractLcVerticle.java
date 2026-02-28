package com.larpconnect.njall.common.verticle;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Closer;
import com.google.errorprone.annotations.Var;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.proto.Observability;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import jakarta.inject.Provider;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.slf4j.MDC;

abstract class AbstractLcVerticle extends AbstractVerticle {
  private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();
  private static final int SPAN_ID_BYTES = 8;

  private final String channel;
  private final Provider<RandomGenerator> randomProvider;

  protected AbstractLcVerticle(String channel) {
    this(channel, ThreadLocalRandom::current);
  }

  protected AbstractLcVerticle(String channel, Provider<RandomGenerator> randomProvider) {
    this.channel = channel;
    this.randomProvider = randomProvider;
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

              String spanIdStr = HEX.encode(newSpanId);

              try (Closer closer = Closer.create()) {
                if (traceIdStr != null) {
                  closer.register(MDC.putCloseable("trace_id", traceIdStr));
                }
                if (parentSpanIdStr != null) {
                  closer.register(MDC.putCloseable("parent_span_id", parentSpanIdStr));
                }
                closer.register(MDC.putCloseable("span_id", spanIdStr));

                MessageResponse response = handleMessage(newSpanId, message);
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
