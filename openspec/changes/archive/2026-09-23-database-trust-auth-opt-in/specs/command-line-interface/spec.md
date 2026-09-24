## ADDED Requirements

### Requirement: Migrate Subcommand Execution and Options
The application SHALL provide a `migrate` subcommand that executes database migrations. The subcommand SHALL support options `--jdbc-url`, `-u/--username`, `-p/--password`, `--trust-auth` (flag to enable trust authentication), `--schemas`, `--default-schema`, `--server-name`, `--primary-domain`, and `--admin-contact`. Command accessors and `MigrationOptions` records SHALL expose these options via `Optional<T>` return types (with `--trust-auth` exposed as `Optional<Boolean>`). Parsed non-empty options SHALL override configuration defaults via `CliConfigBuilder`.

#### Scenario: Migrate subcommand executes with trust-auth flag
- **GIVEN** the application is invoked with arguments `migrate --trust-auth`
- **WHEN** argument parsing occurs
- **THEN** `migrateCommand.trustAuth()` returns `Optional.of(true)`
- **AND** `CliConfigBuilder` sets override `larpconnect.data.database.migration.trust-auth` to `true`

#### Scenario: Migrate subcommand defaults trust-auth to empty Optional when omitted
- **GIVEN** the application is invoked with argument `migrate` without `--trust-auth`
- **WHEN** argument parsing occurs
- **THEN** `migrateCommand.trustAuth()` returns `Optional.empty()`
- **AND** no `trust-auth` CLI override is injected by `CliConfigBuilder`
