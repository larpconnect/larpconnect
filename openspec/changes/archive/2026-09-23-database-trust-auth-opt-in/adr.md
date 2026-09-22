# ADR Review Manifest

- Status: completed
- Review date: 2026-09-23

## Review Summary

ADR review completed for this change. The architectural convention requiring explicit opt-in for database trust authentication (`trust-auth = true`), fail-fast credential validation during configuration ingestion, and CLI migration trigger support has been codified in repository-level ADR 0016, amending ADR 0003 and ADR 0005.

## In-Force ADRs Reviewed

- [0001-pekko-http-runtime-architecture.md](../../../../adr/0001-pekko-http-runtime-architecture.md)
- [0002-dropwizard-healthcheck-actor-pattern.md](../../../../adr/0002-dropwizard-healthcheck-actor-pattern.md)
- [0003-data-module-and-flyway-migration-architecture.md](../../../../adr/0003-data-module-and-flyway-migration-architecture.md)
- [0004-picocli-subcommand-architecture.md](../../../../adr/0004-picocli-subcommand-architecture.md)
- [0005-sealed-dao-and-hibernate-dual-session-architecture.md](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0006-package-level-nonnull-defaults-and-nullability-conventions.md](../../../../adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md)
- [0007-docker-compose-and-container-orchestration.md](../../../../adr/0007-docker-compose-and-container-orchestration.md)
- [0008-module-level-singleton-scoping-convention.md](../../../../adr/0008-module-level-singleton-scoping-convention.md)
- [0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md](../../../../adr/0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md)
- [0010-admin-schema-and-multi-tenant-studio-routing.md](../../../../adr/0010-admin-schema-and-multi-tenant-studio-routing.md)
- [0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md)
- [0012-cli-command-option-null-encapsulation-and-optional-accessors.md](../../../../adr/0012-cli-command-option-null-encapsulation-and-optional-accessors.md)
- [0013-domain-query-filter-enums-and-dedicated-route-response-mappers.md](../../../../adr/0013-domain-query-filter-enums-and-dedicated-route-response-mappers.md)
- [0014-guice-injector-hardening-and-strict-bindings.md](../../../../adr/0014-guice-injector-hardening-and-strict-bindings.md)
- [0015-docker-compose-secret-derivation-and-isolation.md](../../../../adr/0015-docker-compose-secret-derivation-and-isolation.md)

## New Durable ADRs Created

- [0016-database-trust-authentication-opt-in.md](../../../../adr/0016-database-trust-authentication-opt-in.md): Codifies mandatory database password validation by default, explicit opt-in for passwordless connections via `trust-auth = true`, test classpath configuration overlay in `:common`, and CLI migration trigger support.
