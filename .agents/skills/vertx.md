# Skill: Vert.x 5.x Framework

## Domain Context
This skill handles working with the Vert.x 5.x reactive framework.

## Technical Constraints

Vert.x 5 is the primary reactive framework.

* Always use Vert.x 5+
* Applications are composed of Verticles deployed on EventLoops
* Favor the Future-based API over callbacks
* Avoid blocking the Event Loop at all costs

## Specific guidance

The following best practices are followed:

* **Verticle Implementation**: Extend `io.vertx.core.AbstractVerticle` and override `start()` to
  return a `Future`. This aligns with the future-based model.
* **Asynchronous Coordination**: Use `io.vertx.core.Future` composition methods (`compose`, `map`,
  `recover`) instead of nesting callbacks.
* **Event Bus**: Use the Event Bus for loose coupling between components. Define message codecs for
  custom types.
* **Blocking Code**: Offload blocking operations (JDBC, file I/O) to Worker Verticles or use
  `vertx.executeBlocking`.
* **Configuration**: Use `ConfigRetriever` to load configuration from multiple sources (env, file,
  Kubernetes).
* **Dependency Injection**: Combine Vert.x with Guice. Verticles should be created by a
  `VerticleFactory` or injected directly.
* **Testing**: Use `VertxTestContext` (from `vertx-junit5`) for asynchronous testing.

### Standard Pattern

The standard pattern for a Verticle is to extend `AbstractVerticle` and return a `Future` from
`start`:

```java
package com.example.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;

public class HttpServerVerticle extends AbstractVerticle {

  @Override
  public Future<?> start() {
    HttpServer server = vertx.createHttpServer();

    server.requestHandler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x 5!");
    });

    return server.listen(8080).onSuccess(s -> {
      System.out.println("HTTP server started on port " + s.actualPort());
    });
  }
}
```

### Common Patterns

| Pattern | Description | Usage |
| :--- | :--- | :--- |
| **Standard Verticle** | Non-blocking, single-threaded event loop. | HTTP servers, reactive logic. |
| **Worker Verticle** | Thread-pool based, allows blocking. | DB access (JDBC), heavy computation. |
| **EventBus RPC** | Point-to-point async Request-Response. | Internal service calls. |
| **EventBus Pub-Sub** | One-to-many broadcast. | Notifications, cache invalidation. |
| **Api Gateway** | Single entry point routing to services. | Exposing microservices. |
| **Sidecar** | Helper process attached to main app. | Logging, metrics, proxying. |
