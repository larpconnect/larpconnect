with open("server/src/main/java/com/larpconnect/njall/server/WebServerVerticle.java", "r") as f:
    content = f.read()


new_handler = """                        var builder = RouterBuilder.create(vertx, contract);
                        builder.getRoute("getMessage").addHandler(this::handleGetMessage);

                        var hc = HealthCheckHandler.create(vertx);
                        builder.getRoute("healthz").addHandler(hc);

                        builder.getRoute("webfinger").addHandler(this::handleWebfinger);

                        var router = builder.createRouter();
                        vertx
                            .createHttpServer()
                            .requestHandler(router)
                            .listen(port)
                            .onSuccess(
                                server -> {
                                  this.server = server;
                                  int actualPort = server.actualPort();
                                  portListener.ifPresent(listener -> listener.accept(actualPort));
                                  logger.info("HTTP server started on port {}", actualPort);
                                  startPromise.complete();
                                })
                            .onFailure(startPromise::fail);
                      });
            });
  }

  // Package-private for testing
  @AiContract(
      ensure = "ctx.response() \\\\text{ contains JSON Greeting}",
      implementationHint = "Serializes a Greeting message to JSON and writes it to the response")
  void handleGetMessage(RoutingContext ctx) {
    var message =
        MessageRequest.newBuilder()
            .setProto(
                com.larpconnect.njall.proto.ProtoDef.newBuilder()
                    .setProtobufName("Greeting")
                    .build())
            .build();
    try {
      var json = serializer.print(message);
      ctx.response().putHeader("content-type", "application/json").end(json);
    } catch (RuntimeException | IOException e) {
      logger.error("Failed to convert message to JSON", e);
      ctx.fail(e);
    }
  }

  @AiContract(
      ensure = "ctx.response() \\\\text{ contains JSON Webfinger response}",
      implementationHint = "Returns a valid JSON response for a webfinger query")
  void handleWebfinger(RoutingContext ctx) {
    var response = new JsonObject().put("subject", "acct:system@localhost");
    ctx.json(response);
  }
}
"""

import re
content = re.sub(r'                        var builder = RouterBuilder.create\(vertx, contract\);[\s\S]*\}\n', new_handler, content)

with open("server/src/main/java/com/larpconnect/njall/server/WebServerVerticle.java", "w") as f:
    f.write(content)
