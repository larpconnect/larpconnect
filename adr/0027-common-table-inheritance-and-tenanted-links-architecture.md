# 0027: Common Table Inheritance and Tenanted Links Architecture

## Status

Accepted

## Date

2026-09-28

## Context

Project **Njall** operates on a multi-single-tenant model where gaming organizations (**Studios**) have strict data isolation enforced at the database engine level via PostgreSQL Row-Level Security (RLS), as established in [0005: Sealed DAO and Hibernate Dual Session Architecture](0005-sealed-dao-and-hibernate-dual-session-architecture.md) and [0025: User Space Studios API and Tenant Row-Level Security](0025-user-space-studios-api-and-tenant-row-level-security.md).

As the application expands to support external references, federated objects, and domain resources, modeling each domain entity as an independent, unrelated table leads to schema fragmentation, duplicate lifecycle audit logic, and divergent tenant isolation strategies. Furthermore, generic entity routing or exposed `/entities/` endpoints are undesirable because all user interactions occur against canonical, typed resources.

## Considered Options

- **Option 1: Common Table Inheritance (CTI) with Composite Keys `(tenant_id, id)`** (Selected)
  - Root table `njall_users.entities` holds common metadata (`id`, `tenant_id`, `entity_type`, `summary`, `created_on`, `updated_on`, `deleted_on`).
  - Subtype tables (e.g., `njall_users.links`) share composite primary key `(tenant_id, id)` and foreign key constraint referencing `entities(tenant_id, id) ON DELETE CASCADE`.
  - Row-Level Security is enabled on both root and subtype tables filtering by `tenant_id = current_setting('app.tenant_id', true)::uuid`.
  - Soft deletion is centralized on `entities.deleted_on`.
- **Option 2: Single Table Inheritance (STI)** (Rejected: entity subtypes have divergent attributes, resulting in sparsely populated null columns and weak relational constraints).
- **Option 3: Isolated Non-Inherited Domain Tables** (Rejected: duplicates audit timestamps, soft-deletion handling, and UUIDv7 generation across every domain table without common entity tracking).

## Decision

1. **Common Table Inheritance Structure**: Establish `njall_users.entities` as the central CTI table in the `njall_users` schema. Subtype tables, starting with `njall_users.links`, must define `(tenant_id, id)` as composite primary key referencing `njall_users.entities(tenant_id, id)`.
2. **Centralized Soft Deletion**: Soft deletion is tracked exclusively on `njall_users.entities.deleted_on`. Subtype tables do not contain deletion columns; queries for active subtype records join `entities` and assert `entities.deleted_on IS NULL`.
3. **Transaction-Scoped Tenant Context**: All user-space DAOs operating under `@NjallUsers` must set `app.tenant_id` locally within the active transaction using `SELECT set_config('app.tenant_id', :tenantId, true)` prior to executing operations.
4. **Canonical Resource Basepath**: Subtype entities are exposed through canonical paths under `/api/studios/{studio-id}/v1/...`. Generic `/entities/` endpoints are prohibited.

## Consequences

- **Positive**: Consistent entity lifecycle management, standardized audit trails, optimal RLS query filtering using composite primary keys, and strong referential integrity across subtypes.
- **Negative**: Subtype writes require atomic multi-table inserts; reads require joined queries across the supertype and subtype tables.
