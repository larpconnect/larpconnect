# ADR Review Manifest

- Status: completed
- Review date: 2026-09-22

## Review Summary

ADR review completed for this change. Durable architectural decisions governing constructor injection visibility and package dependency topology have been recorded in repository-level ADR 0011.

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

## New Durable ADRs Created

- [0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md): Mandates non-public visibility for `@Inject`/`@AssistedInject` constructors, establishes downward/outward package dependency invariants, and relocates `RouteProvider` to `com.larpconnect.njall.api.http`.
