## Context

The **Njall** server architecture is assembled through Google Guice dependency injection. By default, Guice operates in a permissive mode:
1. When circular dependencies occur, Guice attempts to synthesize dynamic bytecode proxies. This undermines Section 4 of `AGENTS.md` ("Strict DAG Enforcements: Circular dependencies between packages or Gradle modules are absolutely forbidden").
2. When a class declares a zero-argument constructor without `@Inject`, Guice will silently instantiate it via reflection. This bypasses explicit constructor injection standards and weakens static verification.
3. When an injection point requests an unbound concrete class, Guice synthesizes a Just-In-Time (JIT) binding. This permits undeclared dependencies that bypass package-to-module architectural boundaries.
4. When annotations with parameters differ, Guice may bind loosely unless exact matching is enforced.

To reinforce system integrity, the root **Server** **application module** (`ServerModule`) installs four hardening modules from `com.google.inject.util.Modules`: `disableCircularProxiesModule()`, `requireAtInjectOnConstructorsModule()`, `requireExplicitBindingsModule()`, and `requireExactBindingAnnotationsModule()`.

```
+──────────────────────────────────────────────────────────────────────────────+
|            C4 COMPONENT DIAGRAM: ROOT GUICE INJECTOR HARDENING               |
+──────────────────────────────────────────────────────────────────────────────+
|                                                                              |
|  [ServerApp Bootstrap / Test Harness]                                        |
|         │                                                                    |
|         │ creates injector                                                   |
|         ▼                                                                    |
|  +────────────────────────────────────────────────────────────────────────+  |
|  | Application Module: ServerModule                                       |  |
|  |                                                                        |  |
|  |  +─────────────────────────+     +─────────────────────────+           |  |
|  |  | disableCircularProxies  |     | requireAtInjectOn       |           |  |
|  |  | Module                  |     | ConstructorsModule      |           |  |
|  |  +───────────┬─────────────+     +───────────┬─────────────+           |  |
|  |              │                               │                         |  |
|  |  +───────────┴─────────────+     +───────────┴─────────────+           |  |
|  |  | requireExplicitBindings |     | requireExactBinding     |           |  |
|  |  | Module                  |     | AnnotationsModule       |           |  |
|  |  +───────────┬─────────────+     +─────────────────────────+           |  |
|  +──────────────┼───────────────────────────────┼─────────────────────────+  |
|                 │                               │                            |
|                 │ enforces explicit bindings    │ enforces @Inject           |
|                 ▼                               ▼                            |
|  +────────────────────────────+   +───────────────────────────────────────+  |
|  | API Plane: AdminModule     |   | Data Plane: Session & Migration       |  |
|  |                            |   |                                       |  |
|  | - bind(StudioAdminRoute)   |   | - ActiveSessionFactories: @Inject     |  |
|  | - bind(UserAdminRoute)     |   | - DefaultDataSourceFactory: @Inject   |  |
|  | - bind(RoleAdminRoute)     |   | - DefaultFlywayFactory: @Inject       |  |
|  | - bind(AdminRoute)         |   | - DefaultSessionFactoryFactory        |  |
|  +────────────────────────────+   +───────────────────────────────────────+  |
|                                                                              |
+──────────────────────────────────────────────────────────────────────────────+
```

### Diagram Explanation
- **Boundaries**: `ServerModule` operates as the top-level application compositor. Hardening flags apply across the root injector and all submodules (`ApiModule`, `DataModule`, `CommonModule`, `HttpServerModule`, `CliModule`, `ServerBindingModule`).
- **Responsibilities**: `ServerModule` enforces architectural invariants during Guice bootstrap. `AdminModule` provides explicit bindings for sub-routes. Data classes declare explicit constructor injection.
- **Key Relationships**: `DefaultAdminRoute` depends on `StudioAdminRoute`, `UserAdminRoute`, and `RoleAdminRoute`, which are now resolved via explicit bindings rather than JIT synthesis.
- **Assumptions**: Hardening is installed exclusively in the root `ServerModule`; subordinate **library modules** retain flexibility during isolated unit testing unless explicitly composed into the full server graph.
- **Open Questions**: None; confirmed that all 4 hardening modules are installed in `ServerModule` only.

