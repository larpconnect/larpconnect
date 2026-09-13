## Context

See `proposal.md` for background and motivation. Project Njall requires an administrative health check probe at `/api/admin/v1/health` that assesses whether the Apache Pekko runtime is up and healthy. The health check mechanism must be extensible, so future subsystems (e.g. database, queue) can register indicators without altering core routing logic. The HTTP server must also cleanly support multiple distinct base paths (`/` and `/api/admin`).

ADR `0001: Pekko HTTP Runtime Architecture and Multimodule Layout` is in force and accepted. This design adheres strictly to the module DAG, package-to-module parity, and Guice dependency injection rules established in ADR 0001.

## Goals / Non-Goals

**Goals:**
- Integrate `io.dropwizard.metrics:metrics-healthchecks` in `:common` using Guice `Multibinder<HealthCheck>` to centralize health check aggregation.
- Implement `PekkoHealthCheck` assessing `ActorSystem` termination, coordinated shutdown state, and dispatcher health.
- Wrap health check execution in a Pekko Typed `HealthCheckActor` using an active ask pattern with a timeout.
- Implement `AdminRoute` for base path `/api/admin` with endpoint `GET /api/admin/v1/health` returning `200 OK` (empty body) when healthy and `500 Internal Server Error` (empty body) when unhealthy or timed out.
- Support clean route aggregation for multiple base paths (`/` and `/api/admin`) in `:api` and `:server`.
- Provide automated verification via unit tests (using `BehaviorTestKit` and route tests) and Cucumber integration tests.

**Non-Goals:**
- Health checks for database (`:data`) or message queue (`:queue`)—deferred until those modules are active.
- Detailed JSON diagnostic payloads or administrative authentication/authorization (endpoint is public for this change).
- Metrics collection or reporting beyond the health check status.

---

## Architectural Diagrams (C4 Models)

### Container Diagram

```
+─────────────────────────────────────────────────────────────────────────────+
|                                CLIENTS / USERS                              |
+───────────────────────────────────────┬─────────────────────────────────────+
                                        │ HTTP Requests
                                        v
+─────────────────────────────────────────────────────────────────────────────+
|                     NJALL SERVER CONTAINER (:server)                        |
|                                                                             |
|  +--------------------+                     +----------------------------+  |
|  | Config & Health    |                     | Pekko HTTP Server          |  |
|  | (:common)          |                     | (:server)                  |  |
|  | - HealthCheckReg   |                     | - Port 8080 / Dynamic      |  |
|  | - Multibinder      |                     | - CoordinatedShutdown      |  |
|  +---------+----------+                     +--------------+-------------+  |
|            │                                               │                |
|            │ supplies registry                             │ binds          |
|            v                                               v                |
|  +───────────────────────────────────────────────────────────────────────+  |
|  | Routing & Actor Layer (:api)                                          |  |
|  | - RootRoute:  GET /                                                   |  |
|  | - AdminRoute: GET /api/admin/v1/health                                |  |
|  | - HealthCheckActor (Pekko Typed)                                      |  |
|  +───────────────────────────────────────────────────────────────────────+  |
+─────────────────────────────────────────────────────────────────────────────+
```

### Component Diagram (API & Health Architecture)

```
+─────────────────────────────────────────────────────────────────────────────+
|  API COMPONENT (:api)                                                       |
|                                                                             |
|   +-------------------+              ask timeout                            |
|   | AdminRoute        |──────────────────────────────────────────┐          |
|   | (/api/admin)      |                                          │          |
|   +---------+---------+                                          ▼          |
|             │                                            +---------------+  |
|             │ ask CheckHealth                            | 500 Timeout   |  |
|             v                                            +---------------+  |
|   +-------------------+       execute()       +-------------------------+   |
|   | HealthCheckActor  |─────────────────────> | HealthCheckRegistry     |   |
|   | (Pekko Typed)     |                       | (:common)               |   |
|   +---------+---------+                       +------------+------------+   |
|             │                                              │                |
|             │ reply Healthy / Unhealthy                    │ evaluates      |
|             v                                              v                |
|   +-------------------+                       +-------------------------+   |
|   | 200 OK / 500 Err  |                       | PekkoHealthCheck        |   |
|   +-------------------+                       +-------------------------+   |
+─────────────────────────────────────────────────────────────────────────────+
```

