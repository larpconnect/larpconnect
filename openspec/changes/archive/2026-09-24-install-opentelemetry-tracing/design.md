## Context

In **Njall**, HTTP requests entering the **Server** runtime are processed asynchronously across Pekko HTTP directives, typed dispatchers, and Pekko Typed **Actor** systems. While Logback provides logging infrastructure, logs currently lack correlated trace and span identifiers across asynchronous execution boundaries.

To provide unified observability and distributed request correlation across the **API plane**, this design establishes an OpenTelemetry-based telemetry pipeline integrated with Logback Mapped Diagnostic Context (MDC), W3C `traceparent` response headers, and Pekko Typed actor message protocols.

## Goals / Non-Goals

**Goals:**
- Add OpenTelemetry BOM, OpenTelemetry API, OpenTelemetry SDK, and OpenTelemetry Logback MDC instrumentation (`opentelemetry-logback-mdc-1.0`) to the centralized dependency catalog and **parent** platform.
- Configure `logback.xml` in **server** and `logback-test.xml` in **test** using the `OpenTelemetryAppender` wrapper and updated log pattern formatting `[trace_id=%X{trace_id} span_id=%X{span_id}]`.
- Provide an immutable `TraceContext` record and a Guice `TelemetryModule` in **common** exposing `OpenTelemetry` and `Tracer` singletons.
- Implement a Pekko HTTP `TracingDirective` in **api** wrapping `RootRoute`:
  - Starts a new root OpenTelemetry server span for every incoming HTTP request.
  - Ignores and discards any client-supplied `traceparent` request headers to prevent trace spoofing.
  - Attaches the compliant W3C `traceparent` header to all HTTP responses.
  - Populates MDC on the route thread and safely closes the span upon response completion.
- Support Pekko Typed **Actor** logging correlation by passing `TraceContext` in actor commands and applying `Behaviors.withMdc`.
- Update `openapi.yaml` to document the `Traceparent` response header component.
- Deliver comprehensive unit tests in `:common`, `:api`, `:server` and Cucumber acceptance tests in `:integration`.

**Non-Goals:**
- Accepting or continuing inbound distributed traces from external clients (all ingress requests originate fresh root spans).
- Direct OTLP exporter network configuration or external collector agent deployments (local MDC stdout correlation is the primary focus; OTLP exporters remain an opt-in future enhancement).
- Tracing internal database JDBC calls via ByteBuddy bytecode instrumentation (deferred to future persistence telemetry work).

## Architectural Diagrams (C4 Container & Component Sequence)

### C4 Container Diagram
```
+─────────────────────────────────────────────────────────────────────────────+
|                                CLIENT BROWSER / CLI                         |
+──────────────────────────────────────┬──────────────────────────────────────+
                                       │ HTTP GET /api/admin/v1/health
                                       │ (Client traceparent headers ignored)
                                       v
+─────────────────────────────────────────────────────────────────────────────+
|                         HTTP SERVER RUNTIME (:server)                       |
|                                                                             |
|   +─────────────────────────────────────────────────────────────────────+   |
|   |                       API COMPONENT (:api)                          |   |
|   |                                                                     |   |
|   |   +──────────────────+               +──────────────────────────+   |   |
|   |   | TracingDirective | ─────────────>| DefaultAdminRoute        |   |   |
|   |   | (Generates Root  |               | (Constructs command with |   |   |
|   |   |  Span & Header)  |               |  TraceContext)           |   |   |
|   |   +────────┬─────────+               +─────────────┬────────────+   |   |
|   |            │                                       │ (typed ask)|   |   |
|   |            v                                       v            |   |   |
|   |   +──────────────────+               +──────────────────────────+   |   |
|   |   | Response Header  |               | HealthCheckActor         |   |   |
|   |   | Injector         |               | (Behaviors.withMdc)      |   |   |
|   |   +──────────────────+               +─────────────┬────────────+   |   |
|   +────────────┬───────────────────────────────────────┼────────────────+   |
|                │                                       │                    |
|                v                                       v                    |
|   +─────────────────────────────────────────────────────────────────────+   |
|   |                   COMMON COMPONENT (:common)                        |   |
|   |                                                                     |   |
|   |   +──────────────────+               +──────────────────────────+   |   |
|   |   | TraceContext     |               | TelemetryModule          |   |   |
|   |   | (Immutable Record|               | (OpenTelemetry / Tracer  |   |   |
|   |   +──────────────────+               |  Guice Singletons)       |   |   |
|   |                                      +─────────────┬────────────+   |   |
|   +────────────────────────────────────────────────────┼────────────────+   |
|                                                        │                    |
|                                                        v                    |
|   +─────────────────────────────────────────────────────────────────────+   |
|   |                   LOGGING SUBSYSTEM (Logback / SLF4J)               |   |
|   |                                                                     |   |
|   |   +─────────────────────────────────────────────────────────────+   |   |
|   |   | OpenTelemetryAppender (MDC Wrapper)                         |   |   |
|   |   | Pattern: %d [%thread] %-5level %logger [trace_id=%X span_id=%X] |   |
|   |   +─────────────────────────────────────────────────────────────+   |   |
|   +─────────────────────────────────────────────────────────────────────+   |
+─────────────────────────────────────────────────────────────────────────────+
```

