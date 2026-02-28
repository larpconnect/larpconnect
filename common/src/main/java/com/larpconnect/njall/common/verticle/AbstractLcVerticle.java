package com.larpconnect.njall.common.verticle;

import com.google.common.io.BaseEncoding;
import com.google.errorprone.annotations.Var;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.proto.Observability;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import java.security.SecureRandom;
import java.util.function.Consumer;
import org.slf4j.MDC;

abstract class AbstractLcVerticle extends AbstractVerticle {
  private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();
  private static final int SPAN_ID_BYTES = 8;

  private final String channel;
  private final Consumer<byte[]> randomBytesGenerator;

  private static final SecureRandom RANDOM = new SecureRandom();

  protected AbstractLcVerticle(String channel) {
    this(
        channel,
        bytes -> {
          RANDOM.nextBytes(bytes);
        });
  }

  protected AbstractLcVerticle(String channel, Consumer<byte[]> randomBytesGenerator) {
    this.channel = channel;
    this.randomBytesGenerator = randomBytesGenerator;
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
              randomBytesGenerator.accept(newSpanId);

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

              if (traceIdStr != null) {
                MDC.put("trace_id", traceIdStr);
              }
              if (parentSpanIdStr != null) {
                MDC.put("parent_span_id", parentSpanIdStr);
              }
              MDC.put("span_id", spanIdStr);

              try {
                handleMessage(newSpanId, message);
              } finally {
                MDC.remove("trace_id");
                MDC.remove("parent_span_id");
                MDC.remove("span_id");
              }
            });

    startPromise.complete();
  }

  protected abstract void handleMessage(byte[] spanId, Message message);
}
