# 0004: Picocli Command-Line Interface and Subcommand Architecture

## Status

Accepted, supersedes ADR-0003 (Decision 4)

## Date

2026-09-15

## Supersedes

ADR-0003

## Context

ADR-0003 established the `:data` module and Flyway migration runner, specifying in Decision 4 that `ServerApp` provides a `--migrate` command-line flag to execute migrations and terminate immediately.

As configuration requirements expanded to support dynamic database credentials, custom seed placeholders, and HTTP server binding parameters, an ad-hoc flag-checking mechanism proved insufficient. The application requires a type-safe, extensible CLI framework supporting first-class subcommands (`server`, `migrate`), parameter validation, usage help, external configuration overlays, and container-friendly defaults.

## Considered Options

- **Option 1: Picocli with Subcommands, Typesafe Config Layering, and Guice Integration** (Selected)
- **Option 2: Apache Commons CLI** (Rejected: outdated procedural API, lacks first-class subcommand routing, verbose boilerplate)
- **Option 3: Custom handwritten command-line parser** (Rejected: violates anti-reinvention principle in `AGENTS.md`, error-prone, poor help formatting)

## Decision

1. **Adopt Picocli**: Standardize on Picocli 4.7+ as the project-wide command-line parsing engine within `:server`.
2. **Subcommand Routing with Container Defaults**:
   - `server`: Subcommand launching the HTTP server runtime (`-h/--host`, `-p/--port`, etc.). Invoking the root command without subcommands defaults to `server`.
   - `migrate`: Subcommand executing Flyway database migrations (`--jdbc-url`, `-u/--username`, `-p/--password`, `--schemas`, etc.).
   - Supersession: Replaces the `--migrate` CLI flag specified in ADR-0003 Decision 4 with the `migrate` subcommand without legacy backward compatibility.
3. **Typesafe Config Overlay (`CliConfigBuilder`)**: Parse CLI options into HOCON configuration maps layered on top of `ConfigFactory.load()` so existing Guice modules (`ConfigModule`, `ServerConfig`, `DatabaseConfig`) seamlessly receive CLI overrides without modifying data-layer record factories.
4. **Subpackage Parity**: Package CLI components in `com.larpconnect.njall.server.cli` with a dedicated `CliModule` installed into `ServerModule`.

## Consequences

- **Positive**: Clean, type-safe CLI structure with automatic usage generation; flexible deployment options via CLI parameters and `-c/--config` file inclusion; consistent configuration hierarchy across the application.
- **Negative**: Adds a compile-time and runtime dependency on `info.picocli:picocli` in `:server`.
- **Follow-up**: Wire future AMQP queue and operational CLI flags into `ServerCommand` as message brokers are integrated.