---

## Decisions

### Decision 1: Dropwizard `metrics-healthchecks` with Guice `Multibinder` in `:common`
- **Choice**: Add `io.dropwizard.metrics:metrics-healthchecks` (version `4.2.30`) to version catalog and constraint list in `parent`. In `:common`, create a `HealthModule` that declares `Multibinder.newSetBinder(binder(), HealthCheck.class)` and provides a `@Singleton HealthCheckRegistry`.
- **Rationale**: Decouples health check producers from consumers. Modules (like `:api`, and future `:data` or `:queue`) can contribute their own `HealthCheck` implementations to the multibinder without circular dependencies or central registry modifications.
- **Alternatives Considered**: Rolling a custom health check interface (reinvents Dropwizard's battle-tested abstraction); Hardcoding health checks in `:api` (violates modularity).

### Decision 2: Active Ask Pattern with `HealthCheckActor` in `:api`
- **Choice**: Implement a stateless Pekko Typed `HealthCheckActor` that processes `HealthCheckCommand.CheckHealth(replyTo)`. When queried, it runs `registry.runHealthChecks()` and replies with `Healthy` (if all passed) or `Unhealthy(reason)` (if any failed). `AdminRoute` queries this actor via `AskPattern.ask(...)` with a 2-second timeout.
- **Rationale**: An active ask through the actor verifies that Pekko's scheduler, dispatcher, and mailbox processing loop are actively functional. If Pekko is deadlocked or frozen, the ask future times out and safely yields HTTP 500.
- **Alternatives Considered**: Directly calling `registry.runHealthChecks()` on the HTTP request thread (bypasses the actor system, failing to test actor mailbox and dispatcher responsiveness).

### Decision 3: Modular Base Path Architecture
- **Choice**: Introduce a composite router pattern in `:api`. `RootRoute` continues to handle `/`, while `AdminRoute` handles base path `/api/admin` with sub-path `/v1/health`. `ApiModule` binds an aggregated `RootRoute` (or route provider) that concatenates all registered base paths using `Directives.concat(...)`.
- **Rationale**: Keeps `/` isolated from `/api/admin` and prepares the server for additional base paths (e.g. `/api/v1`) without modifying the server startup service.
- **Alternatives Considered**: Monolithic route file with all endpoints in one class (violates single responsibility and makes routes unwieldy as endpoints grow).

### Decision 4: Strict HTTP Status Code Mapping with Empty Payload
- **Choice**: The `/api/admin/v1/health` endpoint returns `200 OK` with an empty response body when healthy, and `500 Internal Server Error` with an empty response body when unhealthy or on ask timeout.
- **Rationale**: Conforms directly to user requirements for lightweight health probes suitable for load balancers. Diagnostic details are logged via SLF4J rather than exposed on the public body.
- **Alternatives Considered**: Returning JSON `{ "status": "UP" }` (rejected per user request for no substantive payload at this stage).

---

## Risks / Trade-offs

- **[Risk]** The health check actor ask might time out under extreme CPU load, causing false 500 errors.
  - **Mitigation**: Use a generous timeout (2 seconds) for the health check ask, which is sufficient for non-blocking in-memory checks.
- **[Risk]** `PekkoHealthCheck` might report healthy if the ActorSystem has started shutting down but hasn't entered `CoordinatedShutdown` yet.
  - **Mitigation**: Check both `CoordinatedShutdown.get(system).shutdownReason().isPresent()` and `system.whenTerminated().toCompletableFuture().isDone()`.

## Migration Plan

Not applicable; this change introduces a new endpoint and does not alter existing endpoints or database schemas.

## Open Questions

None. All architectural decisions (Dropwizard HealthCheck, Guice multibinder in `:common`, actor ask pattern in `:api`, base path routing) were resolved during exploration.
