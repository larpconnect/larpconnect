# ADR Review Manifest

- Status: completed
- Review date: 2026-10-11

## Review Summary

ADR review completed for this change. The technical design aligns with in-force architectural standards while establishing a dedicated durable ADR for tenanted collections.

## In-Force ADRs Reviewed

- [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)
- [0026: Caffeine Cached Studio Lookup Service](../../../../adr/0026-caffeine-cached-studio-lookup-service.md)
- [0027: Common Table Inheritance and Tenanted Links Architecture](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md)
- [0029: Tenanted Locations and Addresses Architecture](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md)
- [0030: DAO Naming Conventions and Entity Immutability Standards](../../../../adr/0030-dao-naming-conventions-and-entity-immutability-standards.md)
- [0035: Tenanted Events and Location Associations Architecture](../../../../adr/0035-tenanted-events-and-location-associations-architecture.md)
- [0036: Tenanted Individuals Architecture](../../../../adr/0036-tenanted-individuals-architecture.md)

## New Durable ADRs Created

- [0037: Tenanted Collections Architecture](../../../../adr/0037-tenanted-collections-architecture.md): Establishes CTI inheritance for collections under `njall_users.entities`, subordinate junction table `collection_items` with integer ordinals, RLS policies, dual representation forms (?form=compressed|expanded), AIP-136 custom methods (:addItem, :removeItem) returning 204 No Content, and deliberate omission of the collection listing endpoint.
