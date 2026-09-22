## Context

Project **Njall** uses Java 25, Guice 7, and Gradle in a multi-module architecture. The project verification pipeline incorporates ArchUnit in the `:integration` test suite to mechanically verify architectural invariants across all non-test classes in `com.larpconnect.njall..`.

Currently, ArchUnit only verifies that raw Hibernate `SessionFactory` instances are not injected bare (ADR-0009). Two key architectural hygiene rules are unverified:
1. Injected constructors should not be `public` to preserve encapsulation and enforce that object instantiation occurs via dependency injection rather than raw `new` calls outside the package.
2. Package dependencies within `com.larpconnect.njall` must proceed downward to subpackages or outward to sibling/peer packages, never upward to parent/ancestor packages.

Prior analysis revealed 4 public `@Inject` constructors in the **API plane** (`PekkoHealthCheck`, `RoleAdminRoute`, `StudioAdminRoute`, `UserAdminRoute`) and an upward dependency in the **library module** `:api` where subpackages `com.larpconnect.njall.api.admin` and `com.larpconnect.njall.api.http` depend upward on `com.larpconnect.njall.api.RouteProvider`.

## Goals / Non-Goals

**Goals:**
- Enforce via ArchUnit in `:integration` that any constructor annotated with `@Inject` (`com.google.inject.Inject`, `jakarta.inject.Inject`, `javax.inject.Inject`) or `@AssistedInject` (`com.google.inject.assistedinject.AssistedInject`) is non-public.
- Enforce via ArchUnit in `:integration` that no class within `com.larpconnect.njall` depends on an ancestor/parent package within `com.larpconnect.njall`.
- Relocate `RouteProvider` from `com.larpconnect.njall.api` to `com.larpconnect.njall.api.http` (Option A), eliminating existing upward package dependencies.
- Change the 4 public `@Inject` constructors in `com.larpconnect.njall.api.admin` to package-private.
- Ensure all quality gates (`./gradlew check build`) pass with zero ArchUnit violations.

**Non-Goals:**
- Restricting package dependencies between different top-level sibling subprojects (e.g. `:server` depending on `:api`, `:data`, or `:common`), which are governed by Gradle DAG module dependencies.
- Prohibiting package-private or protected constructor access from same-package unit test classes.
- Applying package direction constraints to third-party or Java standard library dependencies.

## C4 Architectural Diagrams

### C4 Component Diagram: ArchUnit Verification Scope

```
+----------------------------------------------------------------------------------+
|                            Project Njall Codebase                                |
|                                                                                  |
|   +--------------------------------------------------------------------------+   |
|   |                      :integration (Test Suite)                           |   |
|   |                                                                          |   |
|   |   +------------------------------------------------------------------+   |   |
|   |   |                       ArchitectureTest                           |   |   |
|   |   |                                                                  |   |   |
|   |   |  - session_factory_must_not_be_injected_bare                     |   |   |
|   |   |  - inject_constructors_must_not_be_public                        |   |   |
|   |   |  - package_dependencies_must_not_go_up                           |   |   |
|   |   +------------------------------------------------------------------+   |   |
|   +-------------------------------------|------------------------------------+   |
|                                         | inspects bytecode                      |
|                                         v                                        |
|   +-------------------+  +-------------------+  +----------------------------+   |
|   |       :api        |  |       :data       |  |          :server           |   |
|   |  (Library Module) |  |  (Library Module) |  |    (Application Module)    |   |
|   +-------------------+  +-------------------+  +----------------------------+   |
|             |                      |                          |                  |
|             +----------------------+--------------------------+                  |
|                                    |                                             |
|                                    v                                             |
|                          +-------------------+                                   |
|                          |      :common      |                                   |
|                          |  (Library Module) |                                   |
|                          +-------------------+                                   |
+----------------------------------------------------------------------------------+
```

### Component Package Hierarchy in `:api` (Option A)

```
                     +---------------------------------------+
                     |       com.larpconnect.njall.api       |
                     |             (ApiModule)               |
                     +---------------------------------------+
                                   |           |
               install submodules  | (Down)    | (Down)
                                   v           v
            +--------------------------+    +--------------------------+
            | ...api.http              |    | ...api.admin             |
            |                          |    |                          |
            | - RouteProvider [MOVED]  |<---| - AdminRoute             |
            | - RootRoute              |    | - AdminModule            |
            | - DefaultRootRoute       |    | - DefaultAdminRoute      |
            | - HttpModule             |    | - PekkoHealthCheck       |
            +--------------------------+    +--------------------------+
                         ^                               |
                         +----------------(Out)----------+
```

