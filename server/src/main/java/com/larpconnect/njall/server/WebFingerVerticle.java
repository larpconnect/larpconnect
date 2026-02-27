package com.larpconnect.njall.server;

import com.google.inject.name.Named;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Verticle to handle WebFinger requests. */
@Singleton
final class WebFingerVerticle extends AbstractVerticle {

  static final String ADDRESS = "com.larpconnect.njall.server.WebFingerVerticle.ADDRESS";
  private final Logger logger = LoggerFactory.getLogger(WebFingerVerticle.class);
  private final int port;

  @Inject
  WebFingerVerticle(@Named("web.port") int port) {
    this.port = port;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    vertx.eventBus().consumer(ADDRESS, this::handleWebFingerRequest);
    startPromise.complete();
  }

  private void handleWebFingerRequest(Message<JsonObject> message) {
    JsonObject body = message.body();
    String resource = body.getString("resource");
    logger.info("Received WebFinger request for resource: {}", resource);

    JsonObject response =
        new JsonObject().put("resource", resource).put("port", port).put("links", new JsonArray());

    message.reply(response);
  }
}
