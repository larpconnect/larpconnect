## Context

In **Njall**, Java records represent strict data carriers. AGENTS.md establishes that *"record objects are strict data carriers and do not need factory methods."* In practice, however, 15 records across the codebase declare static factory methods. These fall into two categories:

1. **Simple Passthrough Factories**: Methods such as `ServerContact.of(...)`, `AdminRole.of(...)`, and `AdminErrorResponse.of(...)` that merely forward arguments to the canonical constructor.
2. **Complex Logic / Parsing Factories**: Methods such as `ServerConfig.fromConfig(...)`, `SessionConfig.fromConfig(...)`, and `TraceContext.parseTraceparent(...)` that perform configuration traversal, fallback resolution, and regex header parsing directly inside data carrier objects.

Furthermore, actor command records like `HealthCheckCommand.CheckHealth` carry an `Optional<TraceContext>` parameter, conflating domain request data with telemetry correlation metadata.

This design introduces an automated ArchUnit rule in `:integration` enforcing that records declare zero static factory methods returning the record type (or `Optional<Record>`), replaces simple factories with constructors, moves configuration parsing to Guice providers and assisted injection, and introduces a generic `ApiCall<T>` command envelope for telemetry.

## Goals / Non-Goals

**Goals:**
- Enforce via ArchUnit in `:integration` (`ArchitectureTest`) that all record classes in `com.larpconnect.njall..` declare no static methods returning the record type or `Optional<Record>`.
- Replace all static `.of(...)` factory methods across 15 records with direct canonical or overloaded constructors.
- Extract Typesafe `Config` parsing out of configuration records (`ServerConfig`, `DatabaseConfig`, `MigrationConfig`, `SessionConfig`) into Guice `@Provides` methods and a Guice assisted injection factory (`SessionConfigFactory`).
- Decouple telemetry correlation from Pekko Typed **Actor** commands by introducing `ApiCall<T>` in `:common` and extracting W3C traceparent parsing to a dedicated `TraceparentParser`.
- Pass all project verification gates (`./gradlew check build`) with 100% compliance.

**Non-Goals:**
- Banning static utility methods on records that return non-record types (e.g. validation predicates returning `boolean` or formatter methods returning `String`).
- Modifying external HTTP/REST API endpoints, status codes, or OpenAPI JSON payload schemas.

## Architectural Component Diagram (C4 Component Level)

```
+─────────────────────────────────────────────────────────────────────────────+
|                                  NJALL                                      |
+─────────────────────────────────────────────────────────────────────────────+
|                                                                             |
|  :integration (Library Module)                                              |
|  +───────────────────────────────────────────────────────────────────────+  |
|  | ArchitectureTest                                                      |  |
|  |  - records_must_not_declare_factory_methods ArchRule                   |  |
|  +───────────────────────────────────┬───────────────────────────────────+  |
|                                      │ verifies non-test classes            |
|                                      v                                      |
|  :common (Library Module)                                                   |
|  +───────────────────────────────+   +───────────────────────────────────+  |
|  | ServerConfig (Pure Record)    |   | ApiCall<T>(call, traceContext)    |  |
|  | TraceContext (Pure Record)    |   | TraceparentParser                 |  |
|  | ConfigModule (@Provides)      |   |                                   |  |
|  +───────────────────────────────+   +───────────────────────────────────+  |
|                                      ^                                      |
|                                      │ imports                              |
|  :data (Library Module)              │                                      |
|  +───────────────────────────────────┴───+   +───────────────────────────+  |
|  | Domain Records: Server, ServerContact |   | SessionConfigFactory      |  |
|  |                 AdminUser, Studio...  |   | (Assisted Injection)      |  |
|  | Config Records: DatabaseConfig,       |   | DatabaseConfigModule      |  |
|  |                 MigrationConfig, ...  |   | (@Provides methods)       |  |
|  +───────────────────────────────────────+   +───────────────────────────+  |
|                                      ^                                      |
|                                      │ imports                              |
|  :api (Library Module)               │                                      |
|  +───────────────────────────────────┴───+   +───────────────────────────+  |
|  | Request/Response DTO Records          |   | HealthCheckActor          |  |
|  | DefaultAdminRoute                     |   | (receives ApiCall<Cmd>)   |  |
|  | DefaultTracingDirective               |   |                           |  |
|  +───────────────────────────────────────+   +───────────────────────────+  |
+─────────────────────────────────────────────────────────────────────────────+
```

## Decisions

### Decision 1: ArchUnit Rule Structure for Record Factory Prohibition
- **Choice**: Enforce via ArchUnit that for all classes where `javaClass.isRecord()` is true within `com.larpconnect.njall..`, no static method has a return type matching the record class itself or `Optional<RecordClass>`.
- **Alternatives Considered**:
  - *Ban all static methods on records*: Too rigid; would prevent harmless static constants or pure utility functions that don't instantiate the record.
  - *Rely only on code review without ArchUnit*: Violates automated architectural invariant requirements and permits regressions.
