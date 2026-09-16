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
The system SHALL open an administrative database connection as user `njall` strictly for the duration of the migration execution. The connection and underlying DataSource SHALL be closed and released immediately once migration succeeds or fails.

#### Scenario: Administrative connection is closed after migration
- **GIVEN** a configured database migration service
- **WHEN** migration execution finishes
- **THEN** the administrative `njall` connection pool is closed and no active connections to the database as user `njall` remain

### Requirement: Command-Line Trigger for Migration Execution
The system SHALL support a `migrate` subcommand on application launch. When the `migrate` subcommand is executed, the application SHALL apply Flyway migrations using configured database parameters and terminate with exit status 0 on success (or 1 on failure) without binding HTTP sockets. The subcommand SHALL support optional CLI overrides for `--jdbc-url`, `--username`, `--password`, `--schemas`, `--default-schema`, `--server-name`, `--primary-domain`, and `--admin-contact`.

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

#### Scenario: Application terminates with error status when migration fails
- **GIVEN** the application is started with argument `migrate` against an unreachable database
- **WHEN** application execution runs
- **THEN** database migration logs an error
- **AND** the application process terminates with exit status 1
