## MODIFIED Requirements

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
