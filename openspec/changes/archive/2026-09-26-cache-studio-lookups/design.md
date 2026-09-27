# Technical Design: In-Memory Studio Lookup Cache

## Context

In **Njall**, architectural decision [0025: User Space Studios API and Tenant Row-Level Security](../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md) introduced user-space **Studio** entities and the `/api/studios/{studio-id}/v1/studio` endpoint. Because client requests supply either a public studio **ID** (UUID) or a human-readable **Alias**, the **API plane** must resolve `{studio-id}` to the internal database `tenant_id` before querying the tenanted user schema via `StudioDAO`.

Currently, this lookup queries `StudioLookupDAO` against `njall_admin.studios_lookup` on every request. While `StudioLookupDAO` execution was offloaded to a dedicated `@Blocking Props` dispatcher to prevent thread starvation on Pekko HTTP connection threads, the database round-trip remains unnecessary overhead: studio directory metadata is small, rarely changes, and is read on virtually every tenant API request.

This design introduces an in-memory Caffeine cache managed by a Guava `AbstractScheduledService` (`DefaultStudioLookupCacheService`) in `:data`. The cache is pre-populated at **Server** startup before socket binding, periodically reloaded on a 5-minute schedule, and refreshed immediately upon administrative mutations.

## Goals / Non-Goals

**Goals:**
- Eliminate blocking database queries for studio lookup resolution on the tenant request path.
- Provide non-blocking, O(1) in-memory resolution by both UUID and alias via `StudioLookupCache`.
- Reject requests for nonexistent or soft-deleted studios with HTTP 404 immediately on the route thread, preventing unnecessary **Actor** dispatching.
- Pre-warm the cache synchronously during server startup, aborting server boot if initial database loading fails.
- Periodically reload the entire cache every 5 minutes (configurable via HOCON) while gracefully tolerating transient database errors.
- Trigger on-demand cache refresh on administrative studio provisioning and soft-deletion for immediate single-node consistency.

**Non-Goals:**
- Distributed cache synchronization (e.g. Memcached, Redis); single-node Caffeine caching is sufficient for the current phase.
- Caching tenanted user data (`njall_users.studios`); only administrative routing lookup metadata (`njall_admin.studios_lookup`) is cached.
- Modifying public REST API contracts, path structures, or OpenAPI schemas.

## Architecture & C4 Component Diagram

```
+--------------------------------------------------------------------------------------------------+
| Component: Project Njall Runtime Application (:server, :api, :data)                              |
|                                                                                                  |
| [Server Manager Service]                                                                         |
| (DefaultServerManagerService)                                                                    |
|   |                                                                                              |
|   +-- (1) startAsync().awaitRunning()                                                            |
|   |         |                                                                                    |
|   |         v                                                                                    |
|   |   +-------------------------------------------------------------+                            |
|   |   | Studio Lookup Cache Service (:data)                         |                            |
|   |   | (DefaultStudioLookupCacheService: AbstractScheduledService) |                            |
|   |   |                                                             |                            |
|   |   | - startUp(): executes initial full reload from DB           |                            |
|   |   | - runOneIteration(): periodic 5m reload                     |                            |
|   |   | - refresh(): immediate synchronous reload                   |                            |
|   |   | - in-memory Caffeine Cache: [id:<UUID>, alias:<String>]     |                            |
|   |   +-------------------------------------------------------------+                            |
|   |                                 |                                                            |
|   |                                 | queries on start / refresh                                 |
|   |                                 v                                                            |
|   |   +-------------------------------------------------------------+                            |
|   |   | StudioLookupDAO (:data) (@NjallAdmin SessionFactory)        |                            |
|   |   +-------------------------------------------------------------+                            |
|   |                                                                                              |
|   +-- (2) start() (only after cache is RUNNING)                                                  |
|             |                                                                                    |
|             v                                                                                    |
|       +-------------------------------------------------------------+                            |
|       | HTTP Server Service (:server)                               |                            |
|       +-------------------------------------------------------------+                            |
+--------------------------------------------------------------------------------------------------+
                                      |
                                      | incoming HTTP requests
                                      v
+--------------------------------------------------------------------------------------------------+
| API Plane Components (:api)                                                                      |
|                                                                                                  |
| +-----------------------------------------------------------------+                              |
| | StudiosRoute (Pekko HTTP Route Directives)                      |                              |
| | - injects StudioLookupCache                                     |                              |
| | - resolves studio-id param in-memory (0ms)                      |                              |
| | - missing / soft-deleted --> returns HTTP 404 immediately       |                              |
| | - active match ---------> passes resolved StudioLookup to actor |                              |
| +-----------------------------------------------------------------+                              |
|                                     |                                                            |
|                                     | StudioCommand.GetStudio(StudioLookup, replyTo)             |
|                                     v                                                            |
| +-----------------------------------------------------------------+                              |
| | StudioActor (:api) (Dispatcher: @Blocking Props)                |                              |
| | - queries tenanted StudioDAO with app.tenant_id                 |                              |
| +-----------------------------------------------------------------+                              |
|                                     |                                                            |
|                                     v                                                            |
| +-----------------------------------------------------------------+                              |
| | StudioDAO (:data) (@NjallUsers SessionFactory)                  |                              |
| +-----------------------------------------------------------------+                              |
|                                                                                                  |
| +-----------------------------------------------------------------+                              |
| | StudioAdminActor (:api)                                         |                              |
| | - injects StudioLookupDAO and StudioLookupCache                 |                              |
| | - onCreate / onSoftDelete: commits to DB, then calls refresh()  |                              |
| +-----------------------------------------------------------------+                              |
+--------------------------------------------------------------------------------------------------+
```

