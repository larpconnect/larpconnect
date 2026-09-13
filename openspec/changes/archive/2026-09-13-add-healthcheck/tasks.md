## 1. Dependencies and Platform Constraints

- [x] 1.1 Add `metrics-healthchecks` (`io.dropwizard.metrics:metrics-healthchecks:4.2.30`) to `gradle/libs.versions.toml`.
- [x] 1.2 Add `metrics-healthchecks` version constraint to `parent/build.gradle.kts` within the `constraints` block.
- [x] 1.3 Add `api(libs.metrics.healthchecks)` dependency to `common/build.gradle.kts`.

## 2. Common Module Health Infrastructure

- [x] 2.1 Implement `HealthModule` in `com.larpconnect.njall.common.health` configuring Guice `Multibinder<HealthCheck>` and `@Singleton HealthCheckRegistry` provider.
- [x] 2.2 Install `HealthModule` in `CommonModule` (`com.larpconnect.njall.common`).
- [x] 2.3 Add unit tests in `:common/src/test` verifying `HealthCheckRegistry` injection and multibinder functionality, verifying `./gradlew :common:check` passes.

## 3. OpenAPI Contract Specification

- [x] 3.1 Update `openapi.yaml` in `:api/src/main/resources/openapi.yaml` defining endpoint `GET /api/admin/v1/health` with 200 OK and 500 responses.

## 4. API Module Health Check and Actor Implementation

- [x] 4.1 Implement `PekkoHealthCheck` in `com.larpconnect.njall.api.admin` extending Dropwizard `HealthCheck` to check ActorSystem termination and CoordinatedShutdown status.
- [x] 4.2 Define sealed command and response records `HealthCheckCommand` and `HealthCheckResponse` in `com.larpconnect.njall.api.admin`.
- [x] 4.3 Implement stateless `HealthCheckActor` behavior in `com.larpconnect.njall.api.admin` executing registry checks.
- [x] 4.4 Implement `AdminRoute` interface and `DefaultAdminRoute` in `com.larpconnect.njall.api.admin` handling `/api/admin/v1/health` using `AskPattern.ask(...)`.
- [x] 4.5 Update `HttpModule` and `ApiModule` to bind `PekkoHealthCheck` into `Multibinder<HealthCheck>`, bind `AdminRoute`, and concatenate routes for `/` and `/api/admin`.
- [x] 4.6 Add unit tests in `:api/src/test` covering `PekkoHealthCheck`, `HealthCheckActor` (with `BehaviorTestKit`), and `AdminRoute` (verifying 200 OK, 500 error, and timeout), verifying `./gradlew :api:check` passes.

## 5. Server Module Integration and Startup Lifecycle

- [x] 5.1 Update `DefaultHttpServerService` (or route composition) in `:server` to bind composite routing for `/` and `/api/admin`.
- [x] 5.2 Validate server startup lifecycle with registered health checks.
- [x] 5.3 Add unit tests in `:server/src/test` verifying server startup with admin health route, verifying `./gradlew :server:check` passes.

## 6. Integration Testing and Verification

- [x] 6.1 Create Cucumber feature `health_endpoint.feature` in `:integration/src/test/resources/features/` with scenarios for `GET /api/admin/v1/health`.
- [x] 6.2 Implement Cucumber step definitions in `com.larpconnect.njall.integration.HealthEndpointSteps` verifying HTTP 200 OK and empty response body on a running server.
- [x] 6.3 Run full quality verification with `./gradlew check build` verifying all tests, JaCoCo thresholds, Checkstyle, SpotBugs, and Spotless pass.
- [x] 6.4 Run `openspec validate add-healthcheck --strict` to verify all change artifacts satisfy schema requirements.
