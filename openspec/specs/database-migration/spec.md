# database-migration Specification

## Purpose

Database migration execution via Flyway, managing PostgreSQL schemas, role-based security privileges, bootstrap seed tables, and transient administrative connection lifecycles triggered via the `--migrate` CLI flag.

## Requirements

### Requirement: Bootstrap Schemas and Role Permissions
The system SHALL apply an initial database migration via Flyway that enables the `postgis` extension, creates schemas `njall`, `njall_admin`, `njall_users`, and `njall_system` owned by `njall`, sets default search paths for the respective roles, and configures default and explicit permissions so that `njall_admin` and `njall_users` have appropriate data manipulation and query access.

#### Scenario: Bootstrap migration creates schemas and applies privileges
- **GIVEN** a clean PostgreSQL database instance with pre-configured roles `njall`, `njall_admin`, `njall_users`, and `njall_system`
- **WHEN** the database migrator executes the bootstrap migration
- **THEN** schemas `njall`, `njall_admin`, `njall_users`, and `njall_system` exist and are owned by `njall`
- **AND** role `njall_admin` has USAGE on `njall`, `njall_admin`, `njall_users` and SELECT on all tables in `njall`
- **AND** role `njall_users` has USAGE on `njall`, `njall_users` and SELECT on all tables in `njall`

### Requirement: Custom Types and Seed Tables
The system SHALL create custom PostgreSQL enum types `njall.trole` and `njall.tcontact`, along with the `njall.servers` and `njall.server_contacts` tables. The `njall.servers` table SHALL use UUIDv4 (`gen_random_uuid()`) for its primary key, and `njall.server_contacts` SHALL use UUIDv7 (`uuidv7()`) for its primary key. Seed records SHALL be inserted using configured placeholders `${server_name}`, `${primary_domain}`, and `${admin_contact}`.

#### Scenario: Seed records are populated with configured placeholders
- **GIVEN** migration configuration with server name "alpha-node", primary domain "larpconnect.test", and admin contact "ops@larpconnect.test"
- **WHEN** the database migration executes
- **THEN** `njall.servers` contains a record with name "alpha-node" and primary domain "larpconnect.test"
- **AND** `njall.server_contacts` contains an ADMIN/EMAIL contact record for "ops@larpconnect.test"

### Requirement: Transient Administrative Connection Lifecycle
The system SHALL open an administrative database connection as user `njall` strictly for the duration of the migration execution. Administrative migration connections SHALL require a non-blank password by default; connecting without a password SHALL fail fast during configuration ingestion unless `trust-auth = true` is explicitly configured for the migration profile or globally. When `trust-auth = true` is configured and password is blank or omitted, the system SHALL establish the connection without a password. The connection and underlying DataSource SHALL be closed and released immediately once migration succeeds or fails.

#### Scenario: Administrative connection is closed after migration
- **GIVEN** a configured database migration service
- **WHEN** migration execution finishes
- **THEN** the administrative `njall` connection pool is closed and no active connections to the database as user `njall` remain

#### Scenario: Missing migration password without trust-auth fails fast
- **GIVEN** a migration configuration where the password is blank or omitted
- **AND** `trust-auth` is not set to `true` globally or on the migration section
- **WHEN** configuration parsing occurs via `MigrationConfig.fromConfig`
- **THEN** configuration ingestion throws an `IllegalStateException` identifying the migration profile

### Requirement: Command-Line Trigger for Migration Execution
The system SHALL support a `migrate` subcommand on application launch. When the `migrate` subcommand is executed, the application SHALL apply Flyway migrations using configured database parameters and terminate with exit status 0 on success (or 1 on failure) without binding HTTP sockets. The subcommand SHALL support optional CLI overrides for `--jdbc-url`, `--username`, `--password`, `--trust-auth`, `--schemas`, `--default-schema`, `--server-name`, `--primary-domain`, and `--admin-contact`.

