package com.larpconnect.njall.api.verticle;

import com.google.protobuf.Message;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.Parameter;
import com.larpconnect.njall.proto.WebfingerResponse;
import io.vertx.core.Promise;
import jakarta.inject.Inject;

/** Verticle handling Webfinger requests from the /.well-known/webfinger endpoint. */
final class WebfingerVerticle extends AbstractLcVerticle {

  static final String CHANNEL = "http.well-known.webfinger.request";

  @Inject
  WebfingerVerticle(IdGenerator idGenerator) {
    super(CHANNEL, idGenerator);
  }

  @Override
  protected MessageResponse handleMessage(
      byte[] spanId, MessageRequest message, Promise<Message> responsePromise) {
    String resource = null;
    var params = message.getParametersList();

    for (Parameter p : params) {
      if ("resource".equals(p.getKey()) && p.hasStringValue()) {
        resource = sanitize(p.getStringValue());
      }
    }

    if (resource == null) {
      // Return a basic empty response rather than fake data if it's missing or invalid
      responsePromise.complete(WebfingerResponse.newBuilder().build());
      return BasicResponse.CONTINUE;
    }

    // Do not return made-up data. Just echo the subject without fake data.
    WebfingerResponse response = WebfingerResponse.newBuilder().setSubject(resource).build();
    responsePromise.complete(response);

    return BasicResponse.CONTINUE;
  }

  private String sanitize(String input) {
    if (input == null) {
      return "";
    }
    // Strip all characters that aren't a-zA-Z, ., :, -, or @
    return input.replaceAll("[^a-zA-Z.:\\-@]", "");
  }
}
