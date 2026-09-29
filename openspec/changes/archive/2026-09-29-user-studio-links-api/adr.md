# ADR Review Manifest

- Status: completed
- Review date: 2026-09-28

## Review Summary

ADR review completed for this change. The introduction of Common Table Inheritance (CTI) for user-space entities and tenanted **Link** management represents a foundational architectural commitment that governs future entity modeling across **Njall**. A new durable Architecture Decision Record has been created.

## In-Force ADRs Reviewed

- [0001: Pekko HTTP Runtime Architecture](../../../../adr/0001-pekko-http-runtime-architecture.md)
- [0003: Data Module and Flyway Migration Architecture](../../../../adr/0003-data-module-and-flyway-migration-architecture.md)
- [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0006: Package-Level NonNull Defaults and Nullability Conventions](../../../../adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md)
- [0008: Module-Level Singleton Scoping Convention](../../../../adr/0008-module-level-singleton-scoping-convention.md)
- [0009: Hibernate Session Factory Provider Injection and ArchUnit Enforcement](../../../../adr/0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md)
- [0010: Admin Schema and Multi-Tenant Studio Routing Architecture](../../../../adr/0010-admin-schema-and-multi-tenant-studio-routing.md)
- [0011: ArchUnit Injected Constructor Visibility and Package Dependency Rules](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md)
- [0014: Guice Injector Hardening and Strict Bindings](../../../../adr/0014-guice-injector-hardening-and-strict-bindings.md)
- [0021: ArchUnit Record Factory Prohibition and Pure Data Carriers](../../../../adr/0021-archunit-record-factory-prohibition-and-pure-data-carriers.md)
- [0022: ArchUnit Immutable Records and ErrorProne Enforcement](../../../../adr/0022-archunit-immutable-records-and-errorprone-enforcement.md)
- [0023: Record Component NonNull Invariant and Boundary Normalization](../../../../adr/0023-record-component-nonnull-invariant-and-boundary-normalization.md)
- [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)
- [0026: Caffeine-Cached Studio Lookup Service](../../../../adr/0026-caffeine-cached-studio-lookup-service.md)

## New Durable ADRs Created

- [0027: Common Table Inheritance and Tenanted Links Architecture](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md): Establishes `njall_users.entities` as the central CTI supertype table with composite primary key `(tenant_id, id)`, mandates subtype tables share composite keys referencing `entities` with `ON DELETE CASCADE`, centralizes soft-deletion on `entities.deleted_on`, and enforces transaction-scoped PostgreSQL Row-Level Security via `set_config('app.tenant_id', ...)`.
