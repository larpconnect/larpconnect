# ADR Review Manifest

- Status: completed
- Review date: 2026-09-24

## Review Summary

ADR review completed for this change. The change introduces compiled OpenTelemetry SDK dependencies, root span generation at the HTTP boundary, response header injection, Logback MDC integration, and Pekko Typed actor message tracing.

## In-Force ADRs Reviewed

- `adr/0001-pekko-http-runtime-architecture.md`: Pekko HTTP runtime and coordinated shutdown.
- `adr/0002-dropwizard-healthcheck-actor-pattern.md`: Health check actor pattern.
- `adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md`: Sealed DAO and persistence boundary.
- `adr/0008-module-level-singleton-scoping-convention.md`: Module-level singleton scoping conventions.
- `adr/0010-admin-schema-and-multi-tenant-studio-routing.md`: Admin routing and multi-tenant routes.
- `adr/0014-guice-injector-hardening-and-strict-bindings.md`: Guice injector hardening and strict constructor injection.

## New Durable ADRs Created

- `adr/0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md`: Establishes the library-based OpenTelemetry SDK, Logback MDC correlation appender, W3C `traceparent` response header generation, and Pekko Typed actor `withMdc` correlation.
