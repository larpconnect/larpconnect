package com.larpconnect.njall.server.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.proto.VertxMessageServiceGrpc;
import io.vertx.core.Future;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
final class GrpcMessageService extends VertxMessageServiceGrpc.MessageServiceVertxImplBase {

  @Inject
  GrpcMessageService() {}

  @Override
  public Future<Message> getMessage(Empty request) {
    return Future.succeededFuture(
        Message.newBuilder()
            .setMessageType("Greeting")
            .setMessage(Any.pack(StringValue.of("Hello from gRPC")))
            .build());
  }
}
