package com.larpconnect.njall.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
final class GrpcMessageServiceTest {

  @Test
  void getMessage_returnsGreeting(VertxTestContext testContext) {
    var service = new GrpcMessageService();

    service
        .getMessage(Empty.getDefaultInstance())
        .onComplete(
            testContext.succeeding(
                msg -> {
                  assertThat(msg.getMessageType()).isEqualTo("Greeting");
                  testContext.verify(
                      () -> {
                        var content = msg.getMessage().unpack(StringValue.class);
                        assertThat(content.getValue()).isEqualTo("Hello from gRPC");
                        testContext.completeNow();
                      });
                }));
  }
}
