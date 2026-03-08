package com.larpconnect.njall.server;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.protobuf.ByteString;
import com.larpconnect.njall.common.id.IdGenerator;
import com.larpconnect.njall.common.verticle.AbstractLcVerticle;
import com.larpconnect.njall.common.verticle.MessageResponse;
import com.larpconnect.njall.common.verticle.ReplyResponse;
import com.larpconnect.njall.proto.Message;
import com.larpconnect.njall.proto.MimeType;
import com.larpconnect.njall.proto.Parameter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

final class WebfingerVerticle extends AbstractLcVerticle {

  @Inject
  public WebfingerVerticle(IdGenerator idGenerator) {
    super("webfinger.request", idGenerator);
  }

  @Override
  protected MessageResponse handleMessage(byte[] spanId, Message message) {
    List<Parameter> parameters = message.getParameterList();
    Optional<String> resourceParam =
        parameters.stream()
            .filter(p -> "resource".equals(p.getKey()))
            .map(Parameter::getValue)
            .findFirst();

    if (resourceParam.isEmpty()) {
      return new ReplyResponse(
          Message.newBuilder()
              .setMime(
                  MimeType.newBuilder()
                      .setType("application")
                      .setSubtype("json")
                      .setContent(
                          ByteString.copyFrom(
                              "{\"error\":\"missing resource parameter\"}", StandardCharsets.UTF_8))
                      .build())
              .build());
    }

    String resource = resourceParam.get();

    JsonObject response =
        new JsonObject()
            .put("subject", resource)
            .put(
                "links",
                new JsonArray()
                    .add(
                        new JsonObject()
                            .put("rel", "self")
                            .put("type", "application/activity+json")
                            .put(
                                "href",
                                "https://localhost/users/"
                                    + Iterables.get(
                                        Splitter.on('@').split(resource.replace("acct:", "")),
                                        0))));

    return new ReplyResponse(
        Message.newBuilder()
            .setMime(
                MimeType.newBuilder()
                    .setType("application")
                    .setSubtype("jrd+json")
                    .setContent(ByteString.copyFrom(response.encode(), StandardCharsets.UTF_8))
                    .build())
            .build());
  }
}