#### Scenario: Server executes migration and exits when --migrate is provided
- **GIVEN** the application is started with argument `migrate`
- **WHEN** application execution runs
- **THEN** database migrations are executed to completion
- **AND** the application process terminates with exit status 0

#### Scenario: Server starts standard runtime without migration when --migrate is omitted
- **GIVEN** the application is started without argument `migrate`
- **WHEN** application execution runs
- **THEN** no database migration is executed
- **AND** no connection to the database as user `njall` is opened
- **AND** the HTTP server runtime starts normally

#### Scenario: Application executes migration with custom database parameters
- **GIVEN** the application is started with arguments `migrate --jdbc-url jdbc:postgresql://custom:5432/db --username custom_admin`
- **WHEN** application execution runs
- **THEN** database migrations are executed against the custom JDBC URL as custom_admin
- **AND** the application process terminates with exit status 0

#### Scenario: Application executes migration with trust authentication override
- **GIVEN** the application is started with arguments `migrate --trust-auth` and no password
- **WHEN** application execution runs
- **THEN** database migration configuration enables `trust-auth` and successfully applies migrations without prompting for credentials
- **AND** the application process terminates with exit status 0

#### Scenario: Application terminates with error status when migration fails
- **GIVEN** the application is started with argument `migrate` against an unreachable database
- **WHEN** application execution runs
- **THEN** database migration logs an error
- **AND** the application process terminates with exit status 1

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

### Requirement: Users Schema Entities and Links CTI Migration
The system SHALL apply Flyway migration `V4__entities_and_links_cti.sql` to initialize the Common Table Inheritance (CTI) root `njall_users.entities` table and the `njall_users.links` subtype table. The migration SHALL define `njall_users.entities` with composite primary key `(tenant_id, id)` where `id` defaults to `uuidv7()`, `tenant_id` references `njall_users.studios(id)`, and audit columns `entity_type VARCHAR NOT NULL`, `summary VARCHAR NULL`, `created_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`, `updated_on TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`, and `deleted_on TIMESTAMPTZ NULL DEFAULT NULL`. The migration SHALL define `njall_users.links` with composite primary key `(tenant_id, id)` where `(tenant_id, id)` references `njall_users.entities(tenant_id, id)` with `ON DELETE CASCADE`, and subtype columns `link_type VARCHAR(128) NOT NULL`, `url VARCHAR NOT NULL`, and `media_type VARCHAR NOT NULL`. The migration SHALL define indices `idx_entities_active` on `(tenant_id DESC, id DESC) INCLUDE (entity_type) WHERE deleted_on IS NULL` and `idx_entities_type_lookup` on `(tenant_id DESC, entity_type ASC, id DESC) WHERE deleted_on IS NULL`. The migration SHALL enable Row-Level Security on both `entities` and `links`, define RLS policy `rls_entities` and `rls_links` filtering by `tenant_id = current_setting('app.tenant_id', true)::uuid` for `njall_users`, define bypass policies for `njall_admin`, and grant permissions to both roles.

#### Scenario: Migration provisions entities and links tables with CTI constraints
- **GIVEN** a database with migrations through V3 applied
- **WHEN** migration V4 executes
- **THEN** tables `njall_users.entities` and `njall_users.links` exist
- **AND** composite primary keys `(tenant_id, id)` are established on both tables
- **AND** foreign key constraint references `njall_users.entities(tenant_id, id)` from `njall_users.links`
- **AND** indices `idx_entities_active` and `idx_entities_type_lookup` are active

#### Scenario: Row-Level Security restricts entity and link access by tenant
- **GIVEN** records in `njall_users.entities` and `njall_users.links` for tenants `tenant1` and `tenant2`
- **WHEN** a session connecting as `njall_users` sets `app.tenant_id` to `tenant1`
- **THEN** only rows matching `tenant1` are returned from `entities` and `links`
- **AND** rows for `tenant2` are filtered out
