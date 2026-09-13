## Why

Project Njall requires a foundational multimodule runtime architecture using Java 25 LTS, Apache Pekko Typed, and Google Guice. Establishing this baseline runtime provides the core application skeleton, build pipeline, configuration system, and HTTP server endpoint necessary for all subsequent domain features and API services.

## What Changes

- **Activate Core Pipeline Modules**: Uncomment and configure the active pipeline in `settings.gradle.kts`: `:parent`, `:bom`, `:test`, `:common`, `:api`, `:server`, and `:integration`.
- **Version Catalog & Dependencies**: Define Apache Pekko (`2.0.0-M4`), Pekko HTTP (`2.0.0-M1`), and Typesafe Config in `gradle/libs.versions.toml` and `:parent`.
- **Config Layer (`:common`)**: Implement Typesafe Config integration with `reference.conf` (`larpconnect.http.host`, `larpconnect.http.port`) and strongly-typed `ServerConfig` record supporting environment variable overrides (`PORT`, `HOST`).
- **HTTP Routing (`:api`)**: Define the spec-first OpenAPI contract (`openapi.yaml`) and implement `RootRoute` (`DefaultRootRoute`) serving `200 OK` with an empty entity at `/`.
- **Server Bootstrap (`:server`)**: Implement `HttpServerService` (`DefaultHttpServerService`) using Pekko HTTP `newServerAt(...)`, integrate Pekko `CoordinatedShutdown` for phase-based socket unbinding, and provide the `ServerApp` main entry point.
- **Dependency Injection**: Provide Guice modules (`CommonModule`, `ApiModule`, `ServerModule`) adhering to strict DAG rules and IOSP-Lite.
- **Testing Layers**:
  - Layer 1 Unit Tests in `src/test`: Verify config parsing in `:common`, route behavior with Pekko HTTP testkit in `:api`, and server lifecycle in `:server`.
  - Layer 2 Integration Tests in `:integration`: Add Cucumber feature scenarios testing the running server over HTTP with dynamic port allocation.

## Capabilities

### New Capabilities
- `http-server-runtime`: Core HTTP server lifecycle, Typesafe configuration, and root endpoint returning 200 OK blank response.

### Modified Capabilities
None.

## Impact

- Activates 7 Gradle subprojects in `settings.gradle.kts`.
- Introduces package trees: `com.larpconnect.njall.common`, `com.larpconnect.njall.api`, `com.larpconnect.njall.server`, `com.larpconnect.njall.test`, `com.larpconnect.njall.integration`.
- Establishes `openapi.yaml` and sets up end-to-end Cucumber integration testing.
