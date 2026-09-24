## Why

Command-line options parsed by Picocli in **Njall**'s `:server` **application module** currently expose `@Nullable` return types across all command accessors (`ServerCommand`, `MigrateCommand`, `RootCommand`) and options records (`ServerOptions`, `MigrationOptions`). This allows framework-level nullness "taint" to leak into application logic, forcing callers to maintain defensive `!= null` checks rather than leveraging functional `java.util.Optional` APIs mandated by project Java standards.

## What Changes

- Modify all public option accessor methods on `ServerCommand`, `MigrateCommand`, and `RootCommand` to return `Optional<T>` rather than `@Nullable T`.
- Retain `@Nullable` annotations exclusively on private command fields where Picocli reflection injects parsed arguments.
- Refactor `ServerOptions` and `MigrationOptions` records so their components are typed as `Optional<T>`, providing overloaded constructors accepting nullable raw values for backward-compatible and ergonomic test creation.
- Update `CliConfigBuilder` to consume `Optional` accessors directly via `.ifPresent(...)` and replace `withConfigFile(@Nullable File)` with `withConfigFile(Optional<File>)`.
- Update command and config builder unit tests to assert `Optional` states (`contains(...)`, `hasValue(...)`, `isEmpty()`) using AssertJ.
- Update `nullability-contracts` and `command-line-interface` behaviour specifications.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `command-line-interface`: Clarifies that parsed CLI options are exposed via `Optional<T>` accessors on command and options records, eliminating nullable return types across CLI boundaries.
- `nullability-contracts`: Updates CLI option specifications to mandate `Optional<T>` return types for optional CLI flags and configuration file resolution, isolating `@Nullable` strictly to internal framework injection fields.

## Impact

- **Affected Code**: `com.larpconnect.njall.server.cli` (`ServerCommand`, `MigrateCommand`, `RootCommand`, `ServerOptions`, `MigrationOptions`, `CliConfigBuilder`) in `:server`.
- **Affected Tests**: `ServerCommandTest`, `MigrateCommandTest`, `RootCommandTest`, and `CliConfigBuilderTest` in `:server`.
- **APIs**: Public command accessors now return `Optional<T>`. Callers expecting `@Nullable T` will need to use `.orElse(null)` or monadic `.ifPresent(...)` / `.map(...)`.
- **Dependencies**: No external dependency changes; uses standard `java.util.Optional` and existing `com.google.common.collect.ImmutableList`.
