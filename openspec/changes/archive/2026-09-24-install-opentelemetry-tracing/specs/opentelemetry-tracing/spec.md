## ADDED Requirements

### Requirement: Generate Root Span for Every Inbound HTTP Request
The system SHALL intercept every inbound HTTP request across all routes in the **API plane** and generate a new root OpenTelemetry `Span` with a globally unique trace ID and span ID.

#### Scenario: Inbound request without tracing headers generates fresh root span
- **GIVEN** the **Server** is running
- **WHEN** a client sends an HTTP request to any registered route without a `traceparent` header
- **THEN** a new OpenTelemetry root span SHALL be created
- **AND** the span kind SHALL be `SERVER`
- **AND** the span name SHALL reflect the HTTP method and route target.

### Requirement: Ignore and Discard Inbound Traceparent Headers
To prevent untrusted distributed trace spoofing, the system SHALL strictly ignore and discard any client-supplied `traceparent` request header, creating a new root trace instead of adopting the client-supplied trace or parent span ID.

#### Scenario: Inbound request containing client traceparent is ignored
- **GIVEN** a client sends an HTTP request containing header `traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`
- **WHEN** the request is processed by the HTTP tracing directive
- **THEN** the system SHALL NOT use `4bf92f3577b34da6a3ce929d0e0e4736` as the trace ID
- **AND** the system SHALL generate a new, distinct 32-character hexadecimal trace ID for the root span.

### Requirement: Attach W3C Traceparent Header to All HTTP Responses
The system SHALL attach a compliant W3C `traceparent` header (`00-{trace_id}-{span_id}-{trace_flags}`) to every outgoing HTTP response across all endpoints for debugging and observability purposes.

#### Scenario: Successful response includes traceparent header
- **GIVEN** a client sends a valid HTTP GET request to `/api/admin/v1/health`
- **WHEN** the server returns an HTTP 200 OK response
- **THEN** the response headers SHALL contain a `traceparent` header
- **AND** the header value SHALL match regex `^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$`
- **AND** the trace ID in the header SHALL match the active OpenTelemetry root span.

#### Scenario: Error response includes traceparent header
- **GIVEN** a client sends an HTTP request that triggers an internal error or validation failure (HTTP 4xx or 5xx)
- **WHEN** the server returns the error response
- **THEN** the response headers SHALL contain the `traceparent` header matching the active trace.

### Requirement: Enrich Logback MDC with Active Trace and Span IDs
The system SHALL populate SLF4J / Logback Mapped Diagnostic Context (MDC) with `trace_id` and `span_id` for all log events emitted during request execution, and format these identifiers in stdout logs.

#### Scenario: Server log outputs active trace and span identifiers
- **GIVEN** an active HTTP request with trace ID `t1` and span ID `s1`
- **WHEN** any component or route logs a message at level INFO, WARN, or ERROR
- **THEN** the formatted log event SHALL include `[trace_id=t1 span_id=s1]`.

### Requirement: Propagate TraceContext to Pekko Typed Actors via withMdc
The system SHALL support transferring an immutable `TraceContext` record into Pekko Typed **Actor** command protocols and decorate **Actor** behaviors with `Behaviors.withMdc` to ensure actor thread logs preserve request trace correlation.

#### Scenario: Actor processing preserves request trace context in logs
- **GIVEN** a route dispatches an **Actor** command carrying a `TraceContext` with trace ID `t1` and span ID `s1`
- **WHEN** the target Pekko Typed **Actor** receives and processes the command
- **THEN** the actor's logging context SHALL contain `trace_id=t1` and `span_id=s1` in its MDC.
