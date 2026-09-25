# 0021: ArchUnit Record Factory Prohibition and Pure Data Carriers

## Status

Accepted

## Date

2026-09-25

## Context

Project Njall establishes in AGENTS.md that *"record objects are strict data carriers and do not need factory methods."* In practice, however, 15 records across `:common`, `:data`, and `:api` declared static factory methods:

1. **Passthrough Factories**: Domain entities (`AdminRole`, `ServerContact`, `Server`, `AdminUser`, `StudioLookup`) and DTOs (`AdminErrorResponse`, `CreateRoleRequest`, `RoleAssignmentRequest`, etc.) declared `.of(...)` static factory methods that merely forwarded parameters to constructors without added value.
2. **Framework & Parsing Logic Coupling**: Configuration records (`ServerConfig`, `DatabaseConfig`, `MigrationConfig`, `SessionConfig`) declared static `.fromConfig(...)` methods, embedding Typesafe `Config` tree traversal and fallback logic into value objects rather than delegating to dependency injection.
3. **Telemetry Coupling**: `TraceContext` declared static factory methods (`fromSpan`, `fromSpanContext`, `parseTraceparent`), while actor command records like `HealthCheckCommand.CheckHealth` directly carried `Optional<TraceContext>`, conflating domain commands with infrastructure telemetry.

Without automated architectural enforcement, static factory methods proliferate on record objects, obscuring object creation and violating the single-responsibility principle of data carriers.

## Decision

1. **ArchUnit Record Factory Prohibition Rule**: Enforce via ArchUnit in `:integration` (`ArchitectureTest`) that any `record` class within `com.larpconnect.njall..` must not declare static factory methods whose return type is the record type or an `Optional` containing the record type. Static utility functions returning other types (primitives, booleans, other domain types) and instance methods remain permitted.
2. **Direct Constructors for Records**: Remove all static `.of(...)` factory methods across domain and DTO records. Callers must invoke canonical constructors or public overloaded constructors directly.
3. **Configuration Providers and Assisted Injection**:
   - Remove static `.fromConfig(...)` methods from `ServerConfig`, `DatabaseConfig`, `MigrationConfig`, and `SessionConfig`.
   - Move configuration parsing into Guice `@Provides` methods inside `ConfigModule` and `DatabaseConfigModule`.
   - Introduce `SessionConfigFactory` and `DefaultSessionConfigFactory` (injected with Typesafe `Config` and bound in `DatabaseConfigModule`) to handle path-scoped session configuration creation without requiring AssistedInject annotations on records.
4. **Telemetry Command Envelope (`ApiCall<T>`)**:
   - Introduce `public record ApiCall<T>(T call, Optional<TraceContext> context)` in `com.larpconnect.njall.common.telemetry`.
   - Remove `Optional<TraceContext>` from actor command records (such as `HealthCheckCommand.CheckHealth`), delivering commands to actors wrapped in `ApiCall<T>`.
   - Replace `TraceContext` static factory methods with direct constructors (`TraceContext(Span)`, `TraceContext(SpanContext)`) and extract W3C header parsing into a dedicated `TraceparentParser`.

## Consequences

- **Positive**: Records remain pure, lightweight data carriers; instantiation is uniform and transparent via constructors; configuration parsing is cleanly integrated into Guice DI; telemetry correlation is decoupled from domain message protocols; automated ArchUnit verification prevents future regressions.
- **Negative**: Requires refactoring instantiation sites across 15 records in all library modules.
- **Follow-up**: Maintain the ArchUnit rule as new records and modules are introduced.
