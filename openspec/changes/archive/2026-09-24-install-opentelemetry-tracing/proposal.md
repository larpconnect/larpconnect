## Why

In **Njall**, distributed diagnostics and request correlation across asynchronous Pekko **Actor** boundaries and HTTP endpoints currently lack unified tracing. Installing OpenTelemetry with Logback Mapped Diagnostic Context (MDC) correlation enables deterministic log tracking per request. Emitting W3C `traceparent` response headers across all HTTP endpoints in the **API plane** provides observability for downstream clients and operational debugging before eventual edge proxy stripping.

## What Changes

- Add OpenTelemetry BOM, OpenTelemetry API, OpenTelemetry SDK, and OpenTelemetry Logback MDC instrumentation (`opentelemetry-logback-mdc-1.0`) to dependency version catalog and **parent** platform.
- Configure `logback.xml` in **server** and `logback-test.xml` in **test** with the OpenTelemetry MDC appender and updated log pattern formatting `[trace_id=%X{trace_id} span_id=%X{span_id}]`.
- Provide a `TelemetryModule` in **common** that configures OpenTelemetry `Tracer` and lifecycle bindings within Guice.
- Introduce an immutable `TraceContext` record in **common** to represent correlation identifiers across thread and **Actor** boundaries.
- Implement a Pekko HTTP tracing directive in **api** wrapping `RootRoute`:
  - Generates a new root OpenTelemetry `Span` for every inbound HTTP request.
  - Strictly ignores and discards any client-supplied `traceparent` request headers to prevent untrusted trace spoofing.
  - Automatically attaches the generated W3C `traceparent` header to all outgoing HTTP responses.
  - Closes the active span upon request completion or failure.
- Document the `Traceparent` response header component in `openapi.yaml`.
- Support Pekko Typed actor MDC preservation using `Behaviors.withMdc` and `TraceContext` propagation.
- Add dual-layer verification: unit tests in `:common`, `:api`, `:server` and Cucumber acceptance integration tests in `:integration`.

## Capabilities

### New Capabilities
- `opentelemetry-tracing`: Defines OpenTelemetry root span generation, client traceparent header isolation, response header injection, and Logback MDC correlation across the **API plane**.

### Modified Capabilities
- `http-server-runtime`: Extends all HTTP server endpoints to guarantee inclusion of the W3C `traceparent` response header.

## Impact

- **Affected Modules**:
  - `gradle/libs.versions.toml`: OpenTelemetry versions and library entries.
  - `parent/build.gradle.kts`: OpenTelemetry BOM platforms and dependency constraints.
  - `common`: New `TraceContext` record and `TelemetryModule`.
  - `api`: Pekko HTTP tracing directive, `DefaultRootRoute` integration, actor command trace propagation, and `openapi.yaml`.
  - `server`: `logback.xml` appender wrapping and Guice composition in `ServerBindingModule`.
  - `test`: `logback-test.xml` appender configuration.
  - `integration`: Cucumber scenarios verifying response header and log correlation.
- **APIs**: All HTTP response headers now include `traceparent`. Request contracts remain unchanged (inbound `traceparent` ignored).
- **Dependencies**: New external runtime dependencies on OpenTelemetry Java SDK and Logback MDC instrumentation.
