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
