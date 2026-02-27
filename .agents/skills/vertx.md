# Skill: Vert.x 5 Reactive Development

## Domain Context
This skill extends the core Java development guidelines to handle reactive, non-blocking, and high-performance applications specifically utilizing the Vert.x 5 toolkit. 

## Technical Constraints

1. Vert.x 5 is the core framework. Always utilize the asynchronous and reactive paradigms native to Vert.x unless explicitly operating within a designated blocking context (e.g., using `vertx.executeBlocking()`).

## Specific Guidance

### Core Architectural Principles
 * **Non-Blocking Default:** The primary rule of Vert.x is never to block the Event Loop. CPU-intensive tasks or blocking I/O must be delegated to worker threads or Virtual Threads.
 * **Event-Driven Communication:** Components should communicate primarily via message-passing over the Vert.x EventBus. This guarantees loose coupling and facilitates seamless scaling from single-node to distributed systems.
 * **Virtual Thread Integration:** Vert.x 5 has deep integration with Project Loom. For complex asynchronous orchestration that is hard to read with `Future` composition, prefer deploying Virtual Thread Verticles to maintain straight-line, readable code without sacrificing concurrency.
 * **Security & Resilience:** Assume network unreliability for distributed Vert.x applications. Always implement timeouts on asynchronous operations, use circuit breakers for external service calls, and ensure clustered EventBus communication is secured via TLS.

### Initialization

Initialization of the application Vert.x should be delayed until after the Guice injector has been fully created. 

### Context7 Directives

Always query `Context7` for the most up-to-date Vert.x 5 API documentation, paying special attention to shifts from Vert.x 4 (e.g., deeper Loom integration, SQL client changes, and web routing enhancements).  Load sub-modules as appropriate (e.g., Vert.x Web when working with `vertx-web`).

### Implementation Directives

 * **Verticle Design:** Verticles should be small, focused, and adhere to the Single Responsibility Principle. They must hold their own local state and avoid shared mutable data structures with other Verticles.
 * **State Management:** If state must be shared, utilize Vert.x Local/Cluster Maps or asynchronous data stores. Do not use standard `java.util.concurrent` locks or `synchronized` blocks within an Event Loop context.
 * **Asynchronous Composition:** When not using Virtual Threads, avoid "callback hell" by strictly using `Future.map()`, `Future.compose()`, and `Future.all()` / `Future.any()` for coordinating multiple asynchronous operations. Do not wrap Vert.x APIs in `java.util.concurrent.CompletableFuture` unless interoperating with an external library.
 * **Immutability on the EventBus:** Messages passed over the EventBus must be immutable. Use Java Records or protobuf for custom message codecs and data transfer objects (DTOs).
 * **Routing & Web:** When using Vert.x Web, modularize routes using sub-routers. Always implement global error handlers to catch and format exceptions escaping from route handlers.
 * **Database Access:** Utilize the Vert.x Reactive SQL Clients (e.g., `vertx-pg-client`). JDBC should only be used if wrapped in a worker tier or Virtual Thread, and only if a reactive driver does not exist for the target datastore.
 * **Prefer Vert.x Libraries:** If there is a vert.x solution for something (e.g., OpenAPI integration) that should be used in preference to rolling our own. 

### Vert.x Style

* Custom EventBus message codecs should be named `<RecordName>MessageCodec` and bound during application initialization.
* Verticle classes should be suffixed with `Verticle` (e.g., `HttpServerVerticle`, `DataProcessingVerticle`).
* Testing must heavily leverage `vertx-junit5`. Tests requiring asynchronous resolution should use `VertxTestContext` to await completion, preventing premature test termination.

### Contextual Logging

* Standard MDC (Mapped Diagnostic Context) often fails across thread context switches in asynchronous environments. Ensure trace IDs are explicitly passed or use Vert.x's specific context-aware logging extensions when tracing across EventBus boundaries.

### Vert.x Libraries

The following ecosystem libraries should be used for their respective domains:
* **Vert.x Core & Extensions:**
  * `vertx-core`
  * `vertx-web`
  * `vertx-config`
  * `vertx-health-check`
* **System Design:** `vertx-service-discovery`
* **Data Access:** `vertx-pg-client`
* **Authentication and Authorization:** `vertx-auth-common`
* **Testing:** `vertx-junit5`, `vertx-web-client`
* **Data Serialization:** Use Jackson (integrated into Vert.x) combined with Java Records or protobuf (included in `:proto`) via the protobuf handler we've registered.

## Antipatterns

| Instead of                              | Do this                                      | Justification                                                |
| --------------------------------------- | -------------------------------------------- | ------------------------------------------------------------ |
| `Thread.sleep(...)`                     | `vertx.setTimer(..., handler)`               | `Thread.sleep` blocks the Event Loop, halting the system.    |
| Nested Callbacks (`handler(res -> {})`) | `Future.compose()` or Virtual Threads        | Prevents callback hell and maintains code maintainability.   |
| Standard JDBC drivers                   | Vert.x Reactive SQL Clients                  | Reactive clients do not block threads while awaiting I/O.    |
| Shared `HashMap` between Verticles      | EventBus or `SharedData` maps                | Enforces thread safety without blocking synchronization.     |
| `CompletableFuture.supplyAsync()`       | `vertx.executeBlocking()`                    | Keeps blocking tasks managed within Vert.x worker pools.     |
| Manual JSON parsing/building            | `JsonObject.mapFrom()` with Java Records     | Reduces boilerplate and leverages built-in Jackson mapping.  |
| Try/Catch around async operations       | `.onFailure(...)` on the `Future`            | Standard try/catch blocks do not catch async exceptions.     |

