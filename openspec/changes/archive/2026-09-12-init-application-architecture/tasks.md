## 1. Build and Dependency Configuration

- [x] 1.1 Update `settings.gradle.kts` to include `:parent`, `:bom`, `:test`, `:common`, `:api`, `:server`, and `:integration`, verifying `./gradlew projects` succeeds.
- [x] 1.2 Update `gradle/libs.versions.toml` with Apache Pekko (`2.0.0-M4`), Pekko HTTP (`2.0.0-M1`), and Typesafe Config definitions.
- [x] 1.3 Create `:parent/build.gradle.kts` applying `njall.java-platform-conventions` to centralize dependency versions.
- [x] 1.4 Create `:bom/build.gradle.kts` applying `njall.java-platform-conventions` to define local module constraints.
- [x] 1.5 Create `:test/build.gradle.kts` applying `njall.java-library-conventions` providing shared test dependencies and fixtures.

## 2. Common Module Implementation

- [x] 2.1 Create `:common/build.gradle.kts` applying `njall.java-library-conventions` with Guice and Typesafe Config dependencies.
- [x] 2.2 Create `reference.conf` in `:common/src/main/resources/reference.conf` specifying default `0.0.0.0:8080` and environment overrides.
- [x] 2.3 Implement `ServerConfig` record and `ConfigModule` in `com.larpconnect.njall.common.config`.
- [x] 2.4 Implement `CommonModule` in `com.larpconnect.njall.common` installing `ConfigModule`.
- [x] 2.5 Add unit tests in `:common/src/test` verifying default config and environment variable overrides, verifying `./gradlew :common:check` passes.

## 3. API Module Implementation

- [x] 3.1 Create `:api/build.gradle.kts` applying `njall.java-library-conventions` with `:common`, Pekko HTTP, and Guice dependencies.
- [x] 3.2 Create spec-first contract `openapi.yaml` in `:api/src/main/resources/openapi.yaml` specifying `GET /` with 200 OK.
- [x] 3.3 Implement `RootRoute` interface and `DefaultRootRoute` in `com.larpconnect.njall.api.http` serving `GET /` returning 200 OK blank.
- [x] 3.4 Implement `HttpModule` and `ApiModule` in `com.larpconnect.njall.api` binding `RootRoute`.
- [x] 3.5 Add unit tests in `:api/src/test` using Pekko HTTP `RouteTest` verifying `GET /` returns 200 OK, verifying `./gradlew :api:check` passes.

## 4. Server Module Implementation

- [x] 4.1 Create `:server/build.gradle.kts` applying `njall.java-application-conventions` with dependencies on `:common`, `:api`, and Pekko.
- [x] 4.2 Implement `HttpServerService` interface and `DefaultHttpServerService` in `com.larpconnect.njall.server.http` binding Pekko HTTP server with `CoordinatedShutdown`.
- [x] 4.3 Implement `ServerModule` in `com.larpconnect.njall.server` installing `CommonModule`, `ApiModule`, and providing `@Singleton ActorSystem<Void>`.
- [x] 4.4 Implement `ServerApp` main entry point in `com.larpconnect.njall.server` bootstrapping Guice injector and coordinated shutdown hook.
- [x] 4.5 Add unit tests in `:server/src/test` verifying server lifecycle and Guice bindings, verifying `./gradlew :server:check` passes.

## 5. Integration Testing and Verification

- [x] 5.1 Create `:integration/build.gradle.kts` applying `njall.java-library-conventions` with Cucumber, `:common`, `:api`, and `:server`.
- [x] 5.2 Implement Cucumber feature `root_endpoint.feature` and step definitions in `com.larpconnect.njall.integration` testing live HTTP server on dynamic port.
- [x] 5.3 Run full quality verification with `./gradlew check build` verifying all tests, JaCoCo coverage (85% line, 90% branch), Checkstyle, SpotBugs, ErrorProne, and Spotless pass.
- [x] 5.4 Run `openspec validate init-application-architecture --strict` to verify all change artifacts satisfy schema requirements.
