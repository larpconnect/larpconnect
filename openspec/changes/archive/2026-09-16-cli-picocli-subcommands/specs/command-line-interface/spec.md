## ADDED Requirements

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
The application SHALL provide a `server` subcommand that starts the HTTP server runtime. The subcommand SHALL support options `-h/--host` (bind address), `-p/--port` (listening port), `--name` (server node name), `--primary-domain` (primary domain name), and `--admin-contact` (system administrator contact email). Parsed non-null options SHALL override Typesafe Config and reference defaults.

#### Scenario: Server subcommand starts HTTP server with default options
- **GIVEN** the application is started with argument `server`
- **WHEN** server initialization completes
- **THEN** the HTTP server service starts and binds to the configured host and port
- **AND** Pekko coordinated shutdown hooks are registered

#### Scenario: Server subcommand applies port override from command line
- **GIVEN** the application is started with arguments `server --port 9090 --host 127.0.0.1`
- **WHEN** server initialization completes
- **THEN** the HTTP server service binds to `127.0.0.1:9090`

### Requirement: External Configuration File Support
The application SHALL allow operators to specify a path to an external HOCON configuration file via the `-c/--config` option on root or subcommands. When provided, the configuration file SHALL be parsed and layered into the Typesafe Config hierarchy with precedence over `reference.conf` and environment variables, but subordinated to explicit command-line flags.

#### Scenario: External config file overrides defaults
- **GIVEN** an external HOCON file with custom server and database settings
- **WHEN** the application is started with `--config <path>`
- **THEN** the settings from the external configuration file are applied to the runtime
