## Why

The Njall server currently uses a primitive, ad-hoc argument check supporting only a single `--migrate` flag. It lacks structured subcommand routing, option parsing, validated parameter types, help generation, and the ability to dynamically pass database credentials or HTTP server binding configurations. Introducing Picocli establishes an enterprise-grade CLI architecture with first-class `server` and `migrate` subcommands, parameter validation, and seamless layering over Typesafe Config.

## What Changes

- Add `info.picocli:picocli` (v4.7.7) to centralized version catalog and `:server` module dependencies.
- Create a dedicated CLI subpackage `com.larpconnect.njall.server.cli` containing:
  - `RootCommand`: Top-level CLI command with global flags (`-c/--config`, `-h/--help`, `-V/--version`) and default subcommand routing to `server`.
  - `ServerCommand`: Subcommand managing HTTP server configuration (`-h/--host`, `-p/--port`, `--name`, `--primary-domain`, `--admin-contact`).
  - `MigrateCommand`: Subcommand managing Flyway database migration configuration (`--jdbc-url`, `-u/--username`, `-p/--password`, `--schemas`, `--default-schema`, `--server-name`, `--primary-domain`, `--admin-contact`).
  - `CliConfigBuilder`: Pure configuration transformer that translates non-null CLI options into Typesafe `Config` overrides layered atop `reference.conf`.
  - `CliRunner`: Facade executing Picocli `CommandLine` and returning standard exit codes.
  - `CliModule`: Guice module exposing CLI bindings to `ServerModule`.
- **BREAKING**: Replace the `--migrate` CLI flag on `ServerApp` with the `migrate` subcommand (`larpconnect migrate [options]`).
- Configure root CLI invocation without arguments (`larpconnect`) to automatically default to executing the `server` subcommand.
- Update Cucumber acceptance tests and step definitions in `:integration` to execute database migrations via the `migrate` subcommand.

## Capabilities

### New Capabilities
- `command-line-interface`: Picocli-based command-line interface providing structured subcommand execution (`server`, `migrate`), typed option parsing, config file integration, and usage documentation.

### Modified Capabilities
- `database-migration`: Update execution trigger requirement from legacy `--migrate` flag to the `migrate` subcommand, incorporating CLI options for database connection parameters and seed placeholders.

## Impact

- **Build & Dependencies**:
  - `gradle/libs.versions.toml`: Add `picocli`.
  - `parent/build.gradle.kts`: Add Picocli constraint.
  - `server/build.gradle.kts`: Add Picocli implementation dependency.
- **Server Application (`:server`)**:
  - `ServerApp`: Delegated to `CliRunner`.
  - `CliArgs`: Replaced by `com.larpconnect.njall.server.cli` subpackage.
  - `ServerModule`: Installs `CliModule`.
- **Integration Tests (`:integration`)**:
  - `database_migration.feature`: Scenarios updated to target `migrate` subcommand.
  - `DatabaseMigrationSteps`: Invocation updated from `--migrate` to `migrate`.
