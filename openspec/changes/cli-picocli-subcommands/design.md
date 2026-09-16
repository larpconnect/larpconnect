## Context

The Njall server application (`:server`) currently uses a rudimentary argument parsing mechanism (`CliArgs`) that solely checks for the presence of a `--migrate` flag. As persistence and networking configurations grow, operators require a unified, validated, and self-documenting command-line interface capable of:
1. Routing execution between launching the HTTP server runtime and executing Flyway database migrations.
2. Supplying dynamic database credentials and seed placeholder parameters to migrations.
3. Specifying network bind parameters (host, port) and server metadata.
4. Loading external configuration files (`-c, --config`) to support varied deployment topologies (e.g. Kubernetes ConfigMaps).

Existing in-force ADRs:
- **0001**: Pekko HTTP Runtime Architecture and Multimodule Layout (in force)
- **0002**: Dropwizard HealthCheck Actor Pattern (in force)
- **0003**: Data Module and Flyway Migration Architecture (in force; decision 4 specifies `--migrate` flag, which this design supersedes with `migrate` subcommand)

## Goals / Non-Goals

**Goals:**
- Integrate Picocli 4.7.7 into the Gradle catalog and `:server` module.
- Create a cohesive `com.larpconnect.njall.server.cli` subpackage with `RootCommand`, `ServerCommand`, and `MigrateCommand`.
- Establish `server` as the default subcommand when no subcommand is provided.
- Build a pure `CliConfigBuilder` that converts CLI arguments into Typesafe `Config` overlays placed atop `reference.conf`.
- Support passing database parameters (`--jdbc-url`, `-u/--username`, `-p/--password`, `--schemas`, `--default-schema`, `--server-name`, `--primary-domain`, `--admin-contact`) via `migrate`.
- Support passing server parameters (`-h/--host`, `-p/--port`, `--name`, `--primary-domain`, `--admin-contact`) via `server`.
- Provide `-c/--config` option on root and subcommands for external HOCON file loading.
- Replace `CliArgs` and update `ServerApp` to execute via `CliRunner`.

**Non-Goals:**
- Maintaining backwards compatibility for the deprecated `--migrate` flag (clean break per explicit decision).
- Introducing AMQP or queue CLI parameters at this stage (deferred until AMQP integration).
- Modifying core configuration records in `:common` or `:data`.

## C4 Architectural Diagrams

### C4 Container Diagram (CLI Runtime Context)

```
+─────────────────────────────────────────────────────────────────────────────+
|                                    Operator                                 |
+──────────────────────────────────────┬──────────────────────────────────────+
                                       │
                Invokes CLI Command    │
                                       ▼
+─────────────────────────────────────────────────────────────────────────────+
| Container: Njall Server Application (:server)                               |
|                                                                             |
|  +───────────────────────────────────────────────────────────────────────+  |
|  | Component: CLI Subsystem (com.larpconnect.njall.server.cli)           |  |
|  | - RootCommand (defaultSubcommand = ServerCommand)                     |  |
|  | - ServerCommand                                                       |  |
|  | - MigrateCommand                                                      |  |
|  | - CliConfigBuilder                                                    |  |
|  | - CliRunner (Picocli CommandLine)                                     |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
|                                      │                                      |
|                Merged Typesafe Config│ Overlays                             |
|                                      ▼                                      |
|  +───────────────────────────────────────────────────────────────────────+  |
|  | Component: Guice Runtime & Lifecycle (:server / :common)              |  |
|  | - ServerModule (Installs CliModule, HttpServerModule, Common, Data)   |  |
|  | - ConfigModule (Provides layered Config & ServerConfig)               |  |
|  +───────────────────┬───────────────────────────────────┬───────────────+  |
|                      │                                   │                  |
|     Starts Service   │                  Runs Migrations  │                  |
|                      ▼                                   ▼                  |
|  +───────────────────────────────+   +───────────────────────────────────+  |
|  | Component: HttpServerService  |   | Component: DatabaseMigrator       |  |
|  | (Pekko HTTP, port binding)    |   | (:data, Flyway migrations)        |  |
|  +───────────────────────────────+   +───────────────────────────────────+  |
+──────────────────────┬───────────────────────────────────┬──────────────────+
                       │                                   │
      Binds HTTP Socket│                                   │ JDBC Connection
                       ▼                                   ▼
             +───────────────────+               +───────────────────+
             |    HTTP Clients   |               | PostgreSQL (PSQL) |
             +───────────────────+               +───────────────────+
```

### C4 Component Diagram (CLI Subsystem Pipeline)

