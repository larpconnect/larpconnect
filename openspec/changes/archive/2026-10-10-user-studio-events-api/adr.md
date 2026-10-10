# ADR Review Manifest

- Status: completed
- Review date: 2026-10-10

## Review Summary

ADR review completed for this change. The technical design was reviewed against in-force repository ADRs governing Common Table Inheritance, tenant Row-Level Security, Pekko HTTP routing, and Hibernate DAO architecture. A new repository-level ADR was created to formalize the tenanted events model and composite location association.

## In-Force ADRs Reviewed

- [0001: Pekko HTTP Runtime Architecture](../../../../adr/0001-pekko-http-runtime-architecture.md)
- [0003: Data Module and Flyway Migration Architecture](../../../../adr/0003-data-module-and-flyway-migration-architecture.md)
- [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0006: Package-Level NonNull Defaults and Nullability Conventions](../../../../adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md)
- [0008: Module-Level Singleton Scoping Convention](../../../../adr/0008-module-level-singleton-scoping-convention.md)
- [0014: Guice Injector Hardening and Strict Bindings](../../../../adr/0014-guice-injector-hardening-and-strict-bindings.md)
- [0021: ArchUnit Record Factory Prohibition and Pure Data Carriers](../../../../adr/0021-archunit-record-factory-prohibition-and-pure-data-carriers.md)
- [0022: ArchUnit Immutable Records and ErrorProne Enforcement](../../../../adr/0022-archunit-immutable-records-and-errorprone-enforcement.md)
- [0023: Record Component NonNull Invariant and Boundary Normalization](../../../../adr/0023-record-component-nonnull-invariant-and-boundary-normalization.md)
- [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)
- [0026: Caffeine Cached Studio Lookup Service](../../../../adr/0026-caffeine-cached-studio-lookup-service.md)
- [0027: Common Table Inheritance and Tenanted Links Architecture](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md)
- [0029: Tenanted Locations and Addresses Architecture](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md)
- [0030: DAO Naming Conventions and Entity Immutability Standards](../../../../adr/0030-dao-naming-conventions-and-entity-immutability-standards.md)
- [0032: Package Size Limits and Subpackage Decomposition](../../../../adr/0032-package-size-limits-and-subpackage-decomposition.md)
- [0033: Federated Hashtags and Case-Insensitive Functional Indexing](../../../../adr/0033-federated-hashtags-and-case-insensitive-functional-indexing.md)
- [0034: Subordinate Reactions and Materialized Aggregation Views](../../../../adr/0034-subordinate-reactions-and-materialized-aggregation-views.md)

## New Durable ADRs Created

- [0035: Tenanted Events and Location Associations Architecture](../../../../adr/0035-tenanted-events-and-location-associations-architecture.md)
