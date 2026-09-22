# 0010: Admin Schema and Multi-Tenant Studio Routing Architecture

## Status

Accepted

## Date

2026-09-21

## Context

Project Njall operates as a multi-tenant platform where separate gaming organizations ("studios") require strict tenant isolation. To support multi-tenancy, the database employs a multi-single-tenant model with Row Level Security (RLS). Under this model, an external request carries a studio identifier, which must resolve to an internal database `tenant_id` used for setting session context (`app.tenant_id`) and enforcing RLS policies.

Additionally, the administrative control plane requires dedicated management for platform administrators, role definitions, and role assignments without mixing control plane records into the general tenant schema (`njall_users`) or server infrastructure schema (`njall`).

## Decision

1. **Dedicated Administrative Schema (`njall_admin`)**: Establish the `njall_admin` schema for control plane management, containing `admin_users`, `admin_roles`, `admin_role_assignments`, and `studios_lookup`.
2. **Multi-Tenant Studio Identity Resolution**: `studios_lookup` acts as the definitive directory mapping public UUIDv4 `studio_id` and unique `alias` to the internal UUIDv7 `tenant_id`. Queries default to filtering out soft-deleted studios (`deleted_at IS NULL`).
3. **Cross-Schema RLS Privileges**: Enable Row Level Security on `studios_lookup` with policy `FOR ALL TO njall_users USING (tenant_id = current_setting('app.tenant_id', true)::uuid)`. Grant least-privilege `USAGE ON SCHEMA njall_admin` and `SELECT ON TABLE njall_admin.studios_lookup` to `njall_users` so tenant user sessions can evaluate RLS lookups without possessing write permissions.
4. **Instant Timestamp Standardization**: Standardize on `_at` (`created_at`, `updated_at`, `deleted_at`) for all `TIMESTAMPTZ` columns in the admin schema, serviced by a unified trigger function `njall_admin.sync_admin_timestamp()`.
5. **Symmetric Identifier Resolution in API Layer**: Support both UUID and natural key (alias/username/roleName) identifier resolution across `/api/admin/v1/` endpoints.
6. **Isolated Typed Actors on Dedicated Blocking Dispatcher**: Decompose admin business logic into `StudioAdminActor`, `UserAdminActor`, and `RoleAdminActor`, dispatched exclusively to `larpconnect.blocking-dispatcher` to prevent blocking the HTTP worker thread pool during synchronous Hibernate transactions.

## Consequences

- **Positive**: Clean separation of platform control plane from tenant data; secure and scalable studio-to-tenant routing; robust RLS enforcement with least-privilege grants; clean actor dispatching prevents HTTP thread starvation.
- **Negative**: Requires maintaining cross-schema permissions between `njall_admin` and `njall_users`; requires multi-table lookup handling in DAO and actor layers.
- **Follow-up**: Implement caching in later changes (e.g. Caffeine cache for studio alias/id to tenant_id resolution) and introduce eTag concurrency controls for role assignments.
