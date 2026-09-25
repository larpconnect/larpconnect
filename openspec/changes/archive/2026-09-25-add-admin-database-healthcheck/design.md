## Context

In **Njall**, runtime health probing is hosted by the **Server** application at path `/api/admin/v1/health` and managed by the **Admin verticle**. Under [ADR 0002](../../../../adr/0002-dropwizard-healthcheck-actor-pattern.md), health monitoring follows a decoupled pattern where subsystem modules contribute Dropwizard `HealthCheck` implementations to a central Guice `Multibinder<HealthCheck>` provided by `:common`, while `:api` evaluates them via an active **Actor** query to `HealthCheckActor`.

Currently, the only registered probe is `PekkoHealthCheck` in the **API plane**. If the PostgreSQL database cluster crashes, network connectivity is severed, or the `njall_admin` connection pool is exhausted, the `/api/admin/v1/health` endpoint continues returning HTTP 200 OK. To resolve this blind spot without inducing connection pool saturation or excessive query load during frequent health polling, this change introduces an administrative database health check in the **Data plane** backed by Caffeine caching.

All architectural decisions adhere to existing in-force ADRs:
- [ADR 0002](../../../../adr/0002-dropwizard-healthcheck-actor-pattern.md): Dropwizard HealthCheck actor pattern and multibinder aggregation.
- [ADR 0005](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md): Dual-role Hibernate session architecture (`njall_admin` vs `njall_users`).
- [ADR 0008](../../../../adr/0008-module-level-singleton-scoping-convention.md): Explicit Guice module-level singleton scoping without `@Singleton` on implementation classes.
- [ADR 0009](../../../../adr/0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md): Deferred `Provider<SessionFactory>` injection.
- [ADR 0011](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md): Package-private `@Inject` constructors and downward/outward package dependency DAG.
- [ADR 0014](../../../../adr/0014-guice-injector-hardening-and-strict-bindings.md): Strict explicit bindings with circular proxy rejection.

### C4 Component Diagram

```
+─────────────────────────────────────────────────────────────────────────────+
| CONTAINER: Njall Server Application (:server)                               |
|                                                                             |
|  +────────────────────────────+       +──────────────────────────────────+  |
|  | DefaultAdminRoute (:api)   | ----> | HealthCheckActor (:api)          |  |
|  | - GET /api/admin/v1/health |       | - Typed Pekko Actor (Ask 2s)     |  |
|  +────────────────────────────+       +──────────────────────────────────+  |
|                                                         │                   |
|                                                         │ runHealthChecks() |
|                                                         ▼                   |
|                                       +──────────────────────────────────+  |
|                                       | HealthCheckRegistry (:common)    |  |
|                                       | - Multibinder<HealthCheck>       |  |
|                                       +──────────────────────────────────+  |
|                                                 /              \            |
|                     ┌──────────────────────────┘                │           |
|                     ▼                                           ▼           |
|  +──────────────────────────────────+   +────────────────────────────────+  |
|  | PekkoHealthCheck (:api)          |   | AdminDatabaseHealthCheck       |  |
|  | - ActorSystem termination        |   | (:data)                        |  |
|  | - CoordinatedShutdown state      |   |   +──────────────────────────+ |  |
|  +──────────────────────────────────+   |   | Caffeine LoadingCache    | |  |
|                                         |   | - Key: "admin_db_ping"   | |  |
|                                         |   | - TTL: 10 seconds        | |  |
|                                         |   +──────────────────────────+ |  |
|                                         |                 │ (cache miss) |  |
|                                         |                 ▼              |  |
|                                         |   @NjallAdmin                  |  |
|                                         |   Provider<SessionFactory>     |  |
|                                         +────────────────────────────────+  |
|                                                           │                 |
+───────────────────────────────────────────────────────────┼─────────────────+
                                                            │ SELECT 1 (1s)
                                                            ▼
                                          +───────────────────────────────────+
                                          | DATABASE: PostgreSQL (njall_admin)|
                                          +───────────────────────────────────+
```

## Goals / Non-Goals

**Goals:**
- Provide an administrative database health probe in `:data` assessing PostgreSQL connectivity for `njall_admin`.
- Execute a lightweight `SELECT 1` native query on the Hibernate session with an explicit 1-second timeout.
- Cache health probe results using Caffeine (`expireAfterWrite(10s)`) to throttle database interactions to at most 0.1 QPS.
- Enable fully deterministic unit testing of cache hits, misses, and expirations via an injected `Ticker` and `Duration`.
- Register the probe into `Multibinder<HealthCheck>` via `DataHealthModule` in compliance with ArchUnit rules and Guice injector constraints.

