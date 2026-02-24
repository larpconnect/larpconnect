package com.larpconnect.init;

import com.larpconnect.init.MessageProto.Message;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default implementation of {@link ProtoCodecRegistry}. */
final class DefaultProtoCodecRegistry implements ProtoCodecRegistry {
  private final Logger logger = LoggerFactory.getLogger(DefaultProtoCodecRegistry.class);

  DefaultProtoCodecRegistry() {}

  @Override
  public void register(Vertx vertx) {
    logger.info("Registering Protobuf codecs");
    vertx
        .eventBus()
        .registerDefaultCodec(
            Message.class,
            new MessageCodec<Message, Message>() {
              @Override
              public void encodeToWire(Buffer buffer, Message message) {
                byte[] bytes = message.toByteArray();
                buffer.appendInt(bytes.length);
                buffer.appendBytes(bytes);
              }

              @Override
              @SuppressWarnings({"checkstyle:IllegalCatch"})
              public Message decodeFromWire(int pos, Buffer buffer) {
                int length = buffer.getInt(pos);
                // 4 is the length of the integer indicating the size of the message
                final int lengthSize = 4;
                byte[] bytes = buffer.getBytes(pos + lengthSize, pos + lengthSize + length);
                try {
                  return Message.parseFrom(bytes);
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                  throw new RuntimeException(e);
                }
              }

              @Override
              public Message transform(Message message) {
                return message;
              }

              @Override
              public String name() {
                return Message.class.getName();
              }

              @Override
              public byte systemCodecID() {
                return -1;
              }
            });
  }
}
