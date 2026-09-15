## MODIFIED Requirements

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
