# ADR Review Manifest

- Status: completed
- Review date: 2026-09-29

## Review Summary

ADR review completed for this change. The introduction of tenanted physical locations and subordinate addresses establishes durable architectural patterns for CTI entity subtypes, 1:N subordinate child tables, PostGIS spatial data integration, and GeoJSON serialization in Project **Njall**, codified in repository ADR-0029.

## In-Force ADRs Reviewed

- [0001: Pekko HTTP Runtime Architecture](../../../../adr/0001-pekko-http-runtime-architecture.md)
- [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0008: Module-Level Singleton Scoping Convention](../../../../adr/0008-module-level-singleton-scoping-convention.md)
- [0010: Admin Schema and Multi-Tenant Studio Routing Architecture](../../../../adr/0010-admin-schema-and-multi-tenant-studio-routing.md)
- [0011: ArchUnit Injected Constructor Visibility and Package Dependency Rules](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md)
- [0014: Guice Injector Hardening and Strict Bindings](../../../../adr/0014-guice-injector-hardening-and-strict-bindings.md)
- [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)
- [0027: Common Table Inheritance and Tenanted Links Architecture](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md)
- [0028: Admin Subpackage Decomposition and Module Hierarchy](../../../../adr/0028-admin-subpackage-decomposition-and-module-hierarchy.md)

## New Durable ADRs Created

- [0029: Tenanted Locations and Addresses Architecture](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md): Establishes CTI inheritance for `locations` under `entities`, subordinate 1:N `addresses` child persistence, PostGIS `geography(Point, 4326)` integration, RFC 7946 GeoJSON Point representation, and hierarchical REST routes.
