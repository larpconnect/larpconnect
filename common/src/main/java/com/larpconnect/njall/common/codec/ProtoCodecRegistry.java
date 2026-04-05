package com.larpconnect.njall.common.codec;

import static com.larpconnect.njall.common.annotations.ContractTag.IDEMPOTENT;
import static com.larpconnect.njall.common.annotations.ContractTag.PURE;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.InvalidProtocolBufferException;
import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.annotations.BuildWith;
import com.larpconnect.njall.proto.MessageRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;

/**
 * A message codec for transmitting Protocol Buffer {@link MessageRequest} objects over the Vert.x
 * event bus.
 *
 * <p>This codec is designed to optimize communication within the system. For local delivery (within
 * the same JVM), it relies on the immutability of Protocol Buffers and performs an identity
 * transformation, bypassing serialization overhead. For clustered or remote delivery, it serializes
 * using protobuf rather than JSON to minimize payload size and reduce bandwidth consumption.
 */
@Immutable
@BuildWith(CodecModule.class)
final class ProtoCodecRegistry implements ProtoCodec {
  private static final short VERSION = 0x01;
  private static final int INT_SIZE = 4;
  private static final String NAMESPACE = "com.larpconnect.njall.proto.";

  ProtoCodecRegistry() {}

  @Override
  @AiContract(
      ensure = "buffer \\text{ appended with } VERSION, bytes.length, bytes",
      implementationHint = "Writes protocol version, payload size, and payload bytes.")
  public void encodeToWire(Buffer buffer, MessageRequest message) {
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
  public MessageRequest decodeFromWire(int pos, Buffer buffer) {
    var currentPos = pos + 2;

    var size = buffer.getInt(currentPos);
    currentPos += INT_SIZE;

    try {
      var message =
          MessageRequest.parseFrom(
              BufferImpl.class.cast(buffer).byteBuf().nioBuffer(currentPos, size));

      if (message.hasProto()) {
        var typeUrl = message.getProto().getMessage().getTypeUrl();
        // Only shortname types in our namespace to save space over the wire.
        if (typeUrl.startsWith(NAMESPACE)) {
          var shortName = typeUrl.substring(NAMESPACE.length());
          return message.toBuilder()
              .setProto(message.getProto().toBuilder().setProtobufName(shortName).build())
              .build();
        }
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
  public MessageRequest transform(MessageRequest message) {
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
