# 0017: OpenTelemetry SDK and Logback MDC Tracing Architecture

- Status: accepted
- Date: 2026-09-24

## Context

In **Njall**, asynchronous HTTP execution across Pekko HTTP directives, worker dispatchers, and Pekko Typed actors currently emits log events without unified correlation identifiers. When debugging complex request flows or concurrent failures across multiple actors, operators cannot correlate log events belonging to a single logical HTTP request.

While OpenTelemetry is the industry standard for distributed tracing, introducing external Java agents (`opentelemetry-javaagent.jar`) at runtime violates repository invariants regarding compile-time dependency predictability, lightweight container packaging, and isolated testing. Furthermore, allowing external clients to supply unverified `traceparent` headers introduces trace spoofing risks prior to edge proxy controls.

## Decision

1. **Compiled OpenTelemetry SDK Dependencies**:
   - The system MUST incorporate OpenTelemetry as compiled library dependencies (`io.opentelemetry:opentelemetry-api`, `io.opentelemetry:opentelemetry-sdk`, and `io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0`) managed via the version catalog and `parent` platform.
   - The application MUST NOT require an external Java agent (`-javaagent`) for core tracing or MDC enrichment.
2. **Inbound Trace Isolation (Root Span Generation)**:
   - Every inbound HTTP request arriving at the **API plane** MUST initiate a new root OpenTelemetry server span.
   - Any client-supplied `traceparent` request header MUST be ignored and discarded.
3. **Egress Trace Correlation via Response Headers**:
   - All HTTP responses (both successful and failure) MUST attach the W3C `traceparent` response header (`00-{trace_id}-{span_id}-{trace_flags}`) corresponding to the active request span to facilitate operational debugging and test verification.
4. **Logback Mapped Diagnostic Context (MDC) Bridge**:
   - The system MUST configure `OpenTelemetryAppender` from `opentelemetry-logback-mdc-1.0` in `logback.xml` and `logback-test.xml` wrapping the stdout appender.
   - The log pattern MUST format `[trace_id=%X{trace_id} span_id=%X{span_id}]`.
5. **Actor Message Protocol Correlation**:
   - Pekko Typed actor message protocols requiring trace context MUST carry an immutable `TraceContext` record.
   - Target actor behaviors MUST be decorated with Pekko Typed `Behaviors.withMdc` to ensure logs emitted from actor worker threads retain the originating request's `trace_id` and `span_id`.

## Consequences

### Positive
- Unified, deterministic request correlation in all stdout and test log streams without manual MDC cleanup.
- Pure library-based architecture with zero bytecode manipulation or external JVM agent requirements.
- Eliminates external trace spoofing while providing clients and tests with `traceparent` response headers for debugging.
- Seamless compatibility with Pekko Typed actors and Guice dependency injection.

### Negative
- Distributed trace propagation from upstream external services is disabled at the application boundary until an authenticated edge gateway (such as HAProxy) is configured.
- Passing `TraceContext` explicitly in actor command ADTs introduces an additional parameter in message protocols where actor tracing is required.
