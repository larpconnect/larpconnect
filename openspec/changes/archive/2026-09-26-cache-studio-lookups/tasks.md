## 1. Configuration and Data Layer Implementation (:common, :data)

- [x] 1.1 Add `larpconnect.data.cache.studio-lookup.refresh-interval = "5m"` and environment variable override to `reference.conf`.
- [x] 1.2 Define `StudioLookupCache` and `StudioLookupCacheService` interfaces in package `com.larpconnect.njall.data.cache`.
- [x] 1.3 Implement `DefaultStudioLookupCacheService` extending Guava's `AbstractScheduledService` wrapping an in-memory Caffeine cache, with synchronous `startUp()` pre-population, periodic `runOneIteration()` refresh, and on-demand `refresh()` method.
- [x] 1.4 Create `CacheModule` binding `StudioLookupCache` and `StudioLookupCacheService` in `Scopes.SINGLETON`, and install within `DataModule`.
- [x] 1.5 Add comprehensive unit tests in `DefaultStudioLookupCacheServiceTest` and `CacheModuleTest`, and verify `./gradlew :data:check` passes.

## 2. Server Lifecycle Integration (:server)

- [x] 2.1 Update `DefaultServerManagerService` to inject `StudioLookupCacheService`, call `startAsync().awaitRunning()` before `HttpServerService.start()` in `startUp()`, and call `stopAsync().awaitTerminated()` in `shutDown()`.
- [x] 2.2 Update `ServerManagerServiceTest` and `ServerModuleTest` to verify cache service startup ordering and coordinated shutdown.
- [x] 2.3 Verify `./gradlew :server:check` passes.

## 3. API Plane Route and Actor Integration (:api)

- [x] 3.1 Update `StudioCommand.GetStudio` to carry the resolved `StudioLookup` record instead of the raw string parameter.
- [x] 3.2 Update `StudiosRoute` to inject `StudioLookupCache`, resolve `{studio-id}` in-memory on the route thread, short-circuit unmapped or soft-deleted studios with immediate HTTP 404, and forward active lookups to `StudioActor`.
- [x] 3.3 Refactor `StudioActor` and `DefaultStudioActorFactory` to remove `StudioLookupDAO` injection, receiving the resolved `StudioLookup` directly from `StudioCommand.GetStudio` to query `StudioDAO`.
- [x] 3.4 Update `StudioAdminActor` to inject `StudioLookupCache` and call `studioLookupCache.refresh()` following successful studio creation and soft-deletion mutations.
- [x] 3.5 Update unit tests in `:api` (`StudiosRouteTest`, `StudioActorTest`, `StudioAdminActorTest`, `StudiosModuleTest`, `AdminModuleTest`, `ApiModuleTest`), and verify `./gradlew :api:check` passes.

## 4. Integration Testing and Verification

- [x] 4.1 Update Cucumber feature scenarios and step definitions in `:integration` to assert non-blocking cached studio lookups, startup pre-warming, and immediate post-mutation cache refresh.
- [x] 4.2 Run `./gradlew check build` across all modules to ensure Spotless, Checkstyle, SpotBugs, ArchUnit, JaCoCo (85% line, 90% branch), and all tests pass.
- [x] 4.3 Run `openspec validate cache-studio-lookups --type change --strict` to verify OpenSpec planning validity.