**Non-Goals:**
- Probing the `njall_users` session factory in this iteration (scoped to `njall_admin` per requirements; `njall_users` can be added in a future change).
- Health checking database replica lag, connection pool utilization percentages, or disk space metrics.
- Background asynchronous polling via daemon threads (Caffeine synchronous read-eviction avoids daemon executor overhead).

## Decisions

### Decision 1: Dedicated Package and Guice Module (`com.larpconnect.njall.data.health`)
- **Choice**: Place `AdminDatabaseHealthCheck` and `DataHealthModule` in a new package `com.larpconnect.njall.data.health`, and install `DataHealthModule` from `DataModule`.
- **Rationale**: Isolates health diagnostic concerns from session factory construction (`com.larpconnect.njall.data.session`) and DAOs (`com.larpconnect.njall.data.dao`), maintaining a clean, modular DAG structure.
- **Alternatives Considered**: 
  - *Place inside `com.larpconnect.njall.data.session`*: Rejected to avoid conflating session lifecycle management with Dropwizard health monitoring.

### Decision 2: Hibernate Native Query with Explicit Timeout
- **Choice**: Execute `session.createNativeQuery("SELECT 1", Integer.class).setTimeout(1).getSingleResult()` inside `pingDatabase()`.
- **Rationale**: Matches native query conventions used across the **Data plane** DAOs. The explicit 1-second timeout ensures a hung query or locked connection fails fast before the 2-second HTTP actor ask timeout elapses.
- **Alternatives Considered**:
  - *Direct JDBC `session.doWork(...)`*: More verbose, drops below Hibernate abstraction level without performance advantages for a 10-second cached query.

### Decision 3: In-Memory Caching via Caffeine `LoadingCache`
- **Choice**: Wrap probe execution with a Caffeine `LoadingCache<String, Result>` configured with `expireAfterWrite(cacheTtl)` and a default TTL of 10 seconds.
- **Rationale**: Meets repository standards prioritizing Caffeine for in-memory caching (`AGENTS.md`). Ensures thread-safe request deduplication and eliminates probe storms against PostgreSQL during high-frequency HTTP health checks. Synchronous eviction on access avoids background executor thread leaks.
- **Alternatives Considered**:
  - *Scheduled background executor*: Spawns background daemon threads that complicate lifecycle management and test shutdown.
  - *No caching (execute on every request)*: Exposes the database and connection pool to denial-of-service under high request volumes.

### Decision 4: Injectable Ticker and Duration for Deterministic Testing
- **Choice**: Provide a package-private constructor `AdminDatabaseHealthCheck(Provider<SessionFactory> sessionFactoryProvider, Duration cacheTtl, Ticker ticker)` alongside the package-private `@Inject` constructor injecting `Ticker` via Guice and defaulting cache TTL to 10 seconds.
- **Rationale**: Allows unit tests to verify cache hits, cache misses, and expiration transitions instantly using fake tickers without `Thread.sleep` or timing flakiness.

## Risks / Trade-offs

- **[Risk]** Stale health status for up to 10 seconds following a database crash.
  - *Mitigation*: 10 seconds is well within industry standard health probe tolerances (e.g. Kubernetes liveness probe periods are typically 10-30s). The reduction in connection pool strain heavily outweighs 10 seconds of reporting latency.
- **[Risk]** Existing integration tests mocking `SessionFactory` may fail when `/api/admin/v1/health` executes.
  - *Mitigation*: Update `HealthEndpointSteps` to mock `sessionFactory.openSession()` returning a healthy `SELECT 1` mock session.

## Migration Plan

1. Add `api(libs.caffeine)` to `:data` **Library module** `build.gradle.kts`.
2. Implement `AdminDatabaseHealthCheck`, `DataHealthModule`, and `package-info.java` in `com.larpconnect.njall.data.health`.
3. Install `DataHealthModule` in `DataModule.java`.
4. Update `HealthEndpointSteps.java` in `:integration` and add new Cucumber scenarios for database health checks and caching.
5. Verify build and all quality gates via `./gradlew check build`.

## Open Questions

None. All architectural aspects are resolved and align with repository standards and in-force ADRs.
