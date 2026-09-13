## Why

The HTTP runtime server requires an administrative health probe at `/api/admin/v1/health` to allow external monitors, orchestrators, and load balancers to determine whether the Apache Pekko framework is up and responsive. Currently, only the root path `/` exists. This change establishes an extensible healthcheck architecture based on Dropwizard Metrics and Pekko Typed actors, supporting multiple base paths (`/` and `/api/admin`).

## What Changes

- **Dependency**: Add `io.dropwizard.metrics:metrics-healthchecks` dependency constrained in `:parent` and exposed in `:common`.
- **Health Check Infrastructure**: Introduce `HealthModule` in `:common` configuring a Guice `Multibinder<HealthCheck>` and providing a centralized `HealthCheckRegistry`.
- **Pekko Health Check**: Implement `PekkoHealthCheck` in `:api` evaluating Pekko `ActorSystem` termination state, `CoordinatedShutdown` lifecycle, and message processing capability.
- **Actor Wrapper**: Implement `HealthCheckActor` in `:api` wrapping `HealthCheckRegistry` execution with a Pekko Typed ask pattern.
- **Admin Routing**: Introduce `AdminRoute` handling base path `/api/admin` with endpoint `GET /api/admin/v1/health` returning HTTP 200 OK (empty body) when healthy and HTTP 500 Internal Server Error (empty body) when unhealthy.
- **Composite Routing**: Update `:api` route structure to cleanly aggregate multiple base paths (`/` and `/api/admin`) into the HTTP server binding.
- **Contract & Verification**: Update `openapi.yaml` with the healthcheck specification, add unit tests across `:common` and `:api`, and add Cucumber integration tests in `:integration`.

## Capabilities

### New Capabilities
- `admin-healthcheck`: Administrative health probe at `/api/admin/v1/health` reporting Pekko framework health status.

### Modified Capabilities
*(None)*

## Impact

- **Build & Dependencies**: `gradle/libs.versions.toml`, `parent/build.gradle.kts`, `common/build.gradle.kts`.
- **Common Module**: `com.larpconnect.njall.common.health` package with `HealthModule`.
- **API Module**: `com.larpconnect.njall.api.admin` package with `AdminRoute`, `PekkoHealthCheck`, and `HealthCheckActor`; updated `openapi.yaml`.
- **Server Module**: Server startup health check verification and router composition.
- **Integration Module**: Cucumber feature `health_endpoint.feature` and step definitions.
