# 0012: CLI Command Option Null Encapsulation and Optional Accessors

## Status

Accepted

## Date

2026-09-22

## Context

Project Njall uses Picocli for command-line parsing inside the `:server` application module (ADR 0004). Picocli uses reflection to populate private command fields, leaving absent options as `null`.

Previously, public accessor methods across `ServerCommand`, `MigrateCommand`, and `RootCommand`, as well as immutable option records (`ServerOptions`, `MigrationOptions`), directly exposed `@Nullable` return types. This leaked framework-level nullness "taint" into application configuration builders (`CliConfigBuilder`) and test harnesses, requiring repetitive defensive null checks and conflicting with Njall's Java 25 standards that mandate `java.util.Optional` for absent values.

## Decision

1. **Boundary Encapsulation Pattern**: Isolate `@Nullable` strictly to internal private fields populated by Picocli reflection. All public accessor methods on CLI command classes (`ServerCommand`, `MigrateCommand`, `RootCommand`) must return `java.util.Optional<T>` (e.g. `Optional.ofNullable(field)`).
2. **Options Records with Optional Components**: Define components of `ServerOptions` and `MigrationOptions` as `Optional<T>` to eliminate null state in domain and configuration carriers. Provide overloaded secondary constructors accepting `@Nullable` raw values to ensure backward compatibility and ergonomics in test setups.
3. **Preserve Semantic Absence on Collection Options**: Return `Optional<ImmutableList<String>>` for multi-value options (such as `--schemas`) to preserve the semantic distinction between an omitted flag (which should not override configuration) and an explicitly provided empty list.
4. **Monadic Configuration Assembly**: Update `CliConfigBuilder` to consume `Optional` accessors directly using functional operations (`.ifPresent(...)`, `.map(...)`, `.orElse(...)`), eliminating null-checking branches.

## Consequences

- **Positive**: Encapsulates third-party framework reflection at the CLI ingestion boundary; completely prevents null taint from entering application logic; simplifies `CliConfigBuilder` into functional streams; aligns CLI API design with project-wide `Optional` conventions.
- **Negative**: Requires updating AssertJ assertions in CLI unit tests to check `Optional` instances (`.contains(...)`, `.hasValue(...)`, `.isEmpty()`).
