package com.larpconnect.njall.server;

import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Web server verticle that serves OpenAPI specification. */
public final class WebServerVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(WebServerVerticle.class);
  private static final int PORT = 8080;

  public WebServerVerticle() {
    // Default constructor
  }

  @Override
  public void start(Promise<Void> startPromise) {
    if (getClass().getClassLoader().getResource("openapi.yaml") == null) {
      startPromise.fail("openapi.yaml not found on classpath");
      return;
    }

    OpenAPIContract.from(vertx, "openapi.yaml")
        .onFailure(startPromise::fail)
        .onSuccess(
            contract -> {
              RouterBuilder builder = RouterBuilder.create(vertx, contract);
              builder
                  .getRoute("MessageService_GetMessage")
                  .addHandler(
                      ctx -> {
                        Message message = Message.newBuilder().setMessageType("Greeting").build();
                        try {
                          String json = JsonFormat.printer().print(message);
                          ctx.json(new JsonObject(json));
                        } catch (Exception e) {
                          logger.error("Failed to convert message to JSON", e);
                          ctx.fail(e);
                        }
                      });

              Router router = builder.createRouter();
              vertx
                  .createHttpServer()
                  .requestHandler(router)
                  .listen(PORT)
                  .onSuccess(
                      server -> {
                        logger.info("HTTP server started on port {}", PORT);
                        startPromise.complete();
                      })
                  .onFailure(startPromise::fail);
            });
  }
}
