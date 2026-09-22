## Why

Project Njall requires a dedicated administrative data schema (`njall_admin`) and management API surface to control the platform's multi-tenant studio routing, administrative user accounts, and role-based access controls. Establishing this schema and associated REST endpoints provides the foundational control plane needed before tenant-isolated studio workloads can be routed, authenticated, and managed.

## What Changes

- **Database Migration (`V2__admin_schema.sql`)**:
  - Creates the `njall_admin.tstatus` enum (`ACTIVE`, `DISABLED`, `DELETED`).
  - Creates the `njall_admin.admin_users` table with UUIDv7 primary keys, unique usernames, status, and timestamps.
  - Creates the `njall_admin.admin_roles` table with UUIDv7 primary keys and unique role names.
  - Creates the `njall_admin.admin_role_assignments` junction table with cascading foreign keys.
  - Creates the `njall_admin.studios_lookup` table for multi-tenant studio routing, random UUIDv4 `studio_id`, unique alias, soft-delete timestamp, and Row Level Security (RLS) policies for `njall_users`.
  - Harmonizes timestamp conventions to `_at` (`created_at`, `updated_at`, `deleted_at`) and installs a single update trigger function `njall_admin.sync_admin_timestamp()`.
  - Grants least-privilege `USAGE` on `njall_admin` schema and `SELECT` on `njall_admin.studios_lookup` to `njall_users`.
- **Data Persistence Layer (`:data`)**:
  - Adds domain records `AdminUser`, `AdminRole`, and `StudioLookup` implementing `DatabaseObject`.
  - Adds JPA entities `AdminUserEntity`, `AdminRoleEntity`, and `StudioLookupEntity` mapped to `njall_admin` tables and bound to `@NjallAdmin` `SessionFactory`.
  - Adds `AdminUserDAO`, `AdminRoleDAO`, and `StudioDAO` interfaces and implementations providing query and mutation operations using `@NjallAdmin Provider<SessionFactory>`.
- **Administrative API & Actor Layer (`:api`)**:
  - Implements typed Pekko actors `StudioAdminActor`, `UserAdminActor`, and `RoleAdminActor` operating on `larpconnect.blocking-dispatcher`.
  - Implements modular sub-routes `StudioAdminRoute`, `UserAdminRoute`, and `RoleAdminRoute` composed into `DefaultAdminRoute`.
  - Exposes `/api/admin/v1/studios[/{id}]` (GET, POST) supporting symmetric UUID and alias lookups, filtering soft-deleted studios by default with `?include_deleted=true`.
  - Exposes `/api/admin/v1/users[/{id}]` (GET, POST) with hybrid creation (optional initial role IDs) and AIP-136 custom methods:
    - `/api/admin/v1/users/{id}:addRole` (POST accepting `roleId` or `roleName`, idempotent 200 OK).
    - `/api/admin/v1/users/{id}:removeRole` (POST accepting `roleId` or `roleName`, idempotent 200 OK).
  - Exposes `/api/admin/v1/roles[/{id}]` (GET, POST) with symmetric UUID and role name lookups.
  - Standardizes error responses on AIP-193 structured JSON objects (`400 Bad Request`, `404 Not Found`, `409 Conflict`).
- **OpenAPI & Integration Contracts (`:api`, `:integration`)**:
  - Updates `openapi.yaml` with schema definitions and endpoint specifications.
  - Adds Cucumber behavioral integration tests in `:integration` validating migration schema privileges, RLS enforcement, and end-to-end HTTP route behaviors.

## Capabilities

### New Capabilities
- `admin-management-api`: Administrative REST endpoints for managing studios, admin users, roles, and user-role assignments under `/api/admin/v1/`.

### Modified Capabilities
- `data-persistence`: Introduces `AdminUser`, `AdminRole`, and `StudioLookup` domain models, JPA entities, and DAOs for the `njall_admin` schema with `@NjallAdmin Provider<SessionFactory>` injection.
- `database-migration`: Applies `V2__admin_schema.sql` migration creating the `njall_admin` schema, tables, triggers, RLS policies, and cross-role grants.

## Impact

- **Database**: Adds schema `njall_admin` with tables `admin_users`, `admin_roles`, `admin_role_assignments`, and `studios_lookup`.
- **API Surface**: Expands `/api/admin/v1` routes and updates `openapi.yaml`.
- **Runtime**: Introduces new typed Pekko actors on the blocking dispatcher.
- **Dependencies**: No new third-party dependencies required.
- **Backward Compatibility**: Fully backward compatible; existing `/` and `/api/admin/v1/health` and `/api/admin/v1/servers` endpoints remain unchanged.
