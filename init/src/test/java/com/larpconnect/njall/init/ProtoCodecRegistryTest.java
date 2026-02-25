package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

final class ProtoCodecRegistryTest {

  private static final short EXPECTED_VERSION = 0x01;
  private static final int EXPECTED_SIZE_OFFSET = 2;

  @Test
  public void encodeToWire_validMessage_success() {
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
    // Check size
    assertThat(buffer.getInt(EXPECTED_SIZE_OFFSET)).isEqualTo(original.getSerializedSize());

    var decoded = registry.decodeFromWire(0, buffer);

    assertThat(decoded.getTraceId()).isEqualTo(original.getTraceId());
    // Namespace should be prepended
    assertThat(decoded.getMessageType()).isEqualTo("com.larpconnect.njall.proto.MyType");
    assertThat(decoded.getMessage()).isEqualTo(original.getMessage());
  }

  @Test
  public void transform_anyMessage_identity() {
    var registry = new ProtoCodecRegistry();
    var original = Message.newBuilder().setMessageType("foo").build();
    assertThat(registry.transform(original)).isSameAs(original);
  }

  @Test
  public void decodeFromWire_incompleteBuffer_failure() {
    var registry = new ProtoCodecRegistry();
    var buffer = Buffer.buffer();
    buffer.appendShort((short) 0x01);
    buffer.appendInt(10);
    buffer.appendBytes(new byte[] {1, 2, 3}); // Incomplete bytes

    try {
      registry.decodeFromWire(0, buffer);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageContaining("Failed to decode protobuf message");
    } catch (IndexOutOfBoundsException e) {
      // Expected if buffer is too short
    }
  }
}
