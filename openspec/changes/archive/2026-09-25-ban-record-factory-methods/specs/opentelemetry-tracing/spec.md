## MODIFIED Requirements

### Requirement: Propagate TraceContext to Pekko Typed Actors via withMdc
The system SHALL support transferring an immutable `TraceContext` record into Pekko Typed **Actor** command protocols using the generic `ApiCall<T>` command envelope, and decorate **Actor** behaviors with `Behaviors.withMdc` to extract `TraceContext` from `ApiCall<T>` and ensure actor thread logs preserve request trace correlation.

#### Scenario: Actor processing preserves request trace context in logs
- **GIVEN** a route dispatches an `ApiCall` wrapping an **Actor** command and carrying a `TraceContext` with trace ID `t1` and span ID `s1`
- **WHEN** the target Pekko Typed **Actor** receives and processes the `ApiCall` command envelope
- **THEN** the actor's logging context SHALL contain `trace_id=t1` and `span_id=s1` in its MDC.
