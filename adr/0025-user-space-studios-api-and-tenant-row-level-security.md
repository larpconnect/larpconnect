# 0025: User Space Studios API and Tenant Row-Level Security

## Status

Accepted

## Date

2026-09-26

## Context

Project **Njall** uses a multi-single-tenant database architecture where external clients access specific gaming organizations (**Studios**) with strict data isolation. Architectural decisions [0005: Sealed DAO and Hibernate Dual Session Architecture](0005-sealed-dao-and-hibernate-dual-session-architecture.md) and [0010: Admin Schema and Multi-Tenant Studio Routing Architecture](0010-admin-schema-and-multi-tenant-studio-routing.md) established the dual `@NjallAdmin` and `@NjallUsers` session factories and the administrative directory table `njall_admin.studios_lookup`.

Until now, the data plane lacked user-space **Studio** entities and tenanted DAOs operating under `@NjallUsers`. The existing `StudioDAO` in `:data` was misnamed because it only queried the administrative routing directory in `njall_admin`. Furthermore, user-facing endpoints did not exist under `/api/studios/{studio-id}/v1` to allow a **Tenant** to query its own studio information without exposing internal database identifiers.

## Considered Options

- **Option 1: Two-Tier DAO Architecture with Transaction-Scoped RLS Configuration** (Selected)
  - Rename admin directory DAO to `StudioLookupDAO` (`@NjallAdmin`).
  - Introduce tenanted `StudioDAO` (`@NjallUsers`) operating on `njall_users.studios` with `set_config('app.tenant_id', :tenantId, true)` transaction-scoped RLS setting.
  - Implement `/api/studios/{studio-id}/v1/studio` resolving `{studio-id}` via `StudioLookupDAO` and querying `StudioDAO`.
- **Option 2: Combined Multi-Schema DAO using `@NjallAdmin`** (Rejected: violates least privilege and fails to utilize the `@NjallUsers` session factory and native PostgreSQL RLS).
- **Option 3: Direct Client-Provided `tenant_id`** (Rejected: exposes internal database primary keys to public APIs; public clients must use natural aliases or public studio UUIDs).

## Decision

1. **DAO Separation**: Rename existing `StudioDAO` / `DefaultStudioDAO` in `:data` to `StudioLookupDAO` / `DefaultStudioLookupDAO` (`@NjallAdmin`). Implement a new user-space `StudioDAO` / `DefaultStudioDAO` (`@NjallUsers`).
2. **Transaction-Scoped Tenant Context**: In `DefaultStudioDAO`, every operation on `@NjallUsers` sets `app.tenant_id` locally within the active transaction (`SELECT set_config('app.tenant_id', :tenantId, true)`) before querying `njall_users.studios`.
3. **Cross-Schema Referential Integrity**: Enforce foreign key `fk_studios_studios_lookup` from `njall_admin.studios_lookup(tenant_id)` to `njall_users.studios(id)`. Update `StudioLookupDAO.create` to insert into `njall_users.studios` before `njall_admin.studios_lookup`.
4. **User-Space Endpoint and Public Model**: Expose `GET /api/studios/{studio-id}/v1/studio` in package `com.larpconnect.njall.api.studios`. The response model serializes public `studioId`, `alias`, and `name`; internal `tenant_id` is never exposed.
5. **AIP-134 Conforming PATCH for Studio Roles**: Expose `/api/admin/v1/studio-roles[/{id}]` with `PATCH` implementing Google AIP-134 standard update behavior.

## Consequences

- **Positive**: Strict tenant isolation enforced at the PostgreSQL engine level via RLS; internal database primary keys (`tenant_id`) completely encapsulated; clean separation between control plane routing and tenant domain models.
- **Negative**: Requires dual-write synchronization during initial studio provisioning across `njall_users` and `njall_admin`; requires two database queries (lookup resolution + tenanted query) on user studio access.
