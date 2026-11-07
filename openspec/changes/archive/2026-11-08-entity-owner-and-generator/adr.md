# ADR Review Manifest

- Status: completed
- Review date: 2026-11-07

## Review Summary

ADR review completed for this change. A new durable architectural decision record has been established under `adr/0039-entity-owner-and-generator-provenance.md` defining the universal `njall_users.entities.owner` **Actor** provenance column, consolidation of `njall_users.actors.owner_id` (amending ADR-0038 while preserving `njall_users.collections.owner_id` from ADR-0037), application-layer **DAO** validation of active `'Actor'` ownership, and the **ActivityStreams** `generator` URI contract across all **CTI** **Entity** endpoints.

## In-Force ADRs Reviewed

- [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0023: Direct Primary Key Session Lookups and Safe Coordinate Parsing](../../../../adr/0023-direct-primary-key-session-lookups-and-safe-coordinate-parsing.md)
- [0024: Entity Immutability and Query Centralization](../../../../adr/0024-entity-immutability-and-query-centralization.md)
- [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)
- [0027: Common Table Inheritance and Tenanted Links Architecture](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md)
- [0029: Locations and Addresses Spatial CTI Architecture](../../../../adr/0029-locations-and-addresses-spatial-cti-architecture.md)
- [0033: Hashtags Three-Level CTI and Dual-ID Resolution](../../../../adr/0033-hashtags-three-level-cti-and-dual-id-resolution.md)
- [0035: Tenanted Events Architecture](../../../../adr/0035-tenanted-events-architecture.md)
- [0036: Tenanted Individuals Architecture](../../../../adr/0036-tenanted-individuals-architecture.md)
- [0037: Tenanted Collections Architecture](../../../../adr/0037-tenanted-collections-architecture.md)
- [0038: Tenanted Actors and Federated Endpoints Architecture](../../../../adr/0038-tenanted-actors-and-federated-endpoints-architecture.md)

## New Durable ADRs Created

- [0039: Universal Entity Owner Provenance and ActivityStreams Generator Contract](../../../../adr/0039-entity-owner-and-generator-provenance.md)

