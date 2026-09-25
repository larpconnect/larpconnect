## Context

In **Njall**, Java records represent strict data carriers. AGENTS.md establishes two fundamental invariants governing data structures:
1. *"record objects are strict data carriers and do not need factory methods."*
2. *"Immutable objects and immutability contracts should be used whenever possible. This may involve using immutable collections from Guava and/or defensive copies, but it needs to be maintained throughout the system."*

In practice, records across the codebase currently lack automated enforcement requiring ErrorProne's `@com.google.errorprone.annotations.Immutable` annotation. Without static analysis and architectural gates, records can inadvertently accept mutable collections (such as `java.util.List` or `java.util.Map`), unconstrained generic type parameters (`T`), or unverified external types.

This design introduces:
1. An automated ArchUnit rule in `:integration` (`ArchitectureTest`) asserting that every `record` class in `com.larpconnect.njall..` (excluding test classes) is annotated with `@Immutable`.
2. Universal compilation availability of `errorprone-annotations` via `build-logic`.
3. ErrorProne compiler enforcement of the `Immutable` bug pattern across all modules.
4. Comprehensive annotation of all production records across `:common`, `:data`, `:api`, and `:server`, resolving subsequent immutability issues (such as `MigrationConfig` collections, `ApiCall<T>` type parameter, and Pekko Typed **Actor** command references).

## Goals / Non-Goals

**Goals:**
- Enforce via ArchUnit in `:integration` (`ArchitectureTest`) that all non-test `record` classes in `com.larpconnect.njall..` are annotated with `@com.google.errorprone.annotations.Immutable`.
- Expose `errorprone-annotations` (`com.google.errorprone:error_prone_annotations`) across all Java compilation targets via `build-logic/src/main/kotlin/njall.java-common-conventions.gradle.kts`.
- Configure ErrorProne `options.errorprone.error("Immutable")` in `njall.java-common-conventions.gradle.kts` to strictly enforce immutability during compilation.
- Annotate all production records in `:common`, `:data`, `:api`, and `:server` with `@Immutable`.
- Refactor `MigrationConfig` (`:data`) to use Guava's `ImmutableList<String>` and `ImmutableMap<String, String>`, retaining overloaded constructors for backward compatibility.
- Configure ErrorProne `Immutable:KnownImmutable` option in build conventions for `org.apache.pekko.actor.typed.ActorRef` so Pekko Typed actor references are recognized as immutable across all modules without requiring scattered `@SuppressWarnings("Immutable")` annotations.
- Constrain `ApiCall<T>` type parameter `T` with `@ImmutableTypeParameter` to guarantee encapsulated command payloads are deeply immutable.
- Pass all quality suite checks (`./gradlew check build`) with 100% compliance.

**Non-Goals:**
- Requiring `@Immutable` on test-only fixture records located in `src/test/java` directories.
- Rewriting or wrapping Pekko Typed's `ActorRef` messaging primitive.
- Modifying external HTTP/REST API endpoints, status codes, or OpenAPI schemas.

## Architectural Component Diagram (C4 Component Level)

