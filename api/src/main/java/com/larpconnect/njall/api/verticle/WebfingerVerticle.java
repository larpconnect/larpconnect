package com.larpconnect.njall.api.verticle;

import com.google.protobuf.Any;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.MessageReply;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.ProtoDef;
import com.larpconnect.njall.proto.WebfingerResponse;
import io.vertx.core.Promise;
import jakarta.inject.Inject;
import java.util.Optional;

/** Verticle handling Webfinger requests from the /.well-known/webfinger endpoint. */
final class WebfingerVerticle extends AbstractLcVerticle {

  static final String CHANNEL = "http.well-known.webfinger.request";

  @Inject
  WebfingerVerticle(IdGenerator idGenerator) {
    super(CHANNEL, idGenerator);
  }

  @Override
  protected MessageResponse handleMessage(
      byte[] spanId, MessageRequest message, Promise<MessageReply> responsePromise) {
    var resourceOpt =
        message.getParametersList().stream()
            .filter(p -> "resource".equals(p.getKey()) && p.hasStringValue())
            .map(p -> sanitize(p.getStringValue()))
            .reduce((first, second) -> second);

    if (resourceOpt.isEmpty()) {
      // Return a basic empty response rather than fake data if it's missing or invalid
      WebfingerResponse wfResp = WebfingerResponse.newBuilder().build();
      responsePromise.complete(
          MessageReply.newBuilder()
              .setProto(
                  ProtoDef.newBuilder()
                      .setProtobufName("WebfingerResponse")
                      .setMessage(Any.pack(wfResp))
                      .build())
              .build());
      return BasicResponse.CONTINUE;
    }

    // Do not return made-up data. Just echo the subject without fake data.
    var response = WebfingerResponse.newBuilder().setSubject(resourceOpt.get()).build();
    responsePromise.complete(
        MessageReply.newBuilder()
            .setProto(
                ProtoDef.newBuilder()
                    .setProtobufName("WebfingerResponse")
                    .setMessage(Any.pack(response))
                    .build())
            .build());

    return BasicResponse.CONTINUE;
  }

  private String sanitize(String input) {
    return Optional.ofNullable(input).map(i -> i.replaceAll("[^a-zA-Z.:\\-@]", "")).orElse("");
  }
}