- **Rationale**: Specifically targeting static methods that return the record type (or `Optional<Record>`) directly addresses the antipattern of pseudo-constructors and hidden factories while keeping records as strict value data carriers.

### Decision 2: Constructor Strategy for Value Records
- **Choice**: Eliminate all `.of(...)` factory methods. Use canonical constructors for direct instantiations (`new AdminRole(id, name)`). Where convenience or default arguments are required (e.g., `ServerConfig` defaulting port metadata, or `RoleAssignmentRequest` accepting either `roleId` or `roleName`), provide public overloaded constructors that delegate to the canonical constructor.
- **Alternatives Considered**:
  - *Keep package-private factory methods*: Fails the data carrier design principle; factory methods add indirection with zero benefit over constructors.
  - *Use builders*: Records in Java are concise value types. Builders introduce excessive class bloat for simple carriers with 1-5 fields.
- **Rationale**: Constructors are the idiomatic mechanism in Java for creating record instances. Overloaded constructors provide default argument handling without polluting the class with factory methods.

### Decision 3: Configuration Parsing via Providers and Assisted Injection
- **Choice**: Remove all static `fromConfig(...)` methods from `ServerConfig`, `DatabaseConfig`, `MigrationConfig`, and `SessionConfig`.
  - For root singletons (`ServerConfig`, `DatabaseConfig`, `MigrationConfig`): The parsing logic moves into `@Provides` methods inside `ConfigModule` and `DatabaseConfigModule`.
  - For path-scoped configurations (`SessionConfig`): Introduce `SessionConfigFactory` using Guice Assisted Injection (`@Assisted String path`), bound via `FactoryModuleBuilder` or a dedicated implementation injected with `Config`.
- **Alternatives Considered**:
  - *Create standalone non-injected static utility classes (e.g. `ServerConfigParser`)*: Misses the opportunity to integrate cleanly with Guice dependency injection and test fixture setups.
  - *Keep parsing logic inside record compact constructors*: Typesafe `Config` is a heavy external configuration abstraction that should not be a runtime dependency of pure data records.
- **Rationale**: Isolates configuration parsing entirely within the Guice dependency injection lifecycle, keeping records independent of Typesafe `Config`.

### Decision 4: Telemetry Separation via `ApiCall<T>` Command Envelope
- **Choice**: Introduce `public record ApiCall<T>(T call, Optional<TraceContext> context)` in `com.larpconnect.njall.common.telemetry`.
  - `HealthCheckCommand.CheckHealth` drops `Optional<TraceContext> traceContext`, retaining only `ActorRef<HealthCheckResponse> replyTo`.
  - `HealthCheckActor` processes `ApiCall<HealthCheckCommand>`, extracting MDC trace attributes uniformly from `apiCall.context()`.
  - `TraceContext` drops `fromSpan`, `fromSpanContext`, and `parseTraceparent` factory methods, replacing them with constructors `TraceContext(Span)` and `TraceContext(SpanContext)`.
  - Parsing W3C header strings moves into a dedicated `TraceparentParser` utility in `:common`.
- **Alternatives Considered**:
  - *Keep `traceContext` on each actor command*: Bloats every command protocol with repetitive telemetry parameters and requires custom extraction pattern matching in every actor.
  - *Use thread-local MDC without passing context in messages*: Pekko Typed actor message dispatching jumps threads asynchronously; thread-locals do not propagate across actor boundaries without explicit message-carried context or interceptors.
- **Rationale**: `ApiCall<T>` creates a clean separation of concerns: domain commands represent the business action, while `ApiCall` handles the infrastructure telemetry wrapper.

## Risks / Trade-offs

- **[Risk] High blast radius across multiple modules and tests** -> *Mitigation*: Perform refactoring in strict bottom-up DAG module dependency order (`:common` -> `:data` -> `:api` -> `:server` -> `:integration`), verifying module test suites at each step.
- **[Risk] ArchUnit false positives on non-factory static methods** -> *Mitigation*: The custom ArchCondition specifically inspects method return types, ensuring static methods returning other types (primitives, booleans, other domain types) are not flagged.

## Migration Plan

1. **Phase 1 (:common)**: Introduce `ApiCall<T>`, `TraceparentParser`, update `TraceContext` constructors, update `ServerConfig` constructors and `ConfigModule`, update common unit tests.
2. **Phase 2 (:data)**: Update `DatabaseConfig`, `MigrationConfig`, `SessionConfig`, introduce `SessionConfigFactory`, update `DatabaseConfigModule`, update domain entity records (`Server`, `ServerContact`, `AdminUser`, `StudioLookup`, `AdminRole`), update data unit tests.
3. **Phase 3 (:api)**: Update request DTOs (`AdminErrorResponse`, `CreateRoleRequest`, etc.), update `HealthCheckCommand` and `HealthCheckActor` to use `ApiCall`, update routes and API tests.
4. **Phase 4 (:server)**: Update server CLI and service bindings.
5. **Phase 5 (:integration)**: Implement the ArchUnit rule in `ArchitectureTest` and update integration Cucumber steps. Verify `./gradlew check build`.

## Open Questions

None. All architectural decisions were aligned during discovery.
