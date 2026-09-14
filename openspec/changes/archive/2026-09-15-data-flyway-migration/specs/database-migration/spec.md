## ADDED Requirements

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
The system SHALL support a command-line flag `--migrate` on server launch. When `--migrate` is supplied, the server SHALL execute the Flyway migrations and exit immediately without binding HTTP sockets. When `--migrate` is not supplied, the server SHALL start its standard runtime without establishing any administrative `njall` connection.

#### Scenario: Server executes migration and exits when --migrate is provided
- **GIVEN** the application is started with argument `--migrate`
- **WHEN** `ServerApp.main` executes
- **THEN** database migrations are executed to completion
- **AND** the application process terminates with exit status 0

#### Scenario: Server starts standard runtime without migration when --migrate is omitted
- **GIVEN** the application is started without argument `--migrate`
- **WHEN** `ServerApp.main` executes
- **THEN** no database migration is executed
- **AND** no connection to the database as user `njall` is opened
- **AND** the HTTP server runtime starts normally
