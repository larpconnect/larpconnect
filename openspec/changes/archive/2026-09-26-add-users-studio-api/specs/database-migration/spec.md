# database-migration Specification Delta

## ADDED Requirements

### Requirement: Users Schema Migration and Tenant Studio Isolation
The system SHALL apply Flyway migration `V3__users_schema_and_default_roles.sql` to initialize the `njall_users` schema structure and establish cross-schema integrity with `njall_admin`. The migration SHALL create the `njall_users.default_studio_roles` table (`id UUID PRIMARY KEY DEFAULT uuidv7()`, `name VARCHAR NOT NULL UNIQUE`), enable Row-Level Security (RLS) on it, and define policy `rls_default_studio_roles_select` for `njall_users` (SELECT) and `rls_default_studio_roles_admin` for `njall_admin` (ALL). The migration SHALL define immutable utility functions `njall_users.reverse_hostname_labels(hostname TEXT)` and `njall_users.extract_reverse_hostname(uri_value TEXT)`. The migration SHALL create the `njall_users.studios` table (`id UUID PRIMARY KEY DEFAULT uuidv7()`, `name VARCHAR NOT NULL`), enable Row-Level Security on it, define policy `rls_studios` for `njall_users` filtering rows by `id = current_setting('app.tenant_id', true)::uuid`, and define policy `rls_studios_admin` for `njall_admin` permitting full access. The migration SHALL enforce foreign key constraint `fk_studios_studios_lookup` on `njall_admin.studios_lookup(tenant_id)` referencing `njall_users.studios(id)`.

#### Scenario: Migration provisions user schema tables and functions
- **GIVEN** a database with migrations V1 and V2 applied
- **WHEN** migration V3 executes
- **THEN** tables `njall_users.default_studio_roles` and `njall_users.studios` exist
- **AND** functions `njall_users.reverse_hostname_labels` and `njall_users.extract_reverse_hostname` exist
- **AND** foreign key constraint `fk_studios_studios_lookup` is active on `njall_admin.studios_lookup`

#### Scenario: Default studio roles select policy allows njall_users
- **GIVEN** records in `njall_users.default_studio_roles`
- **WHEN** a session connecting as `njall_users` queries `default_studio_roles`
- **THEN** all records are visible for reading
- **AND** INSERT, UPDATE, or DELETE statements by `njall_users` are rejected

#### Scenario: Studios table enforces tenant isolation via RLS
- **GIVEN** two studio records in `njall_users.studios` with distinct UUIDs `id1` and `id2`
- **WHEN** a session connecting as `njall_users` sets `app.tenant_id` to `id1` and executes a query
- **THEN** only record `id1` is returned
- **AND** record `id2` is filtered out by Row-Level Security
