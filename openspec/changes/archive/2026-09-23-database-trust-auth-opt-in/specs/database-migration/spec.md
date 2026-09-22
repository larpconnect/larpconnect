## MODIFIED Requirements

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
