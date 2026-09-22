## Context

Project **Njall** executes command-line parsing inside the `:server` **application module** using Picocli 4.7.7. The CLI structure consists of `RootCommand` (handling global `--config` and `--verbose`), which routes to `ServerCommand` (HTTP **Server** runtime) and `MigrateCommand` (Flyway schema migrations).

Currently, all parsed option fields are declared `private @Nullable <Type> <field>`. Although Picocli reflection populates these fields, the public accessor methods (`host()`, `port()`, `name()`, `primaryDomain()`, `adminContact()`, `configFile()`, `jdbcUrl()`, `schemas()`, etc.) and immutable option records (`ServerOptions`, `MigrationOptions`) also expose `@Nullable` types. This causes null "taint" to leak out into `CliConfigBuilder` and testing harnesses, necessitating repetitive null checks and conflicting with **Njall**'s modern Java 25 standards prioritizing `java.util.Optional` for absent values.

Existing in-force architecture decisions:
- [ADR 0004](../../../../adr/0004-picocli-subcommand-architecture.md): Picocli subcommand architecture and command hierarchy.
- [ADR 0006](../../../../adr/0006-package-level-nonnull-defaults-and-nullability-conventions.md): JSpecify package-level non-null defaults and explicit `@Nullable`.

### C4 Component Diagram (ASCII)

```
+--------------------------------------------------------------------------------+
|                         CLI OPTION UN-TAINTING FLOW                            |
+--------------------------------------------------------------------------------+
|                                                                                |
|  [ Operator Invocations: larpconnect server --host 127.0.0.1 -p 9090 ]         |
|                                                                                |
|                                       |                                        |
|                                       v                                        |
|  +--------------------------------------------------------------------------+  |
|  | Framework Ingestion Boundary (Picocli Reflection)                        |  |
|  | - ServerCommand.host: private @Nullable String                           |  |
|  | - ServerCommand.port: private @Nullable Integer                          |  |
|  | - RootCommand.configFile: private @Nullable File                         |  |
|  |   (Framework nullness strictly encapsulated within private fields)       |  |
|  +--------------------------------------------------------------------------+  |
|                                       |                                        |
|                                       | Optional.ofNullable(...)               |
|                                       v                                        |
|  +--------------------------------------------------------------------------+  |
|  | Untainted Command Accessor Layer (Public API)                            |  |
|  | - ServerCommand.host()          --> Optional<String>                     |  |
|  | - ServerCommand.port()          --> Optional<Integer>                    |  |
|  | - RootCommand.configFile()      --> Optional<File>                       |  |
|  | - MigrateCommand.jdbcUrl()      --> Optional<String>                     |  |
|  | - MigrateCommand.schemas()      --> Optional<ImmutableList<String>>      |  |
|  +--------------------------------------------------------------------------+  |
|                                       |                                        |
|                                       | Canonical non-null Optional parameters |
|                                       v                                        |
|  +--------------------------------------------------------------------------+  |
|  | Immutable Options Records                                                |  |
|  | - ServerOptions(Optional<String> host, Optional<Integer> port, ...)      |  |
|  | - MigrationOptions(Optional<String> jdbcUrl, ...)                        |  |
|  +--------------------------------------------------------------------------+  |
|                                       |                                        |
|                                       | Monadic consumption (.ifPresent, .map) |
|                                       v                                        |
|  +--------------------------------------------------------------------------+  |
|  | CliConfigBuilder Layer                                                   |  |
|  | - options.host().ifPresent(h -> withOverride("server.host", h))          |  |
|  | - configFile.map(ConfigFactory::parseFile).orElse(empty())               |  |
|  +--------------------------------------------------------------------------+  |
|                                                                                |
+--------------------------------------------------------------------------------+
```

## Goals / Non-Goals

**Goals:**
- Confine `@Nullable` strictly to internal private fields populated by Picocli reflection.
- Expose `Optional<T>` across all public command option accessors on `ServerCommand`, `MigrateCommand`, and `RootCommand`.
- Retype `ServerOptions` and `MigrationOptions` record components as `Optional<T>`, eliminating null components while providing overloaded constructors for test ergonomics.
- Convert `CliConfigBuilder` configuration assembly to use functional monadic methods (`.ifPresent(...)`, `.map(...)`, `.orElse(...)`).
- Maintain 100% backward compatibility for CLI command-line invocations and Picocli runtime execution.

**Non-Goals:**
- Converting Picocli private fields themselves to `Optional<T>` (avoids runtime reflection edge cases and generic type erasure issues with split lists or primitive wrappers).
- Changing command names, options flags, or configuration keys.
- Changing `Callable<Integer>.call()` return types (governed by Picocli execution lifecycle).

## Decisions

### Decision 1: Boundary Encapsulation Pattern
- **Decision**: Keep private fields annotated with `@Nullable` and expose `Optional<T>` from public accessor methods (`public Optional<String> host() { return Optional.ofNullable(host); }`).
- **Rationale**: Isolates third-party reflection quirks at the ingestion boundary. Downstream callers receive a clean, non-null `Optional` API.
- **Alternatives Considered**:
  - *Declaring private fields as `Optional<T>` directly*: Picocli 4.7 supports `Optional`, but collection options with `split = ","` (`Optional<List<String>>`) can introduce generic type resolution issues, and reflection can bypass initializations. Boundary conversion via accessor methods is safer and guaranteed deterministic.

### Decision 2: Record Component Typing with Overloaded Constructors
- **Decision**: Declare record components in `ServerOptions` and `MigrationOptions` as `Optional<T>`. Provide secondary constructors taking `@Nullable` values that map inputs to `Optional.ofNullable(...)`.
- **Rationale**: In Java records, component types and accessor return types must match. Secondary constructors preserve ergonomic instantiation in test code (`new ServerOptions("127.0.0.1", 9090, ...)`), while the canonical constructor accepts `Optional` instances directly from command accessors.
- **Alternatives Considered**:
  - *Keeping record components as `@Nullable`*: Leaks null into record consumers and violates project standards mandating `Optional` for absent values.

### Decision 3: Collection Option Handling (`schemas`)
- **Decision**: Type `MigrateCommand.schemas()` and `MigrationOptions.schemas()` as `Optional<ImmutableList<String>>`.
- **Rationale**: Preserves the semantic distinction between "the `--schemas` flag was not passed on the CLI" (`Optional.empty()`, keeping base config) versus "the flag was supplied" (`Optional.of(...)`, overriding config).
- **Alternatives Considered**:
  - *Returning empty `ImmutableList<String>` on absence*: Conflates an omitted flag with an explicit empty list, requiring additional boolean tracking flags.

## Risks / Trade-offs

- **[Risk] Test compilation failures due to return type signature changes**:
  - *Mitigation*: Update AssertJ assertions in `ServerCommandTest`, `MigrateCommandTest`, and `RootCommandTest` to use `.contains(...)`, `.hasValue(...)`, and `.isEmpty()`.
- **[Risk] Inadvertent NPE if an `Optional` component in records is passed as null**:
  - *Mitigation*: Add compact constructor null checks (`requireNonNull(host, "host cannot be null")`) to ensure record components are always non-null `Optional` instances.

## Migration Plan

No deployment migration needed; changes are internal to `:server` compilation and test execution.

## Open Questions

None.
