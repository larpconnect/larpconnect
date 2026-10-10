# ADR Review Manifest

- Status: completed
- Review date: 2026-10-09

## Review Summary

ADR review completed for this change. A new durable architectural decision was introduced and documented in repository ADR 0034.

## In-Force ADRs Reviewed

- [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)
- [0027: Common Table Inheritance and Tenanted Links Architecture](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md)
- [0029: Tenanted Locations and Addresses Architecture](../../../../adr/0029-tenanted-locations-and-addresses-architecture.md)
- [0033: Federated Hashtags via Two-Tier Common Table Inheritance and Case-Insensitive Functional Indexing](../../../../adr/0033-federated-hashtags-and-case-insensitive-functional-indexing.md)

## New Durable ADRs Created

- [0034: Subordinate Reactions and Materialized Aggregation Views](../../../../adr/0034-subordinate-reactions-and-materialized-aggregation-views.md): Establishes `njall_users.reactions` subordinate table architecture, PostgreSQL Row-Level Security policies, and `njall_users.reaction_counts` materialized view with non-blocking concurrent refresh support.