## Goals / Non-Goals

**Goals:**
- Install `disableCircularProxiesModule()`, `requireAtInjectOnConstructorsModule()`, `requireExplicitBindingsModule()`, and `requireExactBindingAnnotationsModule()` in `ServerModule.configure()`.
- Explicitly declare `bind(StudioAdminRoute.class)`, `bind(UserAdminRoute.class)`, and `bind(RoleAdminRoute.class)` in `AdminModule.configure()`.
- Add package-private `@Inject` constructors to `ActiveSessionFactories`, `DefaultDataSourceFactory`, and `DefaultFlywayFactory`.
- Add unit tests in `ServerModuleTest` verifying that circular dependencies, unannotated constructors, and missing explicit bindings cause injector configuration errors.

**Non-Goals:**
- Installing hardening modules in individual library module `configure()` methods (e.g., `DataModule`, `ApiModule`, `CommonModule`).
- Modifying ArchUnit rules or external REST API contracts.

## Decisions

### Decision 1: Install hardening modules via `install(Modules.<call>())` in `ServerModule`
We configure hardening rules in `ServerModule.configure()` using standard Guice factory methods:
```java
install(Modules.disableCircularProxiesModule());
install(Modules.requireAtInjectOnConstructorsModule());
install(Modules.requireExplicitBindingsModule());
install(Modules.requireExactBindingAnnotationsModule());
```
*Rationale*: Using `Modules.*Module()` encapsulates the binder configuration into standard Guice `Module` instances, keeping `ServerModule.configure()` declarative and uniform with other `install(...)` statements.
*Alternatives Considered*: Calling `binder().disableCircularProxies()`, `binder().requireExplicitBindings()`, etc., directly. Both have identical runtime semantics; installing via `Modules.*Module()` is idiomatic and matches Guice utility design.

### Decision 2: Add explicit route bindings in `AdminModule`
`DefaultAdminRoute` constructor-injects `StudioAdminRoute`, `UserAdminRoute`, and `RoleAdminRoute`. With JIT bindings disabled, `AdminModule` explicitly registers each concrete class:
```java
bind(StudioAdminRoute.class);
bind(UserAdminRoute.class);
bind(RoleAdminRoute.class);
```
*Rationale*: Preserves package-to-module parity (Section 4 of `AGENTS.md`) where the owning module explicitly exposes all routes it provides.
*Alternatives Considered*: Introducing separate interfaces for each route. Rejected as unnecessary over-abstraction since routes are concrete Pekko directive aggregators that do not have alternative implementations.

### Decision 3: Declare package-private `@Inject` constructors on bound data components
`ActiveSessionFactories`, `DefaultDataSourceFactory`, and `DefaultFlywayFactory` are updated with package-private constructors annotated with `@Inject`.
*Rationale*: Satisfies `requireAtInjectOnConstructors` while adhering to `inject_constructors_must_not_be_public` in `ArchitectureTest` and ADR 0011.
*Alternatives Considered*: Binding them via `toInstance(...)` or provider methods. Adding explicit `@Inject` constructors is preferred because it maintains consistent lazy instantiation and standard Guice lifecycle management.

## Risks / Trade-offs

- **[Risk] A future concrete class injected without an explicit binding fails at startup** -> *Mitigation*: The failure occurs immediately at injector initialization with an explanatory Guice `ConfigurationException`, caught by unit and integration test suites during `./gradlew check build`.
- **[Risk] Subordinate library module unit tests might pass with JIT bindings while failing in root ServerModule** -> *Mitigation*: Integration tests (`:integration`) and `ServerModuleTest` boot the full `ServerModule` and guarantee end-to-end verification.

## Migration Plan

1. Update `:data` classes (`ActiveSessionFactories`, `DefaultDataSourceFactory`, `DefaultFlywayFactory`) with package-private `@Inject` constructors.
2. Update `:api` `AdminModule` with explicit route bindings.
3. Update `:server` `ServerModule` with the four hardening modules.
4. Add verification tests in `ServerModuleTest`.
5. Execute `./gradlew check build` to verify the entire test suite passes.

## Open Questions

None. Prior in-force ADRs (0001 through 0013) have been reviewed; the root injector hardening decision will be recorded in durable ADR 0014.
