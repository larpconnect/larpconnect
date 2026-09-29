# ADR Review Manifest

- Status: completed
- Review date: 2026-09-29

## Review Summary

ADR review completed for this change. The decomposition of `com.larpconnect.njall.api.admin` into domain subpackages and hierarchical Guice **module** structures establishes durable architectural standards in the **API plane**, codified in repository ADR-0028.

## In-Force ADRs Reviewed

- [0001: Pekko HTTP Runtime Architecture](../../../../adr/0001-pekko-http-runtime-architecture.md)
- [0002: Dropwizard Healthcheck Actor Pattern](../../../../adr/0002-dropwizard-healthcheck-actor-pattern.md)
- [0008: Module-Level Singleton Scoping Convention](../../../../adr/0008-module-level-singleton-scoping-convention.md)
- [0010: Admin Schema and Multi-Tenant Studio Routing](../../../../adr/0010-admin-schema-and-multi-tenant-studio-routing.md)
- [0011: ArchUnit Injected Constructor Visibility and Package Dependency Rules](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md)
- [0014: Guice Injector Hardening and Strict Bindings](../../../../adr/0014-guice-injector-hardening-and-strict-bindings.md)
- [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)

## New Durable ADRs Created

- [0028: Admin Subpackage Decomposition and Module Hierarchy](../../../../adr/0028-admin-subpackage-decomposition-and-module-hierarchy.md): Defines the subpackage decomposition strategy for the **Admin verticle**, including `package-info.java` `@NullMarked` requirements, isolation of shared models in `common`, extracted route classes, and hierarchical Guice **module** installation.
