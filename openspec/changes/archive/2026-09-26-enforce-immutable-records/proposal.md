## Why

Project **Njall** establishes in AGENTS.md that records are strict, immutable data carriers and that immutability contracts must be maintained throughout the system. Without automated architectural and static analysis enforcement, records can drift into holding mutable collections, unconstrained type parameters, or unverified external types. Requiring all record classes to be annotated with ErrorProne's `@Immutable` annotation via ArchUnit—and actively enforcing ErrorProne's immutability checker across all modules—guarantees deep compile-time and architectural immutability across the **API plane**, **data plane**, and **application plane**.

## What Changes

- **ArchUnit Immutability Invariant**: Introduce an ArchUnit rule in `ArchitectureTest` (`:integration` **library module**) asserting that every `record` class within `com.larpconnect.njall..` (excluding test classes) is annotated with `com.google.errorprone.annotations.Immutable`.
- **Global ErrorProne Annotations Dependency**: Expose `errorprone-annotations` (`com.google.errorprone:error_prone_annotations`) across all Java compilation targets via `build-logic/src/main/kotlin/njall.java-common-conventions.gradle.kts`.
- **ErrorProne Immutability Compiler Enforcement**: Ensure ErrorProne's `Immutable` check is configured to fail the build on violations across all modules.
- **Annotate Production Records**: Annotate all existing `record` classes in `:common`, `:data`, `:api`, and `:server` with `@Immutable`.
- **Resolve Immutability Invariant Issues**:
  - In `MigrationConfig` (`:data`): Replace mutable collection interfaces (`List<String>`, `Map<String, String>`) with Guava's `ImmutableList<String>` and `ImmutableMap<String, String>`, providing overloaded constructors for backward compatibility.
  - In `ApiCall<T>` (`:common`): Constrain the generic type parameter with `@ImmutableTypeParameter` (`public record ApiCall<@ImmutableTypeParameter T>(T call, Optional<TraceContext> context)`), guaranteeing that wrapped telemetry payloads are deeply immutable.
  - In **Actor** Command Protocols (`:api`): Safely handle external Pekko typed `ActorRef` fields by configuring ErrorProne's `Immutable:KnownImmutable` check option in build conventions (`org.apache.pekko.actor.typed.ActorRef`), recognizing thread-safe Pekko actor references as immutable across the system without requiring scattered suppressions.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `architectural-invariants`: Adds requirement and scenarios mandating that all record classes within `com.larpconnect.njall..` be annotated with `com.google.errorprone.annotations.Immutable`.

## Impact

- **Affected Code**:
  - `build-logic`: `njall.java-common-conventions.gradle.kts` adding `errorprone-annotations` dependency and configuring `Immutable` check severity.
  - `integration`: `ArchitectureTest` adding `records_must_be_annotated_immutable` ArchUnit rule.
  - `common`: `ServerConfig`, `ApiCall<T>`, `TraceContext`.
  - `data`: `DatabaseConfig`, `MigrationConfig`, `SessionConfig`, `AdminRole`, `AdminUser`, `Server`, `ServerContact`, `StudioLookup`.
  - `api`: `AdminErrorResponse`, request **DTO** records (`CreateRoleRequest`, `CreateStudioRequest`, `CreateUserRequest`, `RoleAssignmentRequest`), command records in `RoleAdminCommand`, `ServerAdminCommand`, `StudioAdminCommand`, `UserAdminCommand`, `HealthCheckCommand`, response records in `RoleAdminResponse`, `ServerAdminResponse`, `StudioAdminResponse`, `UserAdminResponse`, `HealthCheckResponse`, internal records in `UserAdminActor`.
  - `server`: `MigrationOptions`, `ServerOptions`.
- **Dependencies**: Uses existing `errorprone-annotations` defined in `gradle/libs.versions.toml`. No new third-party dependencies required.
- **Breaking Changes**: Zero external REST API breaking changes. `MigrationConfig` constructors remain compatible via overloaded constructors accepting standard `List` and `Map`.
