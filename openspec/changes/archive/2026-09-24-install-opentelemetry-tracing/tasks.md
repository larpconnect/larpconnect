## 1. Dependency Catalog and Platform Configuration

- [x] 1.1 Declare OpenTelemetry BOM (`1.66.0`), OpenTelemetry Instrumentation BOM (`2.31.1`), `opentelemetry-api`, `opentelemetry-sdk`, and `opentelemetry-logback-mdc-1.0` in `gradle/libs.versions.toml`.
- [x] 1.2 Import OpenTelemetry BOMs and declare runtime library constraints in `parent/build.gradle.kts`.
- [x] 1.3 Add OpenTelemetry dependencies to `:common` (`api(libs.opentelemetry.api)`, `implementation(libs.opentelemetry.sdk)`), `:api` (`api(libs.opentelemetry.api)`), and `:server` (`implementation(libs.opentelemetry.logback.mdc)`).
- [x] 1.4 Verify platform compilation and dependency resolution via `wsl ./gradlew :parent:check`.

## 2. Common Telemetry Infrastructure

- [x] 2.1 Implement immutable `TraceContext(String traceId, String spanId)` record with validation and W3C header formatting utilities in `:common`.
- [x] 2.2 Implement `TelemetryModule` in `:common` providing `@Singleton` bindings for `OpenTelemetry` and `Tracer` instances.
- [x] 2.3 Install `TelemetryModule` in `CommonModule`.
- [x] 2.4 Add comprehensive unit tests in `:common` verifying `TraceContext` serialization, W3C regex compliance, and Guice provider injection.
- [x] 2.5 Run `wsl ./gradlew :common:check` to ensure 85% line and 90% branch coverage with zero static analysis violations.

## 3. Server Logging and MDC Configuration

- [x] 3.1 Update `server/src/main/resources/logback.xml` to wrap stdout with `OpenTelemetryAppender` and configure log pattern `[trace_id=%X{trace_id} span_id=%X{span_id}]`.
- [x] 3.2 Update `test/src/main/resources/logback-test.xml` with matching `OpenTelemetryAppender` wrapper and trace formatting pattern.
- [x] 3.3 Add unit test in `:server` verifying that active OpenTelemetry spans correctly populate MDC on log events.
- [x] 3.4 Run `wsl ./gradlew :server:check` to verify server module quality gates.

## 4. API Routing Tracing Directive and Contract Documentation

- [x] 4.1 Implement `TracingDirective` in `:api` extracting root server span, populating request MDC, attaching `traceparent` response headers, and ignoring client-supplied `traceparent` request headers.
- [x] 4.2 Integrate `TracingDirective` into `DefaultRootRoute` so all registered routes are traced.
- [x] 4.3 Update `api/src/main/resources/openapi.yaml` to document the `Traceparent` response header component across endpoints.
- [x] 4.4 Add unit tests in `:api` using Pekko HTTP `RouteTestKit` verifying response header injection, forged header discarding, and span completion.
- [x] 4.5 Run `wsl ./gradlew :api:check` to verify API module quality gates.

## 5. Actor Protocol MDC Correlation

- [x] 5.1 Update `HealthCheckCommand.CheckHealth` in `:api` to optionally accept `TraceContext`.
- [x] 5.2 Decorate `HealthCheckActor` behavior with `Behaviors.withMdc` to preserve `trace_id` and `span_id` during health check evaluation.
- [x] 5.3 Add synchronous unit test in `:api` using `BehaviorTestKit` asserting that actor execution populates MDC from incoming commands.
- [x] 5.4 Run `wsl ./gradlew :api:check` to ensure actor tests pass.

## 6. Dual-Layer Verification and Acceptance Testing

- [x] 6.1 Create Cucumber acceptance feature in `:integration` asserting that HTTP requests to `/` and `/api/admin/v1/health` return valid `traceparent` response headers.
- [x] 6.2 Add scenario in `:integration` verifying that client-supplied `traceparent` headers are ignored and replaced with a fresh trace ID.
- [x] 6.3 Add scenario in `:integration` asserting that request execution logs contain the exact matching `trace_id` and `span_id`.
- [x] 6.4 Execute full build and static analysis via `wsl ./gradlew check build`.
- [x] 6.5 Run `wsl openspec validate install-opentelemetry-tracing --type change --strict` to verify specification alignment.
