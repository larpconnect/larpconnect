# ADR Review Manifest

- Status: completed
- Review date: 2026-11-06

## Review Summary

ADR review completed for this change. Upgrading `org.apache.pekko:pekko-http-bom_3` from `2.0.0-M1` to `2.0.0-M2` in `gradle/libs.versions.toml` is a routine milestone dependency upgrade that preserves the existing **Njall** runtime architecture and **Module** boundaries; no major durable architectural decisions were introduced and no new repository-level ADR files were created.

## In-Force ADRs Reviewed

- [0001: Pekko HTTP Runtime Architecture and Multimodule Layout](../../../adr/0001-pekko-http-runtime-architecture.md): Establishes Apache Pekko Typed, Pekko HTTP, and Google Guice across `:common`, `:api`, `:server`, `:test`, and `:integration`.
- [0002: Dropwizard HealthCheck Actor Pattern](../../../adr/0002-dropwizard-healthcheck-actor-pattern.md): Governs health probe actors and HTTP endpoints.
- [0017: OpenTelemetry SDK and Logback MDC Tracing Architecture](../../../adr/0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md): Governs `TracingDirective` and HTTP trace propagation.

## New Durable ADRs Created

- None - no major durable architectural decisions were introduced and no new repository-level ADR files were created.

