# 0026: Caffeine Cached Studio Lookup Service

## Status

Accepted

## Date

2026-09-26

## Context

In **Njall**, architectural decision [0025: User Space Studios API and Tenant Row-Level Security](0025-user-space-studios-api-and-tenant-row-level-security.md) introduced user-space **Studio** endpoints under `/api/studios/{studio-id}/v1/studio`. A negative consequence noted in ADR 0025 was that every user-facing studio query incurred two database queries: an initial lookup query against `njall_admin.studios_lookup` via `StudioLookupDAO` to resolve the `{studio-id}` parameter (UUID or alias) into the internal `tenant_id`, followed by the tenanted query against `njall_users.studios` via `StudioDAO`.

Studio lookup routing metadata changes very rarely, but is evaluated on every tenant API request. Executing synchronous database queries on each request path introduces unnecessary latency, database connection pool consumption, and thread contention.

## Considered Options

- **Option 1: Guava Scheduled Service with In-Memory Caffeine Cache** (Selected)
  - Implement `DefaultStudioLookupCacheService` in `:data` wrapping an in-memory Caffeine cache.
  - Prepopulate the cache synchronously during server startup before the HTTP server binds sockets.
  - Periodically reload the entire cache on a 5-minute background schedule via Guava's `AbstractScheduledService`.
  - Provide an on-demand `refresh()` method invoked by administrative actors upon studio creation or soft-deletion.
  - Resolve lookups directly in-memory on the HTTP route thread, short-circuiting nonexistent or soft-deleted studios with HTTP 404 immediately.
- **Option 2: Distributed Cache (Memcached / Redis)** (Deferred: introduces operational complexity and external infrastructure dependencies that are premature for single-node deployments; can be adopted later).
- **Option 3: Retain On-Demand Database Queries with Actor Offloading** (Rejected: continues incurring database round-trips and connection pool overhead on high-frequency tenant request paths).

## Decision

1. **In-Memory Caching in `:data`**: Introduce `StudioLookupCache` and `StudioLookupCacheService` in `com.larpconnect.njall.data.cache`. Index entries under both `id:<studioId>` and `alias:<lowercaseAlias>`.
2. **Pre-Warming and Lifecycle Orchestration**: Manage cache lifecycle within `DefaultServerManagerService` in `:server`. During startup, pre-warm the cache by calling `startAsync().awaitRunning()` before starting `HttpServerService`. Stop the cache service during server coordinated shutdown.
3. **Periodic Full Reload with Failure Tolerance**: Periodically reload the entire cache every 5 minutes. Catch transient database exceptions to ensure current in-memory cache data remains intact.
4. **Immediate Single-Node Mutation Consistency**: Provide a synchronous `refresh()` method called by `StudioAdminActor` immediately after successful database mutations.
5. **Route-Level Non-Blocking Validation**: In `StudiosRoute`, perform in-memory cache lookup on the route thread, immediately returning HTTP 404 for invalid or soft-deleted studios before dispatching to `StudioActor`.

## Consequences

- **Positive**: Eliminates database queries for studio lookup resolution on tenant request paths; reduces tenant retrieval from two database queries to one; allows HTTP 404 short-circuiting without actor messaging overhead; ensures the cache is fully warm before accepting traffic.
- **Negative**: Single-node cache means multi-node deployments in the future will experience up to 5 minutes of eventual consistency until distributed cache invalidation is implemented.
