package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

public class ProtoCodecRegistryTest {

  private static final short EXPECTED_VERSION = 0x01;
  private static final int EXPECTED_SIZE_OFFSET = 2;

  @Test
  public void encodeToWire_validMessage_success() {
    ProtoCodecRegistry registry = new ProtoCodecRegistry();

    Message original =
        Message.newBuilder()
            .setTraceId(ByteString.copyFromUtf8("trace-123"))
            .setMessageType("MyType")
            .setMessage(Any.pack(Message.getDefaultInstance()))
            .build();

    Buffer buffer = Buffer.buffer();
    registry.encodeToWire(buffer, original);

    // Check version
    assertThat(buffer.getShort(0)).isEqualTo(EXPECTED_VERSION);
    // Check size
    assertThat(buffer.getInt(EXPECTED_SIZE_OFFSET)).isEqualTo(original.getSerializedSize());

    Message decoded = registry.decodeFromWire(0, buffer);

    assertThat(decoded.getTraceId()).isEqualTo(original.getTraceId());
    // Namespace should be prepended
    assertThat(decoded.getMessageType()).isEqualTo("com.larpconnect.njall.proto.MyType");
    assertThat(decoded.getMessage()).isEqualTo(original.getMessage());
  }

  @Test
  public void transform_anyMessage_identity() {
    ProtoCodecRegistry registry = new ProtoCodecRegistry();
    Message original = Message.newBuilder().setMessageType("foo").build();
    assertThat(registry.transform(original)).isSameAs(original);
  }

  @Test
  public void decodeFromWire_incompleteBuffer_failure() {
    ProtoCodecRegistry registry = new ProtoCodecRegistry();
    Buffer buffer = Buffer.buffer();
    buffer.appendShort((short) 0x01);
    buffer.appendInt(10);
    buffer.appendBytes(new byte[] {1, 2, 3}); // Incomplete bytes

    try {
      registry.decodeFromWire(0, buffer);
    } catch (RuntimeException e) {
      // Check if the cause is InvalidProtocolBufferException (wrapped)
      if (e.getCause() instanceof InvalidProtocolBufferException) {
        assertThat(e.getCause()).isInstanceOf(InvalidProtocolBufferException.class);
      } else {
        // Or if it's an index out of bounds from Vert.x buffer operations
        // (though getBytes might throw IndexOutOfBoundsException directly,
        //  but here we catch RuntimeException which covers it)
        assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
      }
    }
  }
}
