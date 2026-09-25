# 0018: Caffeine-Cached Administrative Database Health Check

- Status: accepted
- Date: 2026-09-24

## Context

Under [ADR 0002](0002-dropwizard-healthcheck-actor-pattern.md), Project Njall standardizes runtime health monitoring using Dropwizard Metrics `HealthCheck` integrated via Google Guice and Apache Pekko Typed. The administrative health probe `/api/admin/v1/health` delegates probe evaluation to `HealthCheckActor`, which runs checks registered in `HealthCheckRegistry`.

However, only `PekkoHealthCheck` was bound in the **API plane**. A PostgreSQL database crash, connection failure, or pool exhaustion for the `njall_admin` database role went undetected by `/api/admin/v1/health`. At the same time, executing a live database query on every incoming HTTP health probe creates severe risks of connection pool exhaustion, query thrashing, and denial-of-service during high-frequency health polling from infrastructure monitors and orchestrators.

Under `AGENTS.md`, Caffeine is the designated library for high-performance in-memory caching. We need a standardized approach to database health checking that validates connectivity without overloading persistence infrastructure.

## Decision

1. **Administrative Database Health Check in `:data`**:
   - Introduce `AdminDatabaseHealthCheck` in `com.larpconnect.njall.data.health` implementing Dropwizard `HealthCheck`.
   - Inject `@NjallAdmin Provider<SessionFactory>` to resolve the Hibernate `SessionFactory` configured for the `njall_admin` role in compliance with [ADR 0009](0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md).
2. **Native Ping Query with Fast Timeout**:
   - The probe MUST execute a native SQL query equivalent to `SELECT 1` on the Hibernate session with an explicit 1-second query timeout (`.setTimeout(1)`), ensuring that hung connections fail fast before the 2-second HTTP actor ask timeout elapses.
3. **In-Memory Caching via Caffeine**:
   - The probe MUST cache probe results using an in-memory Caffeine `LoadingCache<String, Result>` configured with `expireAfterWrite` set to 10 seconds.
   - Database ping queries MUST execute only on cache misses or expirations, capping query frequency to at most 0.1 QPS (once every 10 seconds) regardless of incoming HTTP probe frequency.
   - Eviction MUST occur synchronously on access, avoiding background daemon threads or scheduled executor lifecycles.
4. **Deterministic Testing via Ticker**:
   - The class MUST provide a package-private constructor accepting a custom cache `Duration` and `com.github.benmanes.caffeine.cache.Ticker` to enable fully deterministic unit tests of cache hits, misses, and expiration without real-time delays.
5. **Guice Multibinder and Scoping**:
   - The probe MUST be bound with explicit `Scopes.SINGLETON` in `DataHealthModule` and contributed to `Multibinder<HealthCheck>` in compliance with [ADR 0008](0008-module-level-singleton-scoping-convention.md).
   - `DataHealthModule` MUST be installed by `DataModule` in `:data`.

## Consequences

### Positive
- Accurate reporting of `njall_admin` PostgreSQL connectivity at `/api/admin/v1/health`.
- Completely prevents database query storms and connection pool starvation during health probe bursts.
- Eliminates daemon threads and asynchronous scheduler overhead by leveraging Caffeine's synchronous read-time eviction.
- Enables deterministic, instantaneous testing of cache expiration behavior using synthetic tickers.
- Fully compliant with ArchUnit constraints regarding provider injection and constructor encapsulation.

### Negative
- Health check status reflects up to 10 seconds of reporting latency following a database outage or recovery.
- Adds `api(libs.caffeine)` to the `:data` compile classpath (already managed in the version catalog and `:parent` constraints).
