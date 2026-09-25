## 1. Dependency Configuration in `:data`

- [x] 1.1 Add `api(libs.caffeine)` to `data/build.gradle.kts`.
- [x] 1.2 Verify `:data` compilation passes via `./gradlew :data:compileJava`.

## 2. Health Check Implementation in `:data`

- [x] 2.1 Create package `com.larpconnect.njall.data.health` and add `package-info.java` with `@NonNullByDefault`.
- [x] 2.2 Implement `AdminDatabaseHealthCheck` in `com.larpconnect.njall.data.health` executing `SELECT 1` with an explicit 1-second timeout against `@NjallAdmin Provider<SessionFactory>` and caching results via Caffeine `LoadingCache` with a 10-second expiration.
- [x] 2.3 Provide a package-private constructor in `AdminDatabaseHealthCheck` accepting `Provider<SessionFactory>`, cache `Duration`, and `Ticker` for deterministic testing.
- [x] 2.4 Implement `DataHealthModule` in `com.larpconnect.njall.data.health` binding `AdminDatabaseHealthCheck` in `Scopes.SINGLETON` and contributing to `Multibinder<HealthCheck>`.
- [x] 2.5 Update `DataModule` in `com.larpconnect.njall.data` to install `DataHealthModule`.

## 3. Unit Testing in `:data`

- [x] 3.1 Implement unit tests in `AdminDatabaseHealthCheckTest` verifying healthy `SELECT 1` response, cache hits within the 10-second window, cache expiration after 10 seconds using fake `Ticker`, and unhealthy handling on database exceptions.
- [x] 3.2 Update `DataModuleTest` to verify module composition and initialization without redundant binding tests.
- [x] 3.3 Verify all `:data` checks pass via `./gradlew :data:check`.

## 4. Integration and Acceptance Testing in `:integration`

- [x] 4.1 Update `HealthEndpointSteps` in `:integration` to stub `sessionFactory.openSession()` for healthy ping execution across existing health probe tests.
- [x] 4.2 Add new step definitions and scenarios to `health_endpoint.feature` verifying that `/api/admin/v1/health` returns 500 when database ping fails, and returns cached results on repeated queries.
- [x] 4.3 Verify all integration tests and ArchUnit rules pass via `./gradlew :integration:test`.

## 5. Verification and Validation

- [x] 5.1 Run full build and static analysis via `./gradlew check build`.
- [x] 5.2 Validate OpenSpec change integrity via `openspec validate add-admin-database-healthcheck --type change --strict`.
