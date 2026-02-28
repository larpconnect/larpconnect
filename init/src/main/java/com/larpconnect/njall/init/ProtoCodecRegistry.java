package com.larpconnect.njall.init;

import static com.larpconnect.njall.common.annotations.ContractTag.IDEMPOTENT;
import static com.larpconnect.njall.common.annotations.ContractTag.PURE;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.InvalidProtocolBufferException;
import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

@Immutable
final class ProtoCodecRegistry implements MessageCodec<Message, Message> {
  private static final short VERSION = 0x01;
  private static final int INT_SIZE = 4;
  private static final String NAMESPACE = "com.larpconnect.njall.proto.";

  ProtoCodecRegistry() {}

  @Override
  @AiContract(
      ensure = "buffer \\text{ appended with } VERSION, bytes.length, bytes",
      implementationHint = "Writes protocol version, payload size, and payload bytes.")
  public void encodeToWire(Buffer buffer, Message message) {
    var bytes = message.toByteArray();
    buffer.appendShort(VERSION);
    buffer.appendInt(bytes.length);
    buffer.appendBytes(bytes);
  }

  @Override
  @AiContract(
      ensure = "!$res.hasProto() || $res.proto.protobufName \\text{ starts with } NAMESPACE",
      implementationHint =
          "Reads size, parses message, and updates message type if using proto payload.")
  public Message decodeFromWire(int pos, Buffer buffer) {
    var currentPos = pos + 2;

    var size = buffer.getInt(currentPos);
    currentPos += INT_SIZE;

    var bytes = buffer.getBytes(currentPos, currentPos + size);

    try {
      var message = Message.parseFrom(bytes);

      if (message.hasProto()) {
        var originalType = message.getProto().getProtobufName();
        var newType = NAMESPACE + originalType;
        return message.toBuilder()
            .setProto(message.getProto().toBuilder().setProtobufName(newType))
            .build();
      }
      return message;
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException("Failed to decode protobuf message", e);
    }
  }

  @Override
  @AiContract(
      ensure = "$res == message",
      tags = {PURE, IDEMPOTENT},
      implementationHint = "Identity transformation for local event bus delivery.")
  public Message transform(Message message) {
    return message;
  }

  @Override
  @AiContract(
      ensure = "$res.equals(\"protobuf\")",
      tags = PURE,
      implementationHint = "Returns the codec name.")
  public String name() {
    return "protobuf";
  }

  @Override
  @AiContract(
      ensure = "$res == -1",
      tags = PURE,
      implementationHint = "Returns -1 indicating a user-defined codec.")
  public byte systemCodecID() {
    return -1;
  }
}