## Decisions

### 1. Subpackage and Module Placement in `:data`
- **Decision**: Place `StudioLookupCache`, `StudioLookupCacheService`, and `DefaultStudioLookupCacheService` in a new package `com.larpconnect.njall.data.cache` within `:data`. Expose a package Guice `CacheModule` installed by `DataModule`.
- **Rationale**: `StudioLookup` and `StudioLookupDAO` are defined in `:data`. Both `:server` and `:api` depend on `:data`, preserving the strict DAG (`:server -> :api -> :data -> :common`) and package-to-module parity without cyclic dependencies.
- **Alternatives Considered**:
  - Placing in `:common`: Rejected because `StudioLookup` domain records and Hibernate DAO types cannot leak into `:common`.
  - Placing in `:server`: Rejected because `:api` cannot depend on `:server`.

### 2. Dual-Keyed In-Memory Index with Atomic Swap
- **Decision**: Index each `StudioLookup` entry under two string keys: `id:<studioId>` and `alias:<lowercaseAlias>`. The cache service maintains an `AtomicReference` to an immutable index or internal Caffeine cache, completely rebuilt during each reload.
- **Rationale**: Callers query by either public UUID or alias. Atomically swapping the cache reference guarantees zero window of partial state, eliminates lock contention on reads, and ensures soft-deleted or removed studios do not linger in the cache.
- **Alternatives Considered**:
  - Single mutable cache with `putAll()`: Rejected because entries removed or soft-deleted in the database would remain in the cache until expiration.
  - Two separate caches: Rejected because dual caches can drift during reload failures.

### 3. Startup Pre-Warming via Guava `AbstractScheduledService`
- **Decision**: Implement `DefaultStudioLookupCacheService` extending Guava's `AbstractScheduledService`. Pre-population occurs synchronously in `startUp()`. `DefaultServerManagerService` calls `cacheService.startAsync().awaitRunning()` before calling `httpServerService.start()`.
- **Rationale**: Guava's `AbstractScheduledService` natively combines startup initialization, fixed-rate scheduling, and clean scheduler shutdown. Calling `awaitRunning()` blocks until `startUp()` succeeds; if the database is unreachable, startup throws an exception and the server fails fast before binding TCP sockets.
- **Alternatives Considered**:
  - Pekko scheduler: Rejected because persistence caching belongs to the data/server lifecycle, independent of actor systems.
  - Manual `ScheduledExecutorService`: Rejected because Guava's `AbstractScheduledService` provides standard lifecycle states (`STARTING`, `RUNNING`, `STOPPING`, `TERMINATED`, `FAILED`) consistent with `DefaultServerManagerService`.

### 4. Non-Blocking 404 Route Short-Circuiting
- **Decision**: Inject `StudioLookupCache` into `StudiosRoute`. In `GET /api/studios/{studio-id}/v1/studio`, resolve `{studio-id}` against the cache on the route thread. If absent or soft-deleted, return HTTP 404 immediately. If active, forward the resolved `StudioLookup` to `StudioActor`.
- **Rationale**: Looking up in an in-memory cache takes sub-microsecond time and does not block. Rejecting invalid requests before messaging actors prevents actor inbox contention and avoids dispatcher context switching for 404 traffic.
- **Alternatives Considered**:
  - Keeping lookup inside `StudioActor`: Rejected because forwarding invalid requests to a blocking actor thread pool is wasteful when the data is already warm in memory.

### 5. On-Demand Cache Refresh on Admin Mutations
- **Decision**: Add a `refresh()` method to `StudioLookupCache`. Inject `StudioLookupCache` into `StudioAdminActor`, triggering `refresh()` after successful `create` or `softDelete` database transactions.
- **Rationale**: Ensures immediate local consistency on the node where administrative mutations occur, without waiting up to 5 minutes for the background scheduler.

## Risks / Trade-offs

- **[Multi-Node Stale Reads]** -> Mitigation: While single-node admin writes invalidate immediately, in future multi-node deployments other nodes will reflect changes within the 5-minute background refresh interval. A future phase will introduce distributed invalidation (e.g. via Memcached or RabbitMQ pub-sub).
- **[Database Downtime During Startup]** -> Mitigation: If PostgreSQL is down when the server boots, `startUp()` fails fast, preventing the server from serving requests with an uninitialized cache.
- **[Database Downtime During Periodic Refresh]** -> Mitigation: `runOneIteration()` catches exceptions and logs an error, keeping the existing in-memory cache intact so transient DB hiccups do not drop cached studios.
- **[Memory Footprint]** -> Mitigation: Studio lookup records contain only UUIDs, aliases, and timestamps. Even tens of thousands of studios consume only a few megabytes of JVM heap.

## Migration Plan

1. Create `com.larpconnect.njall.data.cache` with `StudioLookupCache`, `StudioLookupCacheService`, and `DefaultStudioLookupCacheService`.
2. Register `CacheModule` in `DataModule`.
3. Add configuration to `reference.conf`.
4. Update `DefaultServerManagerService` to manage cache lifecycle.
5. Update `StudiosRoute` to resolve lookups via `StudioLookupCache` and update `StudioActor` / `StudioCommand.GetStudio`.
6. Update `StudioAdminActor` to trigger `refresh()` on mutations.
7. Verify all unit tests, integration tests, and quality gates pass.

## Open Questions

- None; all architectural branches were resolved during exploration.