```
                       CLI String Arguments (String[] args)
                                       │
                                       ▼
                     +───────────────────────────────────+
                     |             ServerApp             |
                     |           (main entry)            |
                     +─────────────────┬─────────────────+
                                       │
                                       ▼
                     +───────────────────────────────────+
                     |             CliRunner             |
                     |       (CommandLine.execute)       |
                     +─────────────────┬─────────────────+
                                       │
                     ┌─────────────────┴─────────────────┐
                     │                                   │
                     ▼                                   ▼
+─────────────────────────────────────────+   +─────────────────────────────────────────+
|              ServerCommand              |   |             MigrateCommand              |
|                                         |   |                                         |
| Options:                                |   | Options:                                |
|  -h, --host                             |   |  --jdbc-url                             |
|  -p, --port                             |   |  -u, --username                         |
|  -c, --config                           |   |  -p, --password                         |
|  --name, --primary-domain, etc.         |   |  --schemas, --default-schema, etc.      |
+────────────────────┬────────────────────+   +────────────────────┬────────────────────+
                     │                                             │
                     └──────────────────────┬──────────────────────┘
                                            │
                                            ▼
                     +─────────────────────────────────────────+
                     |            CliConfigBuilder             |
                     | (Maps CLI flags to HOCON override map)  |
                     +──────────────────────┬──────────────────+
                                            │
                                            ▼
                     +─────────────────────────────────────────+
                     |           ConfigFactory.load()          |
                     |  (cliOverrides.withFallback(defaults))  |
                     +──────────────────────┬──────────────────+
                                            │
                                            ▼
                     +─────────────────────────────────────────+
                     |        ServerModule(customConfig)       |
                     |         (Guice Injector creation)       |
                     +─────────────────────────────────────────+
```

## Decisions

### Decision 1: Adopt Picocli as Command-Line Engine
- **Rationale**: Picocli is a lightweight, zero-dependency, type-safe CLI library with native subcommand support, automatic usage/help formatting, custom exit codes, and high testability.
- **Alternatives Considered**:
  - *Apache Commons CLI*: Outdated API, lacks first-class subcommand architecture, verbose setup.
  - *Custom ad-hoc parser*: High maintenance burden, fragile option matching, reinventing the wheel contrary to `AGENTS.md`.

### Decision 2: Subcommand Topology and Default Routing
- **Rationale**: Structure CLI around explicit verbs: `server` and `migrate`. Configuring `RootCommand(defaultSubcommand = ServerCommand.class)` ensures that invoking the executable without parameters starts the server, preserving seamless ergonomics for container entrypoints (`docker run larpconnect`).
- **Alternatives Considered**:
  - *Strict subcommand required*: Would break standard container conventions requiring extra arguments in Dockerfile `CMD`.

### Decision 3: Typesafe Config Layering via `CliConfigBuilder`
- **Rationale**: Rather than creating custom CLI config POJOs or manually overriding Guice bindings field-by-field, CLI options are converted into a `Map<String, Object>` matching HOCON property paths (`larpconnect.server.port`, `larpconnect.data.database.migration.jdbc-url`). Merging this overlay via Typesafe `Config.withFallback()` ensures:
  1. No changes are required to `ServerConfig.fromConfig` or `MigrationConfig.fromConfig`.
  2. The configuration precedence is strictly preserved: `CLI Options > External Config File (-c) > System Properties > Environment Variables > reference.conf`.
- **Alternatives Considered**:
  - *Custom Guice module overrides*: More code to maintain and risks desynchronizing configuration parsing between CLI and file-based mechanisms.

### Decision 4: Package Architecture in `:server`
- **Rationale**: Placing CLI classes in `com.larpconnect.njall.server.cli` maintains strict DAG topology and package-to-module parity. `CliModule` exposes `CliRunner` and command factories, installed cleanly by `ServerModule`.

### Decision 5: Clean Break on `--migrate` Flag
- **Rationale**: The user explicitly confirmed no backward compatibility is required. Replacing `--migrate` cleanly with `migrate` eliminates tech debt and avoids ambiguous root option collisions.

## Risks / Trade-offs

- **[Risk]** CLI flag option names differing from HOCON configuration paths.
  - *Mitigation*: Centralize key mappings in `CliConfigBuilder` and write comprehensive unit tests asserting each CLI flag translates to the exact expected HOCON path.
- **[Risk]** Subcommand option collision if global options expand in the future.
  - *Mitigation*: Keep root options limited to universal flags (`-c/--config`, `-h/--help`, `-V/--version`) and isolate operational options to their respective subcommands.

## Migration Plan

1. Add Picocli dependency to `libs.versions.toml`, `parent`, and `server`.
2. Implement `com.larpconnect.njall.server.cli` package.
3. Update `ServerApp` to execute via `CliRunner`.
4. Remove `CliArgs` and replace `CliArgsTest` with tests for the new CLI components.
5. Update `database_migration.feature` and `DatabaseMigrationSteps` to use `migrate` subcommand.
6. Verify `./gradlew check build` passes.

## Open Questions

- None. (ADR 0004 will formally record the supersession of ADR 0003 Decision 4 regarding `--migrate` CLI flag).