## Decisions

### Decision 1: Relocate `RouteProvider` to `com.larpconnect.njall.api.http` (Option A)
- **Choice**: Move `RouteProvider.java` from `com.larpconnect.njall.api` to `com.larpconnect.njall.api.http`.
- **Alternatives Considered**:
  - *Option B (Dedicated subpackage `com.larpconnect.njall.api.route`)*: Creates an extra package layer containing only one interface, introducing unnecessary directory sprawl for a single 16-line interface.
  - *Leaving in `com.larpconnect.njall.api` with an ArchUnit exemption*: Weakens the invariant and creates exceptional cases in static analysis rules.
- **Rationale**: `RouteProvider` defines the HTTP routing contribution contract, which directly relates to the HTTP routing infrastructure in `com.larpconnect.njall.api.http`. Subpackage `api.admin` depends "out" across sibling packages to `api.http`, conforming to the "down or out, never up" invariant.

### Decision 2: Constructor Non-Public Rule Structure in ArchUnit
- **Choice**: Implement an ArchRule on `constructors()` with an annotation predicate:
  ```java
  @ArchTest
  public static final ArchRule inject_constructors_must_not_be_public =
      constructors()
          .that(areAnnotatedWithInjectOrAssistedInject())
          .should()
          .notBePublic()
          .as("Constructors annotated with @Inject or @AssistedInject must not be public");
  ```
- **Rationale**: ArchUnit's `constructors().should().notBePublic()` provides clear, idiomatic failure messages indicating the exact constructor violating visibility rules. Using a custom predicate checking all standard `@Inject` annotations (`com.google.inject.Inject`, `jakarta.inject.Inject`, `javax.inject.Inject`, and `com.google.inject.assistedinject.AssistedInject`) guarantees comprehensive coverage across DI flavors.

### Decision 3: Package Hierarchy Direction Verification Algorithm
- **Choice**: Implement a custom `ArchCondition<JavaClass>`:
  - For each `JavaClass` in `com.larpconnect.njall..`:
    - For each `Dependency` in `javaClass.getDirectDependenciesFromSelf()`:
      - Let `originPkg = javaClass.getPackageName()`.
      - Let `targetPkg = dependency.getTargetClass().getPackageName()`.
      - If `targetPkg` is within `com.larpconnect.njall`:
        - Test whether `targetPkg` is an ancestor of `originPkg`: `!originPkg.equals(targetPkg) && originPkg.startsWith(targetPkg + ".")`.
        - If true, emit a condition violation.
- **Rationale**: String prefix matching with trailing dot delimiter (`originPkg.startsWith(targetPkg + ".")`) provides an exact, zero-overhead mathematical definition of package ancestry in Java namespaces without requiring external AST parsing.

## Risks / Trade-offs

- **[Risk] Test classes might accidentally trigger package dependency violations** -> *Mitigation*: `@AnalyzeClasses` in `ArchitectureTest` uses `ImportOption.DoNotIncludeTests.class`, ensuring unit and integration tests (which often inspect package internals) are excluded from the production package DAG checks.
- **[Risk] External callers attempting raw instantiation of modified classes** -> *Mitigation*: All 4 classes with remediated constructors (`PekkoHealthCheck`, `RoleAdminRoute`, `StudioAdminRoute`, `UserAdminRoute`) are only instantiated within tests in their same package or via Guice injector bindings. None have public API consumers expecting `new` calls.

## Migration Plan

1. In `:api`, move `RouteProvider.java` to `com.larpconnect.njall.api.http.RouteProvider`.
2. Update imports in `DefaultRootRoute.java`, `HttpModule.java`, `AdminRoute.java`, and `AdminModule.java`.
3. In `PekkoHealthCheck.java`, `RoleAdminRoute.java`, `StudioAdminRoute.java`, and `UserAdminRoute.java`, remove `public` from `@Inject` constructors.
4. In `:integration`, update `ArchitectureTest.java` with the two new ArchUnit rules.
5. Run `./gradlew check build` to verify compilation, test passage, and formatting.

## Open Questions

None. All architectural decisions (Option A for `RouteProvider` location, non-public `@Inject` constructor enforcement, and ancestry matching algorithm) have been resolved.
