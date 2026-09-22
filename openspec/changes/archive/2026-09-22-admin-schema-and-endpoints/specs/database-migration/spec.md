## ADDED Requirements

### Requirement: Admin Schema Migration
The system SHALL apply Flyway migration `V2__admin_schema.sql` to initialize the `njall_admin` schema structure. The migration SHALL create the `njall_admin.tstatus` enum (`ACTIVE`, `DISABLED`, `DELETED`), the `njall_admin.admin_users` table, the `njall_admin.admin_roles` table, the `njall_admin.admin_role_assignments` junction table, and the `njall_admin.studios_lookup` table. Primary keys on `admin_users`, `admin_roles`, and `studios_lookup.tenant_id` SHALL default to UUIDv7 (`uuidv7()`), and `studios_lookup.studio_id` SHALL default to random UUIDv4 (`gen_random_uuid()`). The `studios_lookup` table SHALL enforce a CHECK constraint on `alias` (`alias ~ '^[a-z][a-z0-9_]*$'`), and the `admin_roles` table SHALL enforce a CHECK constraint on `role_name` (`role_name ~ '^[a-z][a-z0-9_]*$'`).

#### Scenario: Migration creates admin tables and enum type
- **GIVEN** a database with bootstrap migration V1 applied
- **WHEN** migration V2 executes
- **THEN** type `njall_admin.tstatus` exists with values 'ACTIVE', 'DISABLED', 'DELETED'
- **AND** tables `njall_admin.admin_users`, `njall_admin.admin_roles`, `njall_admin.admin_role_assignments`, and `njall_admin.studios_lookup` exist
- **AND** `studios_lookup` rejects inserts of aliases not matching `^[a-z][a-z0-9_]*$` with a check constraint violation
- **AND** `admin_roles` rejects inserts of role names not matching `^[a-z][a-z0-9_]*$` with a check constraint violation

### Requirement: Admin Timestamp Triggers
The system SHALL install a trigger function `njall_admin.sync_admin_timestamp()` that sets `NEW.updated_at = CURRENT_TIMESTAMP`. The system SHALL attach `BEFORE UPDATE` triggers executing this function to both `njall_admin.admin_users` and `njall_admin.studios_lookup`.

#### Scenario: Timestamp trigger updates updated_at on record modification
- **GIVEN** a record in `njall_admin.admin_users` with an initial `updated_at` timestamp
- **WHEN** an UPDATE statement modifies the record
- **THEN** the `updated_at` column is automatically refreshed to `CURRENT_TIMESTAMP`

### Requirement: Multi-Tenant Studio Row Level Security and Permissions
The system SHALL enable Row Level Security (RLS) on `njall_admin.studios_lookup` with policy `rls_studios_lookup FOR ALL TO njall_users USING (tenant_id = current_setting('app.tenant_id', true)::uuid)`. The system SHALL explicitly grant `USAGE` on schema `njall_admin` and `SELECT` on table `njall_admin.studios_lookup` to role `njall_users`.

#### Scenario: Row Level Security restricts studio lookup access for njall_users
- **GIVEN** multiple studio records in `njall_admin.studios_lookup` with distinct `tenant_id` values
- **WHEN** a session connecting as `njall_users` sets `app.tenant_id` to a specific tenant UUID and queries `studios_lookup`
- **THEN** only the studio matching that `tenant_id` is visible to the query
- **AND** rows belonging to other tenant IDs are filtered out by RLS

#### Scenario: njall_users cannot insert or modify studios_lookup
- **GIVEN** a session connecting as `njall_users`
- **WHEN** an INSERT or UPDATE statement is attempted on `njall_admin.studios_lookup`
- **THEN** the database rejects the operation with a permission denied error
