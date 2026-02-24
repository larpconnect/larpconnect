package com.larpconnect.njall.init;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

public class ProtoCodecRegistryTest {

  @Test
  public void testEncodeDecode() {
    ProtoCodecRegistry registry = new ProtoCodecRegistry();

    Message original = Message.newBuilder()
        .setTraceId(ByteString.copyFromUtf8("trace-123"))
        .setMessageType("MyType")
        .setMessage(Any.pack(Message.getDefaultInstance()))
        .build();

    Buffer buffer = Buffer.buffer();
    registry.encodeToWire(buffer, original);

    // Check version
    assertThat(buffer.getShort(0)).isEqualTo((short) 0x01);
    // Check size
    assertThat(buffer.getInt(2)).isEqualTo(original.getSerializedSize());

    Message decoded = registry.decodeFromWire(0, buffer);

    assertThat(decoded.getTraceId()).isEqualTo(original.getTraceId());
    // Namespace should be prepended
    assertThat(decoded.getMessageType()).isEqualTo("com.larpconnect.njall.proto.MyType");
    assertThat(decoded.getMessage()).isEqualTo(original.getMessage());
  }

  @Test
  public void testTransform() {
    ProtoCodecRegistry registry = new ProtoCodecRegistry();
    Message original = Message.newBuilder().setMessageType("foo").build();
    assertThat(registry.transform(original)).isSameAs(original);
  }
}
