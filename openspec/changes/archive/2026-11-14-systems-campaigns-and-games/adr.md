# ADR Review Manifest

- Status: completed
- Review date: 2026-11-13

## Review Summary

ADR review completed for this change. A new durable architectural decision record has been created at [0041: Tenanted Systems, Campaigns, and Games Two-Level CTI and Domain Decomposition Architecture](../../../../adr/0041-tenanted-systems-campaigns-and-games-architecture.md), amending [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0032: Package Size Limits and Subpackage Decomposition](../../../../adr/0032-package-size-limits-and-subpackage-decomposition.md), and [0035: Tenanted Events and Location Associations Architecture](../../../../adr/0035-tenanted-events-and-location-associations-architecture.md) to establish 2-level Common Table Inheritance for tenanted **Games** (`games` -> `events` -> `entities`) alongside **System** (**GameSystem**) and **Campaigns** CTI subtypes and `njall_users.tevent_type`, expose disjoint flat AIP-124 peer collections with canonical AIP-122 studio resource URIs, unseal `DatabaseObject` across `com.larpconnect.njall.data.domain.{common,admin,servers,studios}`, decompose `studios.events`/`systems`/`campaigns`/`games` **DAO** packages, and aggregate studio HTTP subroutes via a `@StudioSubRoute` Guice multibinder.

## In-Force ADRs Reviewed

- [0001: Pekko HTTP Runtime Architecture and Multimodule Layout](../../../../adr/0001-pekko-http-runtime-architecture.md)
- [0002: Dropwizard HealthCheck and Pekko Typed Health Actor Pattern](../../../../adr/0002-dropwizard-healthcheck-actor-pattern.md)
- [0003: Data Module and Flyway Migration Architecture](../../../../adr/0003-data-module-and-flyway-migration-architecture.md) *(Decision 4 superseded by ADR-0004)*
- [0004: Picocli Command-Line Interface and Subcommand Architecture](../../../../adr/0004-picocli-subcommand-architecture.md)
- [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0007: Docker Compose Multi-Container Orchestration and Staged Packaging](../../../../adr/0007-docker-compose-and-container-orchestration.md)
- [0008: Module-Level Singleton Scoping Convention](../../../../adr/0008-module-level-singleton-scoping-convention.md)
- [0009: Hibernate SessionFactory Provider Injection and ArchUnit Enforcement](../../../../adr/0009-hibernate-session-factory-provider-injection-and-archunit-enforcement.md)
- [0010: Admin Schema and Multi-Tenant Studio Routing Architecture](../../../../adr/0010-admin-schema-and-multi-tenant-studio-routing.md)
- [0011: ArchUnit Injected Constructor Visibility and Package Dependency Rules](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md)
- [0012: CLI Command Option Null Encapsulation and Optional Accessors](../../../../adr/0012-cli-command-option-null-encapsulation-and-optional-accessors.md)
- [0013: Domain Query Filter Enums and Dedicated Route Response Mappers](../../../../adr/0013-domain-query-filter-enums-and-dedicated-route-response-mappers.md)
- [0014: Guice Injector Hardening and Strict Bindings](../../../../adr/0014-guice-injector-hardening-and-strict-bindings.md)
- [0015: Docker Compose Secret Derivation and Local Environment Isolation](../../../../adr/0015-docker-compose-secret-derivation-and-isolation.md)
- [0016: Database Trust Authentication Opt-in and Fail-Fast Enforcement](../../../../adr/0016-database-trust-authentication-opt-in.md)
- [0017: OpenTelemetry SDK and Logback MDC Tracing Architecture](../../../../adr/0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md)
- [0018: Caffeine Cached Database Health Check](../../../../adr/0018-caffeine-cached-database-health-check.md)
- [0020: Codebase-Wide Null Check and NPE Test Elimination](../../../../adr/0020-codebase-wide-null-check-and-npe-test-elimination.md)
- [0021: ArchUnit Record Factory Prohibition and Pure Data Carriers](../../../../adr/0021-archunit-record-factory-prohibition-and-pure-data-carriers.md)
- [0022: ArchUnit Immutable Records and ErrorProne Enforcement](../../../../adr/0022-archunit-immutable-records-and-errorprone-enforcement.md)
- [0023: Record Component Non-Null Invariant and Boundary Normalization](../../../../adr/0023-record-component-nonnull-invariant-and-boundary-normalization.md)
- [0024: HAProxy Ingress Reverse Proxy and Network Segmentation](../../../../adr/0024-haproxy-ingress-reverse-proxy-and-network-segmentation.md)
- [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)
- [0026: Caffeine Cached Studio Lookup Service](../../../../adr/0026-caffeine-cached-studio-lookup-service.md)
- [0027: Common Table Inheritance and Tenanted Links Architecture](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md)
- [0028: Admin Subpackage Decomposition and Module Hierarchy](../../../../adr/0028-admin-subpackage-decomposition-and-module-hierarchy.md)
- [0029: Tenanted Locations and Addresses Architecture](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md)
- [0030: DAO Naming Conventions and Entity Immutability Standards](../../../../adr/0030-dao-naming-conventions-and-entity-immutability-standards.md)
- [0031: Docker Compose Test Data Seeding and Testing Kit Architecture](../../../../adr/0031-docker-compose-test-data-seeding-and-testing-kit.md)
- [0032: Package Size Limits and Subpackage Decomposition](../../../../adr/0032-package-size-limits-and-subpackage-decomposition.md)
- [0033: Federated Hashtags via Two-Tier Common Table Inheritance and Case-Insensitive Functional Indexing](../../../../adr/0033-federated-hashtags-and-case-insensitive-functional-indexing.md)
- [0034: Subordinate Reactions and Materialized Aggregation Views](../../../../adr/0034-subordinate-reactions-and-materialized-aggregation-views.md)
- [0035: Tenanted Events and Location Associations Architecture](../../../../adr/0035-tenanted-events-and-location-associations-architecture.md)
- [0036: Tenanted Individuals Architecture](../../../../adr/0036-tenanted-individuals-architecture.md)
- [0037: Tenanted Collections Architecture](../../../../adr/0037-tenanted-collections-architecture.md)
- [0038: Tenanted Actors and Federated Endpoints Architecture](../../../../adr/0038-tenanted-actors-and-federated-endpoints-architecture.md)
- [0040: Actor-Parented Collections Routes and Entity Generator Column](../../../../adr/0040-actor-parented-collections-and-entity-generator-column.md)

## New Durable ADRs Created

- [0041: Tenanted Systems, Campaigns, and Games Two-Level CTI and Domain Decomposition Architecture](../../../../adr/0041-tenanted-systems-campaigns-and-games-architecture.md)
