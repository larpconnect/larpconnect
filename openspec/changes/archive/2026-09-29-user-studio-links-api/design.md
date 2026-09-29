## Context

Project **Njall** uses a multi-single-tenant architecture where external clients access specific gaming organizations (**Studios**) with strict data isolation. Existing architecture established in ADR [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md), ADR [0010: Admin Schema and Multi-Tenant Studio Routing Architecture](../../../../adr/0010-admin-schema-and-multi-tenant-studio-routing.md), ADR [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md), and ADR [0026: Caffeine-Cached Studio Lookup Service](../../../../adr/0026-caffeine-cached-studio-lookup-service.md) provides the foundation for user space tenanted operations using the `@NjallUsers` session factory and PostgreSQL Row-Level Security (RLS).

Currently, user space only possesses the `studios` table. To support diverse domain objects (such as external links, characters, and events) without duplicating common lifecycle metadata or fracturing the schema, **Njall** adopts the Common Table Inheritance (CTI) pattern anchored by `njall_users.entities`. This design specifies the introduction of CTI in PostgreSQL and implements the first subtype component: external **Link** management under `/api/studios/{studio-id}/v1/links`.

## Goals / Non-Goals

**Goals:**
- Implement the CTI database architecture with `njall_users.entities` (root) and `njall_users.links` (subtype), enforcing composite primary keys `(tenant_id, id)` and referential integrity.
- Enforce PostgreSQL Row-Level Security on both tables using transaction-scoped `set_config('app.tenant_id', :tenantId, true)`.
- Support external link lifecycle operations over HTTP:
  - `POST /api/studios/{studio-id}/v1/links`: Create a link.
  - `GET /api/studios/{studio-id}/v1/links/{link-id}`: Retrieve an active link.
  - `PATCH /api/studios/{studio-id}/v1/links/{link-id}`: Partial update adhering to AIP-134.
  - `DELETE /api/studios/{studio-id}/v1/links/{link-id}`: Soft delete by setting `entities.deleted_on`.
- Resolve `{studio-id}` in-memory via `StudioLookupCache` to map public aliases/UUIDs to internal tenant IDs.
- Structure the **API plane** using Apache Pekko Typed actors (`LinkActor`) and route providers (`LinksRoute`).
- Provide tenanted `LinkDAO` in `:data` with transaction-level RLS configuration.

**Non-Goals:**
- Unfiltered listing of links (`GET /api/studios/{studio-id}/v1/links`): Not supported at this time; custom filtered queries will be added in future work.
- Polymorphic `/entities/...` routing: Entities are not addressed directly via entity routes; access occurs exclusively through canonical subtype paths.
- Complex visibility or permission ACLs beyond the **Studio** **Tenant** boundary: Multi-tenant boundary isolation is the primary constraint.

## Architecture & C4 Component Diagram

```
+───────────────────────────────────────────────────────────────────────────────────────────+
|                                        Client                                             |
|          (POST / GET / PATCH / DELETE /api/studios/{studio-id}/v1/links[/{link-id}])       |
+─────────────────────────────────────────────┬─────────────────────────────────────────────+
                                              │ HTTP Request
                                              ▼
+───────────────────────────────────────────────────────────────────────────────────────────+
| API Plane (:api)                                                                          |
|                                                                                           |
|  +─────────────────────────────────────────────────────────────────────────────────────+  |
|  | LinksRoute                                                                          |  |
|  | - Parses path segments {studio-id} and {link-id}                                    |  |
|  | - Inquires StudioLookupCache for tenantId; returns 404 if missing or soft-deleted   |  |
|  | - Dispatches LinkCommand to LinkActor via AskPattern                                |  |
|  +──────────────────────────────────────────┬──────────────────────────────────────────+  |
|                                             │ Pekko Typed Ask                             |
|                                             ▼                                             |
|  +─────────────────────────────────────────────────────────────────────────────────────+  |
|  | LinkActor                                                                           |  |
|  | - Processes LinkCommand (CreateLink, GetLink, PatchLink, DeleteLink)                |  |
|  | - Delegates execution to LinkDAO                                                    |  |
|  | - Replies with LinkActorResponse (Success, NotFound, BadRequest, Failure)           |  |
|  +──────────────────────────────────────────┬──────────────────────────────────────────+  |
+─────────────────────────────────────────────┼─────────────────────────────────────────────+
                                              │ Java Method Invocation
                                              ▼
+───────────────────────────────────────────────────────────────────────────────────────────+
| Data Plane (:data)                                                                        |
|                                                                                           |
|  +─────────────────────────────────────────────────────────────────────────────────────+  |
|  | LinkDAO / DefaultLinkDAO                                                            |  |
|  | - Opens @NjallUsers Hibernate Session & Transaction                                 |  |
|  | - Executes: SELECT set_config('app.tenant_id', :tenantId, true)                     |  |
|  | - Persists/Queries EntityBaseEntity & LinkEntity with (tenantId, linkId) composite key  |  |
|  | - Soft deletion sets entities.deleted_on = CURRENT_TIMESTAMP                         |  |
|  +──────────────────────────────────────────┬──────────────────────────────────────────+  |
+─────────────────────────────────────────────┼─────────────────────────────────────────────+
                                              │ JDBC Connection (Role: njall_users)
                                              ▼
+───────────────────────────────────────────────────────────────────────────────────────────+
| PostgreSQL Database (Schema: njall_users)                                                 |
|                                                                                           |
|  +──────────────────────────────+               +──────────────────────────────────────+  |
|  | njall_users.entities         | 1           1 | njall_users.links                    |  |
|  |------------------------------|<--------------|--------------------------------------|  |
|  | PK: (tenant_id, id)          |  FK(t_id, id) | PK: (tenant_id, id)                  |  |
|  | tenant_id REFERENCES studios |               | link_type VARCHAR(128)               |  |
|  | id UUID DEFAULT uuidv7()     |               | url VARCHAR                          |  |
|  | entity_type = 'Link'         |               | media_type VARCHAR                   |  |
|  | summary VARCHAR              |               +──────────────────────────────────────+  |
|  | created_on / updated_on      |                                                         |
|  | deleted_on (soft delete)     |  RLS: tenant_id = current_setting('app.tenant_id')     |
|  +──────────────────────────────+                                                         |
+───────────────────────────────────────────────────────────────────────────────────────────+
```

