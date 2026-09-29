## Why

In **Njall**, **Studio** tenants require persistent, structured external references to support rich federation, discovery, and external community integration. To support diverse **Entity** subtypes while avoiding fractured schemas, the system requires a Common Table Inheritance (CTI) pattern anchored by a central `njall_users.entities` table. Introducing tenanted **Link** management at this time establishes the foundational CTI model in the **Data plane** and enables studios to manage their external links under strict PostgreSQL Row-Level Security (RLS) isolation.

## What Changes

- Add Flyway database migration `V4__entities_and_links_cti.sql` defining `njall_users.entities` (the root CTI table with UUIDv7, tenant reference, audit timestamps, and soft deletion) and `njall_users.links` (the link subtype table referencing `entities` via composite foreign key `(tenant_id, id)`).
- Enable and enforce Row-Level Security (RLS) on both `entities` and `links` using `tenant_id = current_setting('app.tenant_id', true)::uuid`.
- Introduce `Link` domain records and JPA entity definitions with composite key handling in the **Data plane**.
- Implement tenanted `LinkDAO` in `:data` providing `create`, `findById`, `patch`, and `softDelete` operations, explicitly configuring `app.tenant_id` within the session transaction before queries.
- Introduce `LinkActor` and typed message protocol in the **API plane** (`:api`) to orchestrate asynchronous link operations.
- Expose tenanted HTTP endpoints in `LinksRoute` under `/api/studios/{studio-id}/v1/links`:
  - `POST /api/studios/{studio-id}/v1/links`: Create a new link.
  - `GET /api/studios/{studio-id}/v1/links/{link-id}`: Retrieve a specific active link.
  - `PATCH /api/studios/{studio-id}/v1/links/{link-id}`: Update mutable link fields (AIP-134 semantics).
  - `DELETE /api/studios/{studio-id}/v1/links/{link-id}`: Soft delete the link by setting `entities.deleted_on`.
- Resolve `{studio-id}` via the in-memory `StudioLookupCache` to map public studio UUIDs or aliases to internal tenant IDs, rejecting soft-deleted or nonexistent studios with HTTP 404 before database interaction.
- Add comprehensive unit tests and Cucumber End-to-End integration tests in `:integration` validating RLS cross-tenant boundaries.

## Capabilities

### New Capabilities
- `user-studio-links-api`: Tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/links[/{link-id}]` for creating, retrieving, updating, and soft-deleting studio link entities with CTI and RLS isolation.

### Modified Capabilities
- `database-migration`: Add V4 migration specification defining `njall_users.entities` and `njall_users.links` tables, indices, RLS policies, and permission grants.
- `data-persistence`: Add `Link` domain object and tenanted `LinkDAO` specification with RLS transaction session configuration.

## Impact

- **Database**: Adds tables `njall_users.entities` and `njall_users.links` in PostgreSQL with RLS policies, composite primary keys, and foreign key cascades.
- **Data Module (`:data`)**: New domain models, JPA entities, DAO interfaces, and Guice bindings in `DaoModule`.
- **API Module (`:api`)**: New Pekko Typed actor `LinkActor`, route `LinksRoute`, Jackson request/response DTOs, and route registration in `StudiosModule`.
- **OpenAPI**: Update `openapi.yaml` to document `/api/studios/{studio-id}/v1/links` endpoints, request schemas, and response schemas.
- **Integration (`:integration`)**: New Cucumber feature specification and step definitions verifying link lifecycle and tenant isolation.
