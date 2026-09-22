## Context

In Project Njall, dependency injection is structured as a Directed Acyclic Graph (DAG) across modules (`:common`, `:data`, `:api`, `:server`). Each package exposes a single Guice module responsible for declaring bindings, configuring object lifecycles, and installing lower-level modules.

Currently, two classes (`ActiveSessionFactories` in `:data` and `DefaultHttpServerService` in `:server`) declare the `@Singleton` annotation directly on their class declarations. This introduces unwanted coupling between concrete classes and the dependency injection framework, hides lifecycle scopes from developers reading the Guice module (as in `HttpServerModule`), and creates inconsistent conventions compared to other modules where `.in(Singleton.class)` is cleanly declared.

### C4 Component Diagram: Module-Governed Lifecycle Scoping

```
+─────────────────────────────────────────────────────────────────────────────+
|                              Container: Njall                               |
|                                                                             |
|  +─────────────────────────+            +────────────────────────────────+  |
|  |   ServerModule (Root)   |            |       DataModule (Root)        |  |
|  +────────────┬────────────+            +───────────────┬────────────────+  |
|               │ installs                                │ installs          |
|               v                                         v                   |
|  +─────────────────────────+            +────────────────────────────────+  |
|  |    HttpServerModule     |            |         SessionModule          |  |
|  |                         |            |                                |  |
|  | bind(HttpServerService) |            | bind(ActiveSessionFactories)   |  |
|  |   .to(...)              |            |   .in(Singleton.class)         |  |
|  |   .in(Singleton.class)  |            +───────────────┬────────────────+  |
|  +────────────┬────────────+                            │ instantiates      |
|               │ instantiates                            v                   |
|               v                         +────────────────────────────────+  |
|  +─────────────────────────+            |     ActiveSessionFactories     |  |
|  |DefaultHttpServerService |            |   (POJO - No @Singleton on cls)|  |
|  | (No @Singleton on class)|            +────────────────────────────────+  |
|  +─────────────────────────+                                                |
+─────────────────────────────────────────────────────────────────────────────+
```

## Goals / Non-Goals

**Goals:**
- Eliminate class-level `@Singleton` annotations across all Java classes in the repository.
- Declare singleton scoping explicitly in Guice modules using `.in(Singleton.class)`.
- Update `HttpServerModule` to explicitly declare `bind(HttpServerService.class).to(DefaultHttpServerService.class).in(Singleton.class)`.
- Add test assertions in `HttpServerModuleTest` to verify that `HttpServerService` resolves as a singleton.
- Update project documentation (`.agents/skills/guice/SKILL.md`) to clearly codify the prohibition of class-level scope annotations.

**Non-Goals:**
- Altering existing `@Provides @Singleton` methods in Guice modules (these reside in modules and already satisfy the module-scoping standard).
- Refactoring `Scopes.SINGLETON` in `ServerBindingModule` (functionally equivalent, though module-governed).
- Changing any runtime behavior, API schemas, or external application contracts.

## Decisions

### Decision: Remove `@Singleton` from Class Declarations
- **Choice**: Strip `@Singleton` and `import com.google.inject.Singleton;` from both `ActiveSessionFactories.java` and `DefaultHttpServerService.java`.
- **Rationale**: Keeps implementation classes decoupled from DI scoping semantics, allowing them to remain plain Java classes. Guice modules retain sole authority over component lifecycle.
- **Alternatives Considered**:
  - *Keep `@Singleton` on classes and omit module scope*: Rejected because it hides scoping from module inspection and violates repository design guidelines.
  - *Keep both class annotation and module `.in(Singleton.class)`*: Rejected as redundant, confusing, and prone to divergence.

### Decision: Explicit Binding Scope in `HttpServerModule`
- **Choice**: Update `bind(HttpServerService.class).to(DefaultHttpServerService.class)` to include `.in(Singleton.class)`.
- **Rationale**: `HttpServerService` manages socket bindings, ports, and termination lifecycles; it must be a singleton across the application. Without the class annotation, this scope must be defined in the module.

## Risks / Trade-offs

- **[Risk] Accidental Prototype Instantiation**: If an implementation class loses `@Singleton` and the module binding is omitted or lacks `.in(Singleton.class)`, multiple instances could be created.
  - **Mitigation**: Verify that `SessionModule` already includes `bind(ActiveSessionFactories.class).in(Singleton.class)` and explicitly add `.in(Singleton.class)` to `HttpServerModule`. Add unit tests asserting identity (`isSameAs`) for both bindings.
- **[Risk] Breaking Existing Tests**: Unit tests instantiating classes directly with `new` are unaffected because pure constructors do not care about Guice annotations. Tests using Guice injectors are covered by module tests.

## Migration Plan

1. Remove `@Singleton` from `ActiveSessionFactories.java`.
2. Remove `@Singleton` from `DefaultHttpServerService.java`.
3. Add `.in(Singleton.class)` and import to `HttpServerModule.java`.
4. Add assertion in `HttpServerModuleTest.java`.
5. Update `.agents/skills/guice/SKILL.md`.
6. Run `./gradlew check` to ensure formatting (Spotless), linting (Checkstyle, SpotBugs, ErrorProne), and 100% test passing.

## Open Questions

- None. All current ADRs (0001 through 0007) remain in force with zero conflicts.
