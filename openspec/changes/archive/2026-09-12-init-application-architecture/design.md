## Context

See `proposal.md` for background and motivation. Project Njall is establishing its initial runtime architecture using Java 25 LTS, Apache Pekko Typed (`2.0.0-M4`), Pekko HTTP (`2.0.0-M1`), and Google Guice (`7.0.0`). The system enforces strict Directed Acyclic Graph (DAG) package boundaries, package-to-module parity under `com.larpconnect.njall.*`, and IOSP-Lite behavioral isolation.

## Goals / Non-Goals

**Goals:**
- Activate the initial core pipeline of Gradle subprojects: `:parent`, `:bom`, `:test`, `:common`, `:api`, `:server`, and `:integration`.
- Deliver a functional HTTP server that binds to a configurable port (8080 default) and returns `200 OK` with a blank response body at `GET /`.
- Structure configuration using Typesafe Config (HOCON) with environment variable overrides and a strongly-typed `ServerConfig` record.
- Implement graceful socket unbinding and ActorSystem termination via Pekko's native `CoordinatedShutdown`.
- Establish the spec-first OpenAPI contract (`openapi.yaml`) in `:api`.
- Implement automated verification across both Layer 1 (unit tests in `src/test`) and Layer 2 (Cucumber integration tests in `:integration`).

**Non-Goals:**
- Activating or implementing persistence (`:data`), event schemas (`:events`), or queue brokers (`:queue`) in this change (deferred to subsequent changes).
- Business domain endpoints or database connectivity.

---

## Architectural Diagrams (C4 Models)

### Container Diagram

```
+─────────────────────────────────────────────────────────────────────────────+
|                                CLIENTS / USERS                              |
+───────────────────────────────────────┬─────────────────────────────────────+
                                        │ HTTP Requests (GET /)
                                        v
+─────────────────────────────────────────────────────────────────────────────+
|                     NJALL SERVER CONTAINER (:server)                        |
|                                                                             |
|  +--------------------+                     +----------------------------+  |
|  | Config Layer       |                     | Pekko HTTP Server          |  |
|  | (:common)          |                     | (:server)                  |  |
|  | - reference.conf   |                     | - Port 8080 / Dynamic      |  |
|  | - ServerConfig     |                     | - CoordinatedShutdown      |  |
|  +---------+----------+                     +--------------+-------------+  |
|            │                                               │                |
|            │ configures                                    │ routes         |
|            v                                               v                |
|  +───────────────────────────────────────────────────────────────────────+  |
|  | Routing Component (:api)                                              |  |
|  | - openapi.yaml                                                        |  |
|  | - RootRoute (GET / -> 200 OK blank)                                   |  |
|  +───────────────────────────────────────────────────────────────────────+  |
+─────────────────────────────────────────────────────────────────────────────+
```

### Component Diagram (Module Dependency Flow)

```
+─────────────────────────────────────────────────────────────────────────────+
|                        GRADLE SUBPROJECT TOPOLOGY                           |
|                                                                             |
|                       +─────────────────────+                               |
|                       |       :parent       |                               |
|                       |   (java-platform)   |                               |
|                       +──────────┬──────────+                               |
|                                  │                                          |
|                                  v                                          |
|                       +─────────────────────+                               |
|                       |        :bom         |                               |
|                       |   (java-platform)   |                               |
|                       +──────────┬──────────+                               |
|                                  │                                          |
|                                  v                                          |
|                       +─────────────────────+                               |
|                       |        :test        |                               |
|                       | (testing utilities) |                               |
|                       +──────────┬──────────+                               |
|                                  │                                          |
|                                  v                                          |
|                       +─────────────────────+                               |
|                       |       :common       |                               |
|                       |  (config & records) |                               |
|                       +──────────┬──────────+                               |
|                                  │                                          |
|                                  v                                          |
|                       +─────────────────────+                               |
|                       |        :api         |                               |
|                       |  (OpenAPI & Routes) |                               |
|                       +──────────┬──────────+                               |
|                                  │                                          |
|                     +────────────┴────────────+                             |
|                     │                         │                             |
|                     v                         v                             |
|           +───────────────────+     +───────────────────+                   |
|           |      :server      |     |   :integration    |                   |
|           | (App composition) |     |  (Cucumber tests) |                   |
|           +───────────────────+     +───────────────────+                   |
+─────────────────────────────────────────────────────────────────────────────+
```