### C4 Component Sequence Diagram
```
Client             TracingDirective       DefaultAdminRoute     HealthCheckActor       Logback
  │                        │                     │                     │                  │
  │──GET /api/admin/v1/───>│                     │                     │                  │
  │   health               │──startSpan()────────┼─────────────────────┼─────────────────>│ (MDC updated)
  │                        │  (create root trace)│                     │                  │
  │                        │──handleRequest─────>│                     │                  │
  │                        │                     │──ask(CheckHealth)──>│                  │
  │                        │                     │  (with TraceContext)│──withMdc()──────>│
  │                        │                     │                     │──logger.info()──>│ [trace_id=t1
  │                        │                     │                     │                  │  span_id=s1]
  │                        │                     │<──Healthy───────────│                  │
  │                        │<──mapResponse───────│                     │                  │
  │                        │──attachHeader()─────┼─────────────────────┼──────────────────│
  │                        │  (traceparent: 00..)│                     │                  │
  │                        │──span.end()─────────┼─────────────────────┼──────────────────│
  │<──200 OK + traceparent─│                     │                     │                  │
```

## Decisions

### Decision 1: Library-Based OpenTelemetry SDK over Java Agent
- **Choice**: Incorporate OpenTelemetry as compiled library dependencies managed by Guice rather than requiring an external `opentelemetry-javaagent.jar`.
- **Alternatives Considered**:
  - `opentelemetry-javaagent.jar`: Rejected because it relies on runtime bytecode modification, requires specialized JVM startup flags in Docker and development environments, and complicates deterministic test execution.
- **Rationale**: Direct SDK integration guarantees strict compile-time type safety, zero external jar runtime dependencies, and seamless compatibility with Pekko Typed and Guice.

### Decision 2: Inbound Trace Isolation (Strict Root Span Generation)
- **Choice**: Every incoming HTTP request initiates a brand-new OpenTelemetry root span. Client-supplied `traceparent` headers are ignored and discarded.
- **Alternatives Considered**:
  - Extracting parent context via `W3CTraceContextPropagator`: Rejected per security requirements to prevent untrusted callers from forging trace contexts or manipulating internal telemetry graphs before external perimeter controls (e.g., HAProxy) are active.
- **Rationale**: Enforces deterministic, untampered trace generation at the application edge.

### Decision 3: Egress Response Header Injection for Debugging
- **Choice**: All HTTP responses emitted by the server attach the W3C `traceparent` header reflecting the request's active trace ID and span ID.
- **Alternatives Considered**:
  - Internal-only tracing without response headers: Rejected because client callers, Cucumber tests, and operators require the trace ID to correlate HTTP responses with server log records.
- **Rationale**: Maximizes operational observability during development and debugging, with the architectural understanding that edge proxies (HAProxy) may strip or transform these headers in production.

### Decision 4: Logback MDC Integration via `opentelemetry-logback-mdc-1.0`
- **Choice**: Use the OpenTelemetry Logback MDC appender wrapper in `logback.xml` and `logback-test.xml` to inject `trace_id` and `span_id` directly into the SLF4J MDC.
- **Alternatives Considered**:
  - Manual MDC calls (`MDC.put("trace_id", ...)`) in every route and actor: Rejected due to repetitive boilerplate and risk of MDC leakage across recycled thread pools.
  - OpenTelemetry Log Appender (`opentelemetry-logback-appender-1.0`): Rejected for this phase because it is designed to export OpenTelemetry `LogRecord` instances to external collectors rather than enriching local stdout log lines.
- **Rationale**: Provides automatic, thread-safe trace correlation in standard console output with minimal overhead.

### Decision 5: Actor Context Transfer via Command Payload and `Behaviors.withMdc`
- **Choice**: Pass an immutable `TraceContext(String traceId, String spanId)` record inside actor command payloads and decorate actor behaviors with Pekko Typed's native `Behaviors.withMdc(...)`.
- **Alternatives Considered**:
  - ThreadLocal propagation across actor mailboxes: Rejected because Pekko Typed mailboxes decouple enqueueing from execution across thread pools, causing context loss or cross-request pollution.
- **Rationale**: Functional message passing preserves actor encapsulation, immutability, and thread safety.

## Risks / Trade-offs

- **[Risk] Thread context loss in asynchronous Pekko streams** -> **Mitigation**: The `TracingDirective` manages span lifecycle using Pekko HTTP `mapResponse` and `extractRequest`, ensuring the span is ended deterministically regardless of whether the route completes normally or exceptionally.
- **[Risk] Performance overhead from span creation on high-frequency endpoints (e.g. `/api/admin/v1/health`)** -> **Mitigation**: OpenTelemetry in-memory trace generation is lightweight (sub-microsecond), and no external network I/O is performed.
- **[Risk] Log pattern clutter when trace context is absent (e.g. startup logs)** -> **Mitigation**: Logback pattern `%X{trace_id}` and `%X{span_id}` evaluate to empty strings when MDC keys are unset, leaving startup logs clean.

## Migration Plan

Additive changes with no database migration required:
1. `parent` & `libs.versions.toml`: Define OpenTelemetry dependencies and BOMs.
2. `:common`: Implement `TraceContext` and `TelemetryModule`.
3. `:api`: Implement `TracingDirective`, integrate into `DefaultRootRoute`, update actor commands and `openapi.yaml`.
4. `:server` & `:test`: Update `logback.xml` and `logback-test.xml` with `OpenTelemetryAppender`.
5. `:integration`: Add Cucumber acceptance scenarios verifying response headers and log output.

## Open Questions

None. All architectural decisions (MDC focus, root span generation, header injection, actor withMdc propagation) have been verified and agreed upon.
