package com.larpconnect.njall.server;

import com.google.protobuf.Empty;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.proto.MessageServiceGrpc;
import io.grpc.stub.StreamObserver;

final class GrpcMessageService extends MessageServiceGrpc.MessageServiceImplBase {
  GrpcMessageService() {}

  @Override
  public void getMessage(Empty request, StreamObserver<Message> responseObserver) {
    Message response = Message.newBuilder().setMessageType("Hello World").build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
