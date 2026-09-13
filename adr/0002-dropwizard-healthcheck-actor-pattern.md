# 0002: Dropwizard HealthCheck and Pekko Typed Health Actor Pattern

## Status

Accepted

## Date

2026-09-13

## Context

Project Njall requires an administrative health check endpoint (`/api/admin/v1/health`) to evaluate runtime health. As additional modules (persistence in `:data`, messaging in `:queue`) are introduced, they will need to register subsystem health indicators without coupling to the HTTP routing layer or creating circular dependencies. Furthermore, evaluating whether the Apache Pekko runtime is functioning properly requires verifying that its message dispatching and mailbox processing are actively responsive, rather than merely inspecting static JVM memory or passive lifecycle flags.

## Considered Options

- **Option 1: Dropwizard Metrics HealthChecks with Guice Multibinder & Pekko Typed Actor Wrapper** (Selected)
- **Option 2: Direct Synchronous Method Invocation on Route Thread** (Rejected: fails to verify that the Pekko actor dispatcher and mailbox queues are responsive)
- **Option 3: Custom Ad-Hoc Health Indicator Interface** (Rejected: reinvents standardized, battle-tested Dropwizard health check contracts)

## Decision

We will standardize subsystem health reporting using Dropwizard Metrics `HealthCheck` integrated via Google Guice and Apache Pekko Typed:
1. **Centralized Health Multibinder in `:common`**: `:common` manages a Guice `Multibinder<HealthCheck>` and provides a centralized `HealthCheckRegistry`. Any module can contribute a `HealthCheck` implementation to this multibinder.
2. **Active Actor Probe in `:api`**: `:api` implements `PekkoHealthCheck` and wraps registry evaluation inside a stateless `HealthCheckActor` (Pekko Typed). The HTTP route executes an active `AskPattern.ask(...)` with a 2-second timeout, ensuring the actor system is responsive.
3. **Base Path Routing**: The route is mounted under `/api/admin/v1/health`, establishing the `/api/admin` base path convention for operational and administrative endpoints.

## Consequences

- **Positive**: Subsystems cleanly register health checks without circular dependencies; actor responsiveness is verified end-to-end; future checks (PostgreSQL, RabbitMQ) plug into the existing multibinder seamlessly.
- **Negative**: Adds a minor external dependency (`metrics-healthchecks`); active ask pattern requires timeout tuning under extreme CPU saturation.
