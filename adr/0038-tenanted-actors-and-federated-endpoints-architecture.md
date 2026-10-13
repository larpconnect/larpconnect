# 0038: Tenanted Actors and Federated Endpoints Architecture

## Status

Accepted

## Date

2026-10-12

## Context

In **Njall**, gaming organizations (**Studios**) and users communicate using federated identities capable of publishing and receiving messages across federated servers in accordance with **FeatherPub** protocols. Following the multi-single-tenant data isolation architecture established in [0005: Sealed DAO and Hibernate Dual Session Architecture](0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0025: User Space Studios API and Tenant Row-Level Security](0025-user-space-studios-api-and-tenant-row-level-security.md), [0027: Common Table Inheritance and Tenanted Links Architecture](0027-common-table-inheritance-and-tenanted-links-architecture.md), [0036: Tenanted Individuals Architecture](0036-tenanted-individuals-architecture.md), and [0037: Tenanted Collections Architecture](0037-tenanted-collections-architecture.md), the system requires first-class models and API endpoints for managing tenanted **Actor** identities.

The architecture must enforce strict tenant isolation, support multiple actor personas per owner entity, guarantee case-insensitive permanent username reservation per studio tenant, manage subordinate federated protocol endpoints with upsert semantics, provision paired inbox and outbox collections automatically, and avoid linguistic naming conflicts with Apache Pekko concurrency actors.

## Considered Options

- **Option 1: Actors as CTI Subtype with Subordinate Endpoints and Provisioned Collections** (Selected)
  - `njall_users.actors` inherits from `njall_users.entities(tenant_id, id)` via CTI.
  - Enforce composite foreign key `(tenant_id, owner_id)` referencing `njall_users.entities(tenant_id, id) ON DELETE CASCADE`, supporting 1:N owner-to-actor relationships.
  - Enforce case-insensitive username uniqueness via functional B-tree index `idx_actors_tenant_username` on `(tenant_id, lower(preferred_username))` to prevent identity collisions and impersonation across soft-deleted records.
  - `njall_users.actor_endpoints` stores federated protocol endpoints with composite primary key `(tenant_id, actor_id, name)` and name validation (`^[a-zA-Z0-9]+$`, length < 32).
  - Automatically provision paired `inbox` and `outbox` collections in `njall_users.collections` upon actor creation, cascading soft deletion when an actor is soft-deleted.
  - Expose `POST /api/studios/{studio-id}/v1/actors` returning HTTP 201 Created.
  - Expose `GET /api/studios/{studio-id}/v1/actors/{id}` returning HTTP 200 OK or HTTP 404 Not Found.
  - Expose `PATCH` (AIP-134) and `DELETE` (soft) under `/api/studios/{studio-id}/v1/actors/{id}`.
  - Expose `GET .../inbox` and `GET .../outbox` returning expanded `OrderedCollection` payloads.
  - Expose `POST .../inbox` and `POST .../outbox` accepting JSON object payloads returning HTTP 201 Created.
  - Expose `POST ...:addEndpoint` (AIP-136 upsert) and `POST ...:removeEndpoint` (AIP-136 removal) returning HTTP 204 No Content.
  - Omit actor listing (`GET /api/studios/{studio-id}/v1/actors`), returning HTTP 404 Not Found.
  - Designate internal Java domain classes and DAOs as `Agent` (`AgentDAO`, `AgentActor`, `AgentRoute`) to avoid collision with Apache Pekko actors.
- **Option 2: Single-column Foreign Keys and Hash Indexing** (Rejected: PostgreSQL does not support unique constraints on hash indexes, and single-column keys permit cross-tenant linkages).
- **Option 3: Mutual Circular Foreign Keys for Inbox/Outbox** (Rejected: adding explicit `inbox_id` and `outbox_id` foreign keys to `actors` creates circular insertion dependencies; convention-based ownership in `collections` reuses existing infrastructure cleanly).

## Decision

1. **CTI for Actors**: `njall_users.actors` inherits from `njall_users.entities` using primary key `(tenant_id, id)`. Deletion is handled via `entities.deleted_on`.
2. **Owner Scoping**: `owner_id` references `njall_users.entities(tenant_id, id)` via composite foreign key, supporting 1:N cardinality without a unique constraint on `(tenant_id, owner_id)`.
3. **Case-Insensitive Username Reservation**: Enforce permanent username uniqueness via functional B-tree index on `(tenant_id, lower(preferred_username))`.
4. **Subordinate Endpoints with Upsert**: `njall_users.actor_endpoints` uses composite primary key `(tenant_id, actor_id, name)`. Custom method `:addEndpoint` performs SQL upsert (`ON CONFLICT DO UPDATE`) returning HTTP 204 No Content. `:removeEndpoint` removes the named endpoint returning HTTP 204 No Content.
5. **Provisioned Inbox and Outbox**: Creating an actor automatically provisions `inbox` and `outbox` records in `njall_users.collections`. Soft-deleting an actor cascades soft deletion to its inbox and outbox.
6. **Inbox and Outbox Endpoints**: `GET .../inbox` and `GET .../outbox` return expanded collections. `POST .../inbox` and `POST .../outbox` accept a JSON object payload and return HTTP 201 Created.
7. **Java Naming as Agent**: Domain classes and DAOs use `Agent` prefix (`AgentDAO`, `DefaultAgentDAO`, `AgentActor`, `AgentRoute`) while external routes remain `/api/studios/{studio-id}/v1/actors`.
8. **Row-Level Security**: Row-Level Security is enabled on both `actors` and `actor_endpoints` enforcing `tenant_id = current_setting('app.tenant_id', true)::uuid` with bypass for `njall_admin`.
9. **List Endpoint Omission**: Do not provide actor list endpoint on `/api/studios/{studio-id}/v1/actors`.

## Consequences

- **Positive**: Strict tenant isolation enforced at database and RLS layers; username collision and impersonation prevented; endpoints upserted cleanly without separate index overhead; inbox/outbox reuses collection engine seamlessly; Pekko actor name collision completely avoided.
- **Negative**: Usernames of soft-deleted actors cannot be recycled within the same tenant.
