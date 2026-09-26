# Proposal: Users Studio API, Admin Default Studio Roles, and Tenant Isolation

## Why

In **Njall**, the **API plane** currently exposes administrative endpoints, but lacks user-facing **Studio** endpoints segmented by **Tenant**. Introducing the user-space **Studio** API (`/api/studios/{studio-id}/v1/studio`) and underlying multi-tenant database infrastructure establishes runtime **Tenant** isolation via PostgreSQL Row-Level Security (RLS) under the `njall_users` session factory, while separating administrative routing lookups from user-space **Studio** entities.

## What Changes

- **Database Migration (`V3__users_schema_and_default_roles.sql`)**:
  - Provision `njall_users.default_studio_roles` (`id`, `name`) with RLS enabled and policies for `njall_users` and `njall_admin`.
  - Provision hostname utility functions: `njall_users.reverse_hostname_labels` and `njall_users.extract_reverse_hostname`.
  - Provision `njall_users.studios` (`id`, `name`) with RLS filtering rows on `id = current_setting('app.tenant_id', true)::uuid`, plus administrative management policies for `njall_admin`.
  - Enforce foreign key reference from `njall_admin.studios_lookup(tenant_id)` to `njall_users.studios(id)`.
- **Data Plane Refactoring (`:data`)**:
  - Rename existing `StudioDAO` and `DefaultStudioDAO` in the admin space to `StudioLookupDAO` and `DefaultStudioLookupDAO` (`@NjallAdmin Provider<SessionFactory>`), maintaining lookup queries by alias and UUID.
  - Implement tenanted `StudioDAO` and `DefaultStudioDAO` in the user space (`@NjallUsers Provider<SessionFactory>`) that sets PostgreSQL `app.tenant_id` for local transactions and queries `njall_users.studios`.
  - Implement `DefaultStudioRoleDAO` (`@NjallAdmin Provider<SessionFactory>`) managing `njall_users.default_studio_roles`.
  - Update `DAO<T>` sealed permits and Guice bindings in `DaoModule`.
  - Update studio provisioning in `StudioLookupDAO.create` to create both `njall_users.studios` and `njall_admin.studios_lookup` entries.
- **Admin Management API (`:api`)**:
  - Add administrative endpoints for default studio roles under kebab-case path `/api/admin/v1/studio-roles`:
    - `GET /api/admin/v1/studio-roles`: list roles.
    - `GET /api/admin/v1/studio-roles/{id}`: retrieve role by ID.
    - `POST /api/admin/v1/studio-roles`: create new default studio role.
    - `PATCH /api/admin/v1/studio-roles/{id}`: update role name conforming to Google AIP-134 with optional `update_mask=name`.
  - Update `POST /api/admin/v1/studios` request payload (`CreateStudioRequest`) to optionally accept `name`, defaulting to `alias` if omitted.
- **User Space Studios API (`:api`)**:
  - Implement `com.larpconnect.njall.api.studios` package with `StudiosRoute`, `StudioActor`, and message protocols.
  - Expose `GET /api/studios/{studio-id}/v1/studio`:
    - Handled asynchronously via `StudiosRoute` delegating to `StudioActor` on a dedicated `@Blocking Props` dispatcher.
    - Resolves `{studio-id}` (UUID or alias) via `StudioLookupDAO` to extract internal `tenant_id`.
    - Queries user-space `StudioDAO` using `tenant_id` via `@NjallUsers`.
    - Returns public representation (`studioId`, `alias`, `name`) without exposing internal `tenant_id`.
    - Returns HTTP 404 if `{studio-id}` is unmapped or soft-deleted.

## Capabilities

### New Capabilities

- `user-studios-api`: User-space HTTP endpoints under `/api/studios/{studio-id}/v1`, providing single-tenant studio retrieval with lookup resolution and internal tenant ID isolation.

### Modified Capabilities

- `data-persistence`: Renames admin lookup DAO to `StudioLookupDAO`, adds user-space tenanted `StudioDAO` backed by `@NjallUsers` with `app.tenant_id` setting, and adds `DefaultStudioRoleDAO`.
- `database-migration`: Adds Flyway migration V3 defining `default_studio_roles`, `studios`, reverse hostname functions, foreign key constraints, and RLS policies.
- `admin-management-api`: Adds `/api/admin/v1/studio-roles` endpoints with AIP-134 PATCH support and updates studio creation to accept optional studio name.

## Impact

- **OpenAPI Specification**: `api/src/main/resources/openapi.yaml` updated with `/api/studios/{studio-id}/v1/studio` and `/api/admin/v1/studio-roles[/{id}]` paths, schemas, and responses.
- **Database Schema**: New tables, constraints, functions, and RLS policies in `njall_users` via Flyway migration V3.
- **Data Layer**: Renamed `StudioDAO` -> `StudioLookupDAO`, new `StudioDAO` and `DefaultStudioRoleDAO`, updated entity mappings and Guice bindings.
- **API Routing**: New `StudiosRoute` mounted in `DefaultRootRoute` via `RouteProvider`.
