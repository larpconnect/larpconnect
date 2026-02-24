package com.larpconnect.njall.init;

import com.google.protobuf.InvalidProtocolBufferException;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

final class ProtoCodecRegistry implements MessageCodec<Message, Message> {
  private static final short VERSION = 0x01;
  private static final String NAMESPACE = "com.larpconnect.njall.proto.";

  @Override
  public void encodeToWire(Buffer buffer, Message message) {
    byte[] bytes = message.toByteArray();
    buffer.appendShort(VERSION);
    buffer.appendInt(bytes.length);
    buffer.appendBytes(bytes);
  }

  @Override
  public Message decodeFromWire(int pos, Buffer buffer) {
    // Read version (2 bytes) - discarding it as per instructions
    // short version = buffer.getShort(pos);
    int currentPos = pos + 2;

    // Read size (4 bytes)
    int size = buffer.getInt(currentPos);
    currentPos += 4;

    // Read bytes
    byte[] bytes = buffer.getBytes(currentPos, currentPos + size);

    try {
      Message message = Message.parseFrom(bytes);

      // Prepend namespace to message_type
      String originalType = message.getMessageType();
      String newType = NAMESPACE + originalType;

      return message.toBuilder().setMessageType(newType).build();
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException("Failed to decode protobuf message", e);
    }
  }

  @Override
  public Message transform(Message message) {
    // If sent locally, we just return the object itself (or a copy if needed, but protobufs are immutable-ish)
    // However, since the prompt specifies logic about modifying the message_type during deserialization,
    // strict interpretation suggests this logic applies "over the wire".
    // For local transport, transform is used. If we want to simulate the same behavior, we could apply it here too.
    // But typically transform is identity for immutable objects.
    // Let's stick to identity for local transport to be efficient, unless requirements say otherwise.
    return message;
  }

  @Override
  public String name() {
    return "proto";
  }

  @Override
  public byte systemCodecID() {
    return -1; // User codec
  }
}
