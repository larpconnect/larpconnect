package com.larpconnect.njall.server;

import com.google.inject.name.Named;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WebFingerVerticle extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(WebFingerVerticle.class);
  private final int port;

  @Inject
  WebFingerVerticle(@Named("web.port") int port) {
    this.port = port;
  }

  @Override
  public void start() {
    vertx.eventBus().consumer("webfinger.service", this::handleWebFingerRequest);
  }

  private void handleWebFingerRequest(Message<JsonObject> message) {
    JsonObject body = message.body();
    if (body != null && body.containsKey("resource")) {
      logger.info("Received WebFinger request for resource: {}", body.getString("resource"));
    }

    JsonObject response = new JsonObject().put("port", port).put("resources", new JsonArray());

    message.reply(response);
  }
}