## Decisions

### 1. Common Table Inheritance (CTI) with Composite Primary Keys
- **Decision**: Define `njall_users.entities` as the root table containing common fields (`id`, `tenant_id`, `entity_type`, `summary`, `created_on`, `updated_on`, `deleted_on`) with composite primary key `(tenant_id, id)` and unique constraint `unq_entities_global_id (id)`. Subtype table `njall_users.links` has composite primary key `(tenant_id, id)` referencing `njall_users.entities(tenant_id, id)` with `ON DELETE CASCADE`.
- **Rationale**: Composite keys containing `tenant_id` allow PostgreSQL to enforce Row-Level Security and index lookups directly on the tenant boundary without requiring cross-table joins for RLS checks. Subtype tables inherit the same tenant partitioning.
- **Alternatives Considered**:
  - *Single Primary Key (`id` only)*: Rejected because RLS policies on subtype tables would require sub-selects against `entities`, incurring significant query overhead.
  - *Single Table Inheritance (STI)*: Rejected because entity subtypes have vastly divergent columns, leading to sparse null tables and weak schema constraints.

### 2. Centralized Soft Deletion on Root Entities
- **Decision**: Soft deletion is tracked exclusively on `entities.deleted_on`. When `LinkDAO.softDelete(tenantId, linkId)` is called, it executes an update setting `entities.deleted_on = CURRENT_TIMESTAMP`. Subtype table `links` has no `deleted_on` column.
- **Rationale**: Centralizing soft deletion on the root table ensures that future generic queries, entity lookups, and audit trails share a single deletion state. Read queries for links join `entities` and filter on `entities.deleted_on IS NULL`.

### 3. Functional Basepath Routing `/api/studios/{studio-id}/v1/links`
- **Decision**: Mount link endpoints under `/api/studios/{studio-id}/v1/links`. The route extracts `{studio-id}` and `{link-id}`, verifies tenant validity against `StudioLookupCache`, and dispatches requests.
- **Rationale**: Maintains exact parity with the `/api/studios/{studio-id}/v1/studio` route and preserves `/api/studios/{studio-id}` as a functional basepath for multi-host and reverse proxy routing.

### 4. AIP-134 Semantics for PATCH Updates
- **Decision**: Support partial updates to `url`, `linkType`, `mediaType`, and `summary`. Automatically update `entities.updated_on`.
- **Rationale**: Complies with API excellence conventions and AIP-134.

### 5. Hibernate and DAO Separation
- **Decision**: Implement `LinkEntity` and `EntityBaseEntity` mapping the tables in `:data`. Implement `LinkDAO` non-sealed interface extending `DAO<Link>` and `DefaultLinkDAO`.
- **Rationale**: Adheres to the sealed DAO pattern established in ADR [0005](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md) and ADR [0025](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md).

## Risks / Trade-offs

- **[Risk] Hibernate session configuration cross-talk**: If `set_config('app.tenant_id', ...)` is not transaction-scoped, connection pool reuse could leak tenant context across requests.
  - *Mitigation*: The third parameter `is_local = true` in `set_config('app.tenant_id', :tenantId, true)` ensures PostgreSQL automatically reverts the setting upon transaction completion (`commit` or `rollback`).
- **[Risk] Unsynchronized insertion in CTI**: Inserting into `links` without a corresponding `entities` row violates foreign key integrity.
  - *Mitigation*: In `DefaultLinkDAO.create()`, both `EntityBaseEntity` and `LinkEntity` are persisted within the same active transaction, ensuring atomic creation.
- **[Risk] Performance of dual-table joins for link queries**: Every link read requires joining `entities` and `links`.
  - *Mitigation*: Both tables share identical `(tenant_id, id)` primary keys, allowing PostgreSQL to perform single-index nested loop or merge joins with optimal efficiency.

## Migration Plan

1. Apply Flyway migration `V4__entities_and_links_cti.sql` creating `njall_users.entities`, `njall_users.links`, composite constraints, indices, RLS policies, and grants.
2. The migration is purely additive and backward-compatible with existing tables and data.
3. Rollback (if necessary in development): drop tables `njall_users.links` and `njall_users.entities`.

## Open Questions

- None. Design choices and scoping have been aligned.
