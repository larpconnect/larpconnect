# ADR Review Manifest

- Status: completed
- Review date: 2026-09-25

## Review Summary

ADR review completed for this change. The change broadens compile-time nullity enforcement and eliminates redundant runtime null checks and NPE test methods across configuration classes, service factories, DAOs, and API directives throughout the entire codebase. This policy supersedes ADR-0019 to apply codebase-wide.

## In-Force ADRs Reviewed

- [0001-pekko-http-runtime-architecture.md](../../../../adr/0001-pekko-http-runtime-architecture.md)
- [0002-dropwizard-healthcheck-actor-pattern.md](../../../../adr/0002-dropwizard-healthcheck-actor-pattern.md)
- [0003-data-module-and-flyway-migration-architecture.md](../../../../adr/0003-data-module-and-flyway-migration-architecture.md)
- [0004-picocli-subcommand-architecture.md](../../../../adr/0004-picocli-subcommand-architecture.md)
- [0005-sealed-dao-and-hibernate-dual-session-architecture.md](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0007-docker-compose-and-container-orchestration.md](../../../../adr/0007-docker-compose-and-container-orchestration.md)
- [0008-module-level-singleton-scoping-convention.md](../../../../adr/0008-module-level-singleton-scoping-convention.md)
- [0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md](../../../../adr/0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md)
- [0010-admin-schema-and-multi-tenant-studio-routing.md](../../../../adr/0010-admin-schema-and-multi-tenant-studio-routing.md)
- [0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md)
- [0012-cli-command-option-null-encapsulation-and-optional-accessors.md](../../../../adr/0012-cli-command-option-null-encapsulation-and-optional-accessors.md)
- [0013-domain-query-filter-enums-and-dedicated-route-response-mappers.md](../../../../adr/0013-domain-query-filter-enums-and-dedicated-route-response-mappers.md)
- [0014-guice-injector-hardening-and-strict-bindings.md](../../../../adr/0014-guice-injector-hardening-and-strict-bindings.md)
- [0015-docker-compose-secret-derivation-and-isolation.md](../../../../adr/0015-docker-compose-secret-derivation-and-isolation.md)
- [0016-database-trust-authentication-opt-in.md](../../../../adr/0016-database-trust-authentication-opt-in.md)
- [0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md](../../../../adr/0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md)
- [0018-caffeine-cached-database-health-check.md](../../../../adr/0018-caffeine-cached-database-health-check.md)
- [0019-static-nullity-enforcement-and-constructor-streamlining.md](../../../../adr/0019-static-nullity-enforcement-and-constructor-streamlining.md)

## New Durable ADRs Created

- [0020-codebase-wide-null-check-and-npe-test-elimination.md](../../../../adr/0020-codebase-wide-null-check-and-npe-test-elimination.md): Establishes universal elimination of defensive runtime null checks and obsolete NPE unit tests across all methods, constructors, factories, DAOs, and directives unless parameters are explicitly annotated with `@Nullable`. Supersedes ADR-0019.
