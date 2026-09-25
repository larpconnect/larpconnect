# ADR Review Manifest

- Status: completed
- Review date: 2026-09-25

## Review Summary

ADR review completed for this change. A new durable architectural decision was established to supersede ADR-0006 and mandate ErrorProne static nullity enforcement alongside constructor streamlining and Guice assertion cleanup.

## In-Force ADRs Reviewed

- `adr/0001-pekko-http-runtime-architecture.md`
- `adr/0002-dropwizard-healthcheck-actor-pattern.md`
- `adr/0003-data-module-and-flyway-migration-architecture.md`
- `adr/0004-picocli-subcommand-architecture.md`
- `adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md`
- `adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md` (superseded by ADR-0019)
- `adr/0007-docker-compose-and-container-orchestration.md`
- `adr/0008-module-level-singleton-scoping-convention.md`
- `adr/0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md`
- `adr/0010-admin-schema-and-multi-tenant-studio-routing.md`
- `adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md`
- `adr/0012-cli-command-option-null-encapsulation-and-optional-accessors.md`
- `adr/0013-domain-query-filter-enums-and-dedicated-route-response-mappers.md`
- `adr/0014-guice-injector-hardening-and-strict-bindings.md`
- `adr/0015-docker-compose-secret-derivation-and-isolation.md`
- `adr/0016-database-trust-authentication-opt-in.md`
- `adr/0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md`
- `adr/0018-caffeine-cached-database-health-check.md`

## New Durable ADRs Created

- `adr/0019-static-nullity-enforcement-and-constructor-streamlining.md`: Mandates ErrorProne 2.50.0 static compiler enforcement (`AddNullMarkedToPackageInfo`, `ParameterMissingNullable`, `RedundantNullCheck`), eliminates redundant constructor null checks on unannotated fields, retires obsolete constructor NPE unit tests, standardizes `Type @Nullable []` array syntax, and eliminates Guice result null assertions in tests.
