package com.larpconnect.njall.api.verticle;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Closer;
import com.google.protobuf.ByteString;
import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.MessageReply;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.Observability;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import jakarta.inject.Provider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

abstract class AbstractLcVerticle extends AbstractVerticle {
  private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();
  private static final int SPAN_ID_BYTES = 8;
  private static final int TRACE_ID_BYTES = 16;
  private static final byte DEFAULT_SPAN_ID_BYTE = 0x11;
  private static final ByteString DEFAULT_SPAN_ID =
      ByteString.copyFrom(
          new byte[] {
            DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE,
            DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE, DEFAULT_SPAN_ID_BYTE
          });

  private static final ThreadLocal<ByteBuffer> TRACE_ID_BUFFER =
      ThreadLocal.withInitial(() -> ByteBuffer.allocate(TRACE_ID_BYTES));

  private final String channel;
  private final Provider<RandomGenerator> randomProvider;
  private final IdGenerator idGenerator;
  private final Logger log = LoggerFactory.getLogger(getClass());

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
  @AiContract(
      require = {"$startPromise \\neq \\bot$"},
      ensure = {},
      implementationHint =
          "Registers the verticle to handle messages on the configured event bus "
              + "channel, adding tracing headers if absent.")
  public void start(Promise<Void> startPromise) {
    vertx.eventBus().<MessageRequest>consumer(channel, this::processMessage);

    startPromise.complete();
  }

  private void processMessage(Message<MessageRequest> msg) {
    MessageRequest message = msg.body();

    byte[] newSpanId = new byte[SPAN_ID_BYTES];
    randomProvider.get().nextBytes(newSpanId);

    MessageRequest finalMessage = ensureObservability(message);
    String traceIdStr = HEX.encode(finalMessage.getTraceparent().getTraceId().toByteArray());
    String parentSpanIdStr = HEX.encode(finalMessage.getTraceparent().getSpanId().toByteArray());
    String spanIdStr = HEX.encode(newSpanId);
    String serverStr = finalMessage.getServer();

    try (Closer closer = Closer.create()) {
      closer.register(MDC.putCloseable("trace_id", traceIdStr));
      closer.register(MDC.putCloseable("parent_span_id", parentSpanIdStr));
      closer.register(MDC.putCloseable("span_id", spanIdStr));
      if (!serverStr.isEmpty()) {
        closer.register(MDC.putCloseable("server", serverStr));
      }

      Promise<MessageReply> responsePromise = Promise.promise();
      responsePromise.future().onComplete(ar -> completeResponse(ar, msg));

      MessageResponse response = handleMessage(newSpanId, finalMessage, responsePromise);
      handleResponse(response);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (RuntimeException e) {
      log.error("Error handling message on channel: {}", channel, e);
      msg.fail(-1, "Internal Error");
    }
  }

  private void completeResponse(AsyncResult<MessageReply> ar, Message<MessageRequest> msg) {
    if (ar.succeeded()) {
      MessageReply reply = ar.result();
      String server = msg.body().getServer();
      if (!server.isEmpty() && reply.getServer().isEmpty()) {
        reply = reply.toBuilder().setServer(server).build();
      }
      msg.reply(reply);
    } else {
      log.error("Error handling message on channel: {}", channel, ar.cause());
      msg.fail(-1, "Internal Error");
    }
  }

  private void handleResponse(MessageResponse response) {
    if (response == BasicResponse.SHUTDOWN) {
      vertx.eventBus().consumer(channel).unregister();
    }
  }

  private MessageRequest ensureObservability(MessageRequest message) {
    Observability.Builder obsBuilder =
        message.hasTraceparent()
            ? message.getTraceparent().toBuilder()
            : Observability.newBuilder();

    if (obsBuilder.getTraceId().isEmpty()) {
      UUID uuid = idGenerator.generate();
      ByteBuffer bb = TRACE_ID_BUFFER.get();
      bb.clear();

      bb.putLong(uuid.getMostSignificantBits());
      bb.putLong(uuid.getLeastSignificantBits());
      bb.flip();
      obsBuilder.setTraceId(ByteString.copyFrom(bb));
    }

    if (obsBuilder.getSpanId().isEmpty()) {
      obsBuilder.setSpanId(DEFAULT_SPAN_ID);
    }

    return message.toBuilder().setTraceparent(obsBuilder.build()).build();
  }

  @AiContract(
      require = {"$spanId \\neq \\bot$", "$message \\neq \\bot$", "$responsePromise \\neq \\bot$"},
      ensure = {"$res \\neq \\bot$"},
      implementationHint =
          "Processes an incoming EventBus message and returns a MessageResponse "
              + "flow control directive.")
  protected abstract MessageResponse handleMessage(
      byte[] spanId, MessageRequest message, Promise<MessageReply> responsePromise);
}
