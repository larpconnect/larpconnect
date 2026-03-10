package com.larpconnect.njall.api.verticle;

import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.proto.MessageRequest;
import com.larpconnect.njall.proto.Parameter;
import com.larpconnect.njall.proto.WebfingerResponse;
import io.vertx.core.Promise;
import jakarta.inject.Inject;
import java.util.List;

/** Verticle handling Webfinger requests from the /.well-known/webfinger endpoint. */
final class WebfingerVerticle extends AbstractLcVerticle {

  static final String CHANNEL = "http.well-known.webfinger.request";

  @Inject
  WebfingerVerticle(IdGenerator idGenerator) {
    super(CHANNEL, idGenerator);
  }

  @Override
  protected void handleMessage(
      byte[] spanId, MessageRequest message, Promise<MessageResponse> responsePromise) {
    String resource = null;
    List<Parameter> params = message.getParametersList();

    for (Parameter p : params) {
      if ("resource".equals(p.getKey()) && p.hasStringValue()) {
        resource = p.getStringValue();
      }
    }

    if (resource == null) {
      // Return empty response or basic subject if no resource provided,
      // though typically webfinger requires a resource.
      responsePromise.complete(
          new ReplyResponse(
              WebfingerResponse.newBuilder().setSubject("acct:system@localhost").build()));
      return;
    }

    // Basic logic to construct a webfinger response based on the subject
    WebfingerResponse response = WebfingerResponse.newBuilder().setSubject(resource).build();

    responsePromise.complete(new ReplyResponse(response));
  }
}
