# ADR Review Manifest

- Status: completed
- Review date: 2026-11-13

## Review Summary

ADR review completed for this change. A new durable architectural decision record has been created at [0040: Actor-Parented Collections Routes and Entity Generator Column](../../../../adr/0040-actor-parented-collections-and-entity-generator-column.md), superseding [0039: Universal Entity Owner Provenance and ActivityStreams Generator Contract](../../../../adr/0039-entity-owner-and-generator-provenance.md) and amending [0037: Tenanted Collections Architecture](../../../../adr/0037-tenanted-collections-architecture.md) and [0038: Tenanted Actors and Federated Endpoints Architecture](../../../../adr/0038-tenanted-actors-and-federated-endpoints-architecture.md) to repath tenanted **Collections** routes beneath `/api/studios/{studio-id}/v1/actors/{actor-id}/collections` via `AgentRoute` -> `CollectionsRoute` delegation, bind `{actor-id}` to `njall_users.entities.generator` while retaining optional `ownerId` for semantic **owner** attachment, and rename `njall_users.entities.owner` to `generator` via Flyway migration `V13`.

## In-Force ADRs Reviewed

- [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0011: ArchUnit Injected Constructor Visibility and Package Dependency Rules](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md)
- [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)
- [0027: Common Table Inheritance and Tenanted Links Architecture](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md)
- [0029: Tenanted Locations and Addresses Architecture](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md)
- [0030: DAO Naming Conventions and Entity Immutability Standards](../../../../adr/0030-dao-naming-conventions-and-entity-immutability-standards.md)
- [0031: Docker Compose Test Data Seeding and Testing Kit](../../../../adr/0031-docker-compose-test-data-seeding-and-testing-kit.md)
- [0032: Package Size Limits and Subpackage Decomposition](../../../../adr/0032-package-size-limits-and-subpackage-decomposition.md)
- [0033: Federated Hashtags and Case-Insensitive Functional Indexing](../../../../adr/0033-federated-hashtags-and-case-insensitive-functional-indexing.md)
- [0035: Tenanted Events and Location Associations Architecture](../../../../adr/0035-tenanted-events-and-location-associations-architecture.md)
- [0036: Tenanted Individuals Architecture](../../../../adr/0036-tenanted-individuals-architecture.md)
- [0037: Tenanted Collections Architecture](../../../../adr/0037-tenanted-collections-architecture.md)
- [0038: Tenanted Actors and Federated Endpoints Architecture](../../../../adr/0038-tenanted-actors-and-federated-endpoints-architecture.md)
- [0039: Universal Entity Owner Provenance and ActivityStreams Generator Contract](../../../../adr/0039-entity-owner-and-generator-provenance.md) *(superseded by ADR-0040)*

## New Durable ADRs Created

- [0040: Actor-Parented Collections Routes and Entity Generator Column](../../../../adr/0040-actor-parented-collections-and-entity-generator-column.md)