```
+─────────────────────────────────────────────────────────────────────────────+
|                                  NJALL                                      |
+─────────────────────────────────────────────────────────────────────────────+
|                                                                             |
|  build-logic (Gradle Conventions)                                           |
|  +───────────────────────────────────────────────────────────────────────+  |
|  | njall.java-common-conventions.gradle.kts                              |  |
|  |  - compileOnly(libs.errorprone.annotations)                           |  |
|  |  - options.errorprone.error("Immutable")                              |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
|                                      │ applies to all modules               |
|                                      v                                      |
|  :integration (Library Module)                                              |
|  +───────────────────────────────────────────────────────────────────────+  |
|  | ArchitectureTest                                                      |  |
|  |  - records_must_be_annotated_immutable ArchRule                        |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
|                                      │ verifies non-test classes            |
|                                      v                                      |
|  :common (Library Module)                                                   |
|  +───────────────────────────────────────────────────────────────────────+  |
|  | ServerConfig (@Immutable)                                             |  |
|  | TraceContext (@Immutable)                                             |  |
|  | ApiCall<@ImmutableTypeParameter T> (@Immutable)                       |  |
|  +───────────────────────────────────▲───────────────────────────────────+  |
|                                      │ imports                              |
|  :data (Library Module)              │                                      |
|  +───────────────────────────────────┴───+   +───────────────────────────+  |
|  | DatabaseObject (sealed interface)     |   | MigrationConfig           |  |
|  | Domain Records (@Immutable):          |   |  - ImmutableList<String>  |  |
|  |  Server, ServerContact, AdminUser,    |   |  - ImmutableMap<K, V>     |  |
|  |  StudioLookup, AdminRole              |   | SessionConfig (@Immutable)|  |
|  |                                       |   | DatabaseConfig            |  |
|  +───────────────────────────────────────+   +───────────────────────────+  |
|                                      ▲                                      |
|                                      │ imports                              |
|  :api (Library Module)               │                                      |
|  +───────────────────────────────────┴───────────────────────────────────+  |
|  | Request/Response DTO Records (@Immutable)                             |  |
|  | Command Protocols (@Immutable):                                       |  |
|  |  RoleAdminCommand, ServerAdminCommand, StudioAdminCommand,            |  |
|  |  UserAdminCommand, HealthCheckCommand                                 |  |
|  |  (ActorRef recognized via Immutable:KnownImmutable option)            |  |
|  +───────────────────────────────────▲───────────────────────────────────+  |
|                                      │ imports                              |
|  :server (Application Module)        │                                      |
|  +───────────────────────────────────┴───────────────────────────────────+  |
|  | MigrationOptions (@Immutable), ServerOptions (@Immutable)             |  |
|  +───────────────────────────────────────────────────────────────────────+  |
|                                                                             |
+─────────────────────────────────────────────────────────────────────────────+
```

### Diagram Walkthrough

- **`build-logic`**: Configures `compileOnly(libs.errorprone.annotations)` so that every **module** has compile-time access to `@Immutable` and `@ImmutableTypeParameter`. Configures ErrorProne's `Immutable` check as an error during compilation.
- **`:integration`**: `ArchitectureTest` enforces via ArchUnit that all compiled non-test record classes in `com.larpconnect.njall..` carry the `@Immutable` annotation.
- **`:common`**: Encapsulates core configuration and telemetry records (`ServerConfig`, `TraceContext`, and `ApiCall<@ImmutableTypeParameter T>`).
- **`:data`**: Models persistent domain entities (`Server`, `AdminUser`, `ServerContact`, etc.) implementing the sealed interface `DatabaseObject`. Modernizes `MigrationConfig` collections to Guava `ImmutableList` and `ImmutableMap`.
- **`:api`**: Enforces `@Immutable` on all request/response **DTO** records and internal actor command/response message protocols.
- **`:server`**: Enforces `@Immutable` on command-line interface options records.

## Decisions

### Decision 1: Dual Verification Gate (ArchUnit + ErrorProne)
- **Choice**: Enforce `@Immutable` presence via ArchUnit in `:integration` (`ArchitectureTest`) while configuring ErrorProne to treat `@Immutable` violations as compilation errors in `build-logic`.
- **Rationale**: ErrorProne's immutability checker only analyzes classes annotated with `@Immutable`; an unannotated record would silently escape compiler inspection. Conversely, ArchUnit cannot inspect deep type graph immutability without massive custom bytecode traversal. The two tools form an airtight verification pair: ArchUnit guarantees the annotation is present, and ErrorProne guarantees deep immutability.
- **Alternatives Considered**:
  - *ErrorProne alone*: Fails because unannotated records are ignored.
  - *ArchUnit alone*: Requires complex reflective checks for field types and collections.

### Decision 2: Universal Annotation Dependency via `build-logic`
- **Choice**: Add `compileOnly(libs.errorprone.annotations)` to the common Java convention plugin (`build-logic/src/main/kotlin/njall.java-common-conventions.gradle.kts`).
- **Rationale**: Keeps annotations available across all modules without polluting runtime classpaths or requiring duplicated entries in every module `build.gradle.kts`.
- **Alternatives Considered**: Adding dependencies per-module. Rejected due to boilerplate and inconsistency risks.

