# ADR Review Manifest

- Status: completed
- Review date: 2026-10-01

## Review Summary

ADR review completed for this change. The architectural convention of orchestrating an ephemeral test data seeding container (`seed`) within Docker Compose using `postgis/postgis:18-3.6-alpine`, authenticating as `njall_admin`, executing modular idempotent SQL fixtures from `docker/postgres/seed/`, and enforcing naming conventions ("Larp" and "example"/"fake") has been codified in repository-level ADR 0031.

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
- [0016-database-trust-authentication-opt-in.md](../../../../adr/0016-database-trust-authentication-opt-in.md)
- [0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md](../../../../adr/0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md)
- [0018-caffeine-cached-database-health-check.md](../../../../adr/0018-caffeine-cached-database-health-check.md)
- [0019-static-nullity-enforcement-and-constructor-streamlining.md](../../../../adr/0019-static-nullity-enforcement-and-constructor-streamlining.md)
- [0020-codebase-wide-null-check-and-npe-test-elimination.md](../../../../adr/0020-codebase-wide-null-check-and-npe-test-elimination.md)
- [0021-archunit-record-factory-prohibition-and-pure-data-carriers.md](../../../../adr/0021-archunit-record-factory-prohibition-and-pure-data-carriers.md)
- [0022-archunit-immutable-records-and-errorprone-enforcement.md](../../../../adr/0022-archunit-immutable-records-and-errorprone-enforcement.md)
- [0023-record-component-nonnull-invariant-and-boundary-normalization.md](../../../../adr/0023-record-component-nonnull-invariant-and-boundary-normalization.md)
- [0024-haproxy-ingress-reverse-proxy-and-network-segmentation.md](../../../../adr/0024-haproxy-ingress-reverse-proxy-and-network-segmentation.md)
- [0025-user-space-studios-api-and-tenant-row-level-security.md](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)
- [0026-caffeine-cached-studio-lookup-service.md](../../../../adr/0026-caffeine-cached-studio-lookup-service.md)
- [0027-common-table-inheritance-and-tenanted-links-architecture.md](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md)
- [0028-admin-subpackage-decomposition-and-module-hierarchy.md](../../../../adr/0028-admin-subpackage-decomposition-and-module-hierarchy.md)
- [0029-tenanted-locations-and-addresses-architecture.md](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md)
- [0030-dao-naming-conventions-and-entity-immutability-standards.md](../../../../adr/0030-dao-naming-conventions-and-entity-immutability-standards.md)

## New Durable ADRs Created

- [0031-docker-compose-test-data-seeding-and-testing-kit.md](../../../../adr/0031-docker-compose-test-data-seeding-and-testing-kit.md): Codifies ephemeral `seed` service in Docker Compose, `njall_admin` role execution, modular SQL test fixtures in `docker/postgres/seed/`, deterministic UUID idempotency, and test entity naming conventions.
