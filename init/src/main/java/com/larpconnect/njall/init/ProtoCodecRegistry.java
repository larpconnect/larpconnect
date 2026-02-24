package com.larpconnect.njall.init;

import com.google.protobuf.InvalidProtocolBufferException;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

final class ProtoCodecRegistry implements MessageCodec<Message, Message> {
  private static final short VERSION = 0x01;
  private static final int INT_SIZE = 4;
  private static final String NAMESPACE = "com.larpconnect.njall.proto.";

  ProtoCodecRegistry() {}

  @Override
  public void encodeToWire(Buffer buffer, Message message) {
    byte[] bytes = message.toByteArray();
    buffer.appendShort(VERSION);
    buffer.appendInt(bytes.length);
    buffer.appendBytes(bytes);
  }

  @Override
  public Message decodeFromWire(int pos, Buffer buffer) {
    int currentPos = pos + 2;

    int size = buffer.getInt(currentPos);
    currentPos += INT_SIZE;

    byte[] bytes = buffer.getBytes(currentPos, currentPos + size);

    try {
      Message message = Message.parseFrom(bytes);

      String originalType = message.getMessageType();
      String newType = NAMESPACE + originalType;

      return message.toBuilder().setMessageType(newType).build();
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("Failed to decode protobuf message", e);
    }
  }

  @Override
  public Message transform(Message message) {
    return message;
  }

  @Override
  public String name() {
    return "protobuf";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
