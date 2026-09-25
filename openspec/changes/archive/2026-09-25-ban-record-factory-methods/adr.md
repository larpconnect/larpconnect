# ADR Review Manifest

- Status: completed
- Review date: 2026-09-25

## Review Summary

ADR review completed for this change. Architectural invariant enforcement and record purity were evaluated against existing in-force architectural decisions, establishing a new durable ADR.

## In-Force ADRs Reviewed

- [0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md](../../../../adr/0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md): Established ArchUnit architectural verification in `:integration`.
- [0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md): Mandated non-public visibility for `@Inject`/`@AssistedInject` constructors and down-or-out package dependencies.
- [0014-guice-injector-hardening-and-strict-bindings.md](../../../../adr/0014-guice-injector-hardening-and-strict-bindings.md): Enforced strict Guice module bindings and explicit provider declarations.
- [0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md](../../../../adr/0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md): Defined OpenTelemetry root span creation and Logback MDC correlation across the **API plane**.

## New Durable ADRs Created

- [0021-archunit-record-factory-prohibition-and-pure-data-carriers.md](../../../../adr/0021-archunit-record-factory-prohibition-and-pure-data-carriers.md): Mandates that records in `com.larpconnect.njall..` must not declare static factory methods returning the record type or `Optional<Record>`, replaces factory methods with constructors, extracts configuration parsing to Guice providers and assisted injection, and introduces `ApiCall<T>` as a generic telemetry command envelope.
