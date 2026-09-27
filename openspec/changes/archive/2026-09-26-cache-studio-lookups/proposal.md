# Change Proposal: In-Memory Studio Lookup Cache

## Why

In **Njall**, every incoming request to tenant-scoped endpoints under `/api/studios/{studio-id}/*` requires resolving the `{studio-id}` identifier (either public studio **ID** or unique **Alias**) to discover the internal **Tenant** identifier and verify that the **Studio** is active. Currently, this resolution performs a blocking database query via `StudioLookupDAO`. Because **Studio** lookup routing metadata is modified very rarely but queried on virtually every tenant API request, resolving it against the database adds unnecessary database round-trips and thread contention.

Pre-populating an in-memory Caffeine cache inside a Guava scheduled service during **Server** startup, refreshing it periodically, and invalidating it on administrative writes eliminates database lookup latency, enables immediate non-blocking validation and HTTP 404 rejection on the **API plane**, and ensures the cache is warm before traffic is served.

## What Changes

- **In-Memory Studio Lookup Caching Service (`:data`)**:
  - Introduce `StudioLookupCache` and `StudioLookupCacheService` interfaces in `com.larpconnect.njall.data.cache`.
  - Implement `DefaultStudioLookupCacheService` extending Guava's `AbstractScheduledService`, wrapping a Caffeine in-memory cache.
  - Prepopulate the cache synchronously during service `startUp()` by querying `StudioLookupDAO.list(DeletionFilter.INCLUDE_DELETED)`.
  - Periodically reload the entire cache on a fixed-rate schedule (default: 5 minutes) via `AbstractScheduledService.runOneIteration()`, using atomic reference swapping or key retention to prevent stale data while tolerating transient database failures.
  - Provide an on-demand `refresh()` operation to reload the cache immediately upon administrative mutations.
  - Expose lookup methods: `findById(UUID studioId)`, `findByAlias(String alias)`, `findByIdOrAlias(String idOrAlias)`, and `listActive()`.
  - Add HOCON configuration `larpconnect.data.cache.studio-lookup.refresh-interval` in `reference.conf` with environment variable override `LARPCONNECT_STUDIO_LOOKUP_CACHE_REFRESH_INTERVAL`.
  - Register bindings in `CacheModule` installed by `DataModule`.

- **Server Lifecycle Integration (`:server`)**:
  - Update `DefaultServerManagerService` to inject `StudioLookupCacheService`.
  - During `startUp()`, start `StudioLookupCacheService` and await running state *before* starting `HttpServerService`, ensuring the cache is fully pre-warmed before the HTTP port opens.
  - During `shutDown()`, stop `StudioLookupCacheService` and await termination to cease background scheduled refresh tasks.

- **Non-Blocking Route Validation (`:api`)**:
  - Update `StudiosRoute` to inject `StudioLookupCache`.
  - In `GET /api/studios/{studio-id}/v1/studio`, resolve `{studio-id}` via `StudioLookupCache.findByIdOrAlias(studioIdParam)` directly on the HTTP route thread.
  - Return HTTP 404 immediately if the **Studio** is unmapped or soft-deleted, avoiding unnecessary **Actor** messaging and thread dispatching.
  - Forward the resolved `StudioLookup` to `StudioActor` via `StudioCommand.GetStudio`.
  - Remove `StudioLookupDAO` dependency from `StudioActor` and its factory, delegating solely tenanted queries to `StudioDAO`.

- **Write-Through / Invalidation on Admin Mutations (`:api`)**:
  - Update `StudioAdminActor` to inject `StudioLookupCache`.
  - On `CreateStudio` or `SoftDeleteStudio`, trigger `studioLookupCache.refresh()` after successful database commit, providing immediate single-node consistency for administrative operations.

## Capabilities

### New Capabilities

- `studio-lookup-cache`: In-memory Caffeine caching service for multi-tenant studio lookups with synchronous pre-warming on server startup, periodic background full reloads, on-demand refresh on administrative mutations, and non-blocking O(1) in-memory resolution.

### Modified Capabilities

- `user-studios-api`: Resolves studio lookups via in-memory cache directly on HTTP routes, yielding immediate 404 responses for nonexistent or soft-deleted studios before dispatching requests to the tenanted studio actor.
- `http-server-runtime`: Coordinates cache service lifecycle within server manager service, ensuring pre-population before HTTP port binding and clean scheduler shutdown upon server termination.

## Impact

- **Affected Modules**: `:data` (new `cache` package and module), `:server` (server manager service startup/shutdown), `:api` (studios route non-blocking resolution and studio admin actor refresh).
- **Dependencies**: Uses existing Caffeine and Guava dependencies already present in `:data` and `:common`.
- **Breaking Changes**: None. Internal component signatures and route resolution paths are optimized without changing public REST contracts or OpenAPI schemas.