---

## Decisions

### Decision 1: Module Separation of Routing vs. Server Binding
- **Choice**: Place route contracts and definitions (`RootRoute`, `DefaultRootRoute`) inside `:api` (`com.larpconnect.njall.api.http`), and place the network socket binding, service lifecycle, and `ServerApp` main entry point inside `:server` (`com.larpconnect.njall.server.http`).
- **Rationale**: Isolates HTTP endpoint definitions from deployment and process lifecycle concerns. Allows `:api` routes to be unit-tested in isolation using `RouteTest` without spinning up actual TCP sockets.
- **Alternatives Considered**: Housing both route definitions and socket binding in `:server` (violates separation of API contract from server host).

### Decision 2: Spec-First Contract via `openapi.yaml` in `:api`
- **Choice**: Define an initial `openapi.yaml` specification in `:api/src/main/resources/openapi.yaml` specifying `GET /` with a `200 OK` response.
- **Rationale**: Strictly complies with `AGENTS.md` spec-first invariant before feature code is authored.
- **Alternatives Considered**: Deferring `openapi.yaml` until multi-endpoint domain APIs exist (risks contract drift).

### Decision 3: Typesafe Config with Strongly-Typed `ServerConfig`
- **Choice**: Supply default settings in `:common/src/main/resources/reference.conf` using HOCON:
  ```hocon
  larpconnect {
    http {
      host = "0.0.0.0"
      host = ${?LARPCONNECT_HTTP_HOST}
      host = ${?HOST}
      port = 8080
      port = ${?LARPCONNECT_HTTP_PORT}
      port = ${?PORT}
    }
  }
  ```
  Map these into a Java 25 record `ServerConfig(String host, int port)` via a pure factory method `ServerConfig.fromConfig(Config)`.
- **Rationale**: Native integration with Pekko, standard environment variable override syntax, and type safety across injection sites.
- **Alternatives Considered**: Raw `System.getenv` lookups in code (untyped, hard to test, lacks default cascades).

### Decision 4: Lifecycle Management via Pekko Native `CoordinatedShutdown`
- **Choice**: Use `CoordinatedShutdown.get(system)` to register unbinding tasks under `PhaseServiceUnbind`, allowing in-flight requests to complete before terminating the ActorSystem. `ServerApp` registers a JVM shutdown hook and awaits `system.whenTerminated()`.
- **Rationale**: Standardized, asynchronous phase-driven shutdown native to Pekko without thread locking or blocking thread joins.
- **Alternatives Considered**: Guava's `AbstractIdleService` or manual `CountDownLatch` synchronization.

### Decision 5: Dynamic Port Allocation for Integration Tests
- **Choice**: Configure integration tests in `:integration` to bind to port 0 (ephemeral dynamic port), retrieving the bound port after startup.
- **Rationale**: Prevents TCP port collisions on port 8080 during local developer builds or parallel CI executions.
- **Alternatives Considered**: Fixed port 8080 across tests (causes flaky test failures when port is already bound).

---

## Risks / Trade-offs

- **[Risk]** Port 8080 might be occupied on the host system during local execution.
  - **Mitigation**: Configurable via `PORT` environment variable and Typesafe Config overrides.
- **[Risk]** JaCoCo coverage thresholds failing on `ServerApp` main method.
  - **Mitigation**: `njall.java-common-conventions.gradle.kts` already includes `**/org/larpconnect/server/ServerApp*` in `jacocoExcludeList`. All other classes maintain >=85% line and >=90% branch coverage.

## Migration Plan

Not applicable; this change introduces the initial application architecture.

## Open Questions

None. All architectural branches were explored and resolved during the grill-me interview.
