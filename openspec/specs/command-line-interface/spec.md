# command-line-interface Specification

## Purpose
Unified command-line interface entry point using Picocli, providing subcommand routing, server runtime launching, database migration execution, help/version display, and external configuration file overlays.

## Requirements

### Requirement: Picocli Root CLI and Subcommand Routing
The application SHALL provide a unified command-line entry point using Picocli that parses command-line arguments and routes execution to subcommands. The root command SHALL support global options `-c/--config` for specifying an external configuration file, `-h/--help` for displaying usage help, and `-V/--version` for displaying the application version. When invoked with no subcommands, the CLI SHALL automatically default to executing the `server` subcommand.

#### Scenario: Root invocation with no arguments defaults to server subcommand
- **GIVEN** the application is invoked with no arguments
- **WHEN** argument parsing occurs
- **THEN** the `server` subcommand is selected and executed with default configurations

#### Scenario: Root invocation with help option displays usage
- **GIVEN** the application is invoked with `--help`
- **WHEN** argument parsing occurs
- **THEN** application usage instructions and available subcommands are printed to standard output
- **AND** the application process terminates with exit status 0

#### Scenario: Invalid CLI option produces usage error
- **GIVEN** the application is invoked with an unrecognized option `--unknown-flag`
- **WHEN** argument parsing occurs
- **THEN** an error message is printed describing the invalid option
- **AND** the application process terminates with exit status 2

### Requirement: Server Subcommand Execution
The application SHALL provide a `server` subcommand that starts the HTTP **Server** runtime. The subcommand SHALL support options `-h/--host` (bind address), `-p/--port` (listening port), `--name` (server node name), `--primary-domain` (primary domain name), and `--admin-contact` (system administrator contact email). Command accessors and `ServerOptions` records SHALL expose these options via `Optional<T>` return types. Parsed non-empty options SHALL override Typesafe Config and reference defaults.

#### Scenario: Server subcommand starts HTTP server with default options
- **GIVEN** the application is started with argument `server`
- **WHEN** server initialization completes
- **THEN** the HTTP server service starts and binds to the configured host and port
- **AND** Pekko coordinated shutdown hooks are registered
- **AND** unset options on `ServerCommand` evaluate to `Optional.empty()`

#### Scenario: Server subcommand applies port override from command line
- **GIVEN** the application is started with arguments `server --port 9090 --host 127.0.0.1`
- **WHEN** server initialization completes
- **THEN** the HTTP server service binds to `127.0.0.1:9090`
- **AND** `serverCommand.port()` returns `Optional.of(9090)`
- **AND** `serverCommand.host()` returns `Optional.of("127.0.0.1")`

### Requirement: External Configuration File Support
The application SHALL allow operators to specify a path to an external HOCON configuration file via the `-c/--config` option on root or subcommands. The root command and configuration builder SHALL expose this option as `Optional<File>`. When present, the configuration file SHALL be parsed and layered into the Typesafe Config hierarchy with precedence over `reference.conf` and environment variables, but subordinated to explicit command-line flags.

#### Scenario: External config file overrides defaults
- **GIVEN** an external HOCON file with custom server and database settings
- **WHEN** the application is started with `--config <path>`
- **THEN** `rootCommand.configFile()` returns an `Optional<File>` containing the path
- **AND** the settings from the external configuration file are applied to the runtime

#### Scenario: No config file specified results in empty Optional
- **GIVEN** the application is started without the `--config` option
- **WHEN** argument parsing occurs
- **THEN** `rootCommand.configFile()` returns `Optional.empty()`
- **AND** configuration defaults from reference and application files are retained

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
