package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

final class ProtoCodecRegistryTest {

  private static final short EXPECTED_VERSION = 0x01;
  private static final int EXPECTED_SIZE_OFFSET = 2;

  @Test
  void encodeToWire_validMessage_success() {
    var registry = new ProtoCodecRegistry();
    var original =
        Message.newBuilder()
            .setTraceId(ByteString.copyFromUtf8("trace-123"))
            .setMessageType("MyType")
            .setMessage(Any.pack(Message.getDefaultInstance()))
            .build();

    var buffer = Buffer.buffer();
    registry.encodeToWire(buffer, original);

    // Check version
    assertThat(buffer.getShort(0)).isEqualTo(EXPECTED_VERSION);
    // Check size matches payload
    // Version (2) + Int (4) + Payload
    assertThat(buffer.getInt(EXPECTED_SIZE_OFFSET)).isEqualTo(original.getSerializedSize());

    // We can decode it back to verify correctness
    var decoded = registry.decodeFromWire(0, buffer);
    assertThat(decoded.getTraceId()).isEqualTo(original.getTraceId());
    // The namespace is prepended during decode
    assertThat(decoded.getMessageType()).isEqualTo("com.larpconnect.njall.proto.MyType");
    assertThat(decoded.getMessage()).isEqualTo(original.getMessage());
  }

  @Test
  void transform_anyMessage_identity() {
    var registry = new ProtoCodecRegistry();
    var original = Message.newBuilder().setMessageType("foo").build();
    assertThat(registry.transform(original)).isSameAs(original);
  }

  @Test
  void decodeFromWire_incompleteBuffer_failure() {
    var registry = new ProtoCodecRegistry();
    var buffer = Buffer.buffer();
    buffer.appendShort((short) 0x01);
    buffer.appendInt(10);
    buffer.appendBytes(new byte[] {1, 2, 3}); // Incomplete bytes

    // This should fail either with IndexOutOfBounds or IllegalArgumentException
    assertThatThrownBy(() -> registry.decodeFromWire(0, buffer))
        .isInstanceOfAny(IndexOutOfBoundsException.class, IllegalArgumentException.class);
  }

  @Test
  void name_returnsProtobuf() {
    assertThat(new ProtoCodecRegistry().name()).isEqualTo("protobuf");
  }

  @Test
  void systemCodecID_returnsNegativeOne() {
    assertThat(new ProtoCodecRegistry().systemCodecID()).isEqualTo((byte) -1);
  }
}