### Decision 3: Collection Modernization in `MigrationConfig`
- **Choice**: Change `List<String> schemas` and `Map<String, String> placeholders` in `MigrationConfig` to `ImmutableList<String>` and `ImmutableMap<String, String>`, providing overloaded constructors accepting standard `List` and `Map` that perform defensive copies via Guava.
- **Rationale**: `java.util.List` and `java.util.Map` are mutable interfaces flagged by ErrorProne. AGENTS.md mandates Guava immutable collections. Overloaded constructors preserve seamless compatibility for callers.
- **Alternatives Considered**: Suppressing `@SuppressWarnings("Immutable")` on `MigrationConfig`. Rejected because `MigrationConfig` should be genuinely immutable.

### Decision 4: Handling Pekko Typed `ActorRef` via ErrorProne `KnownImmutable`
- **Choice**: Configure ErrorProne's `checkOptions.put("Immutable:KnownImmutable", "org.apache.pekko.actor.typed.ActorRef")` centrally in `build-logic/src/main/kotlin/njall.java-common-conventions.gradle.kts`.
- **Rationale**: Pekko Typed `ActorRef` is an immutable, thread-safe message channel defined in a third-party library without ErrorProne annotations. Configuring `KnownImmutable` globally teaches ErrorProne to treat `ActorRef` as immutable without littering `@SuppressWarnings("Immutable")` across command records.
- **Alternatives Considered**: Suppressing `@SuppressWarnings("Immutable")` individually on each record (rejected as noisy and boilerplate-heavy) or wrapping `ActorRef` (rejected as unnecessary boxing).

### Decision 5: Constraining Generic Envelope `ApiCall<T>`
- **Choice**: Annotate the type parameter `T` in `ApiCall` with `@ImmutableTypeParameter`:
  ```java
  @Immutable
  public record ApiCall<@ImmutableTypeParameter T>(T call, Optional<TraceContext> context)
  ```
- **Rationale**: Guarantees that any command or payload passed inside an `ApiCall` is verified as deeply immutable at compile time. Sealed command protocols (e.g., `HealthCheckCommand`) are marked `@Immutable` at their protocol interface root so ErrorProne enforces that all permitted message implementations are immutable records.
- **Alternatives Considered**: Suppressing `@SuppressWarnings("Immutable")` on the type parameter or field. Rejected because compile-time verification guarantees deeply immutable telemetry envelopes.

## Risks / Trade-offs

- **[Risk] ErrorProne flags third-party classes in records** -> **Mitigation**: Pekko `ActorRef` is configured centrally as `KnownImmutable` in Gradle build conventions.
- **[Risk] Test records flagged by ArchUnit** -> **Mitigation**: `ArchitectureTest` uses `importOptions = {ImportOption.DoNotIncludeTests.class}` to ensure test fixtures remain unconstrained.
- **[Risk] Breaking callers of `MigrationConfig`** -> **Mitigation**: Maintain overloaded constructors taking standard `List<String>` and `Map<String, String>` that convert to Guava immutable collections.

## Migration Plan

1. **Build Logic**:
   - Add `compileOnly(libs.errorprone.annotations)` to `njall.java-common-conventions.gradle.kts`.
   - Add `"Immutable"` to `options.errorprone.error(...)`.
2. **Library Records**:
   - Annotate records in `:common` (`ServerConfig`, `TraceContext`, `ApiCall<@ImmutableTypeParameter T>`).
   - Annotate records in `:data` (domain entities, config records). Update `MigrationConfig` fields to `ImmutableList` and `ImmutableMap`.
   - Annotate records in `:api` (request/response DTOs, actor command and response protocols).
   - Annotate records in `:server` (`MigrationOptions`, `ServerOptions`).
3. **ArchUnit Rule**:
   - Add `records_must_be_annotated_immutable` ArchRule in `ArchitectureTest` (`:integration` **library module**).
4. **Verification**:
   - Run `./gradlew check build` to verify Spotless, Checkstyle, SpotBugs, ErrorProne, ArchUnit, and JaCoCo gates pass cleanly.

## Open Questions

- *Does this design conflict with ADR 0021?*
  - No. ADR 0021 established that records are pure data carriers without static factory methods. This design complements ADR 0021 by ensuring those pure data carriers are strictly and deeply immutable. ADR 0022 will be recorded to capture this decision.
