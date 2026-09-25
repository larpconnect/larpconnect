## Why

In **Njall**, records are designed to be strict, immutable data carriers. However, across the **API plane**, **data plane**, and **application plane**, 15 record classes currently declare static factory methods (`.of(...)`, `.fromConfig(...)`, `.fromSpan(...)`, and `.parseTraceparent(...)`). These factory methods introduce procedural instantiation antipatterns, duplicate constructors, and in some cases blend complex framework configuration parsing into data carrier classes. Mechanically prohibiting static factory methods on record objects via ArchUnit in the `:integration` **library module** permanently prevents architectural regression, while replacing simple factories with direct constructors and complex parsing with Guice providers and assisted injection.

## What Changes

- **ArchUnit Record Invariant**: Introduce an ArchUnit rule in `ArchitectureTest` (`:integration` **library module**) asserting that no record class within `com.larpconnect.njall..` declares static factory methods returning its own record type or `Optional` of its record type. Static utility functions (returning other types) and instance methods remain permitted.
- **Direct Record Constructors**: Remove all static `.of(...)` factory methods across domain, DTO, and configuration records in `:common`, `:api`, and `:data`, replacing them with direct canonical constructors and explicit overloaded constructors.
- **Configuration Providers & Assisted Injection**:
  - Remove static `.fromConfig(...)` methods from `ServerConfig`, `DatabaseConfig`, `MigrationConfig`, and `SessionConfig`.
  - Move configuration parsing into dedicated Guice `@Provides` methods inside `ConfigModule` and `DatabaseConfigModule`.
  - Introduce a Guice assisted injection factory (`SessionConfigFactory`) for dynamic, path-scoped session configuration.
- **Generic Actor Command Envelope (`ApiCall<T>`)**:
  - Introduce `public record ApiCall<T>(T call, Optional<TraceContext> context)` in `com.larpconnect.njall.common.telemetry`.
  - Remove embedded `Optional<TraceContext>` from domain command records (such as `HealthCheckCommand.CheckHealth`), dispatching commands to actors wrapped in `ApiCall<T>`.
  - Update `TraceContext` to eliminate static factory methods in favor of constructors (`TraceContext(Span)`, `TraceContext(SpanContext)`) and extract W3C header parsing into a dedicated `TraceparentParser`.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `architectural-invariants`: Prohibits record classes in `com.larpconnect.njall..` from declaring static factory methods returning the record type or `Optional<Record>`.
- `opentelemetry-tracing`: Standardizes trace context propagation to Pekko Typed **Actor** behaviors through the `ApiCall<T>` command envelope instead of ad-hoc command record parameters.

## Impact

- **Affected Code**:
  - `common`: `ServerConfig`, `TraceContext`, `ConfigModule`, `TraceparentParser`, `ApiCall`.
  - `data`: `DatabaseConfig`, `MigrationConfig`, `SessionConfig`, `DatabaseConfigModule`, `SessionConfigFactory`, domain entities (`Server`, `ServerContact`, `AdminUser`, `StudioLookup`, `AdminRole`).
  - `api`: `AdminErrorResponse`, request DTOs (`CreateRoleRequest`, `CreateStudioRequest`, `CreateUserRequest`, `RoleAssignmentRequest`), `HealthCheckCommand`, `HealthCheckActor`, `DefaultAdminRoute`, `DefaultTracingDirective`.
  - `integration`: `ArchitectureTest` ArchUnit rule and Cucumber test steps updating record construction and command dispatching.
- **Dependencies**: Utilizes existing Guice core, Guice assisted injection, and ArchUnit dependencies.
- **Breaking Changes**: Zero external REST API breaking changes. Internal Java record construction shifts from `.of(...)` / `.fromConfig(...)` to constructors and injected factories.
