## Context

Hibernate's `SessionFactory` is a heavyweight, stateful resource that initializes connection pools and dialect metadata upon creation. In Project Njall (LarpConnect), dual `SessionFactory` instances are configured in `:data` for `@NjallAdmin` and `@NjallUsers` (ADR 0005).

Currently, `DefaultServerDAO` injects `@NjallAdmin SessionFactory` directly as a bare constructor argument. In Guice, direct injection of `SessionFactory` forces immediate resolution during injector creation or DAO instantiation, which introduces risks of eager network calls, couples service lifecycles tightly to database availability, and prevents lazy initialization. Furthermore, as new DAOs are developed across the system, without structural guardrails developers may inadvertently introduce bare `SessionFactory` injections.

## Goals / Non-Goals

**Goals:**
- Prohibit bare `org.hibernate.SessionFactory` injection codebase-wide using ArchUnit architecture tests in `:integration`.
- Enforce that any injected dependency on `SessionFactory` is wrapped in a `Provider<SessionFactory>` (`com.google.inject.Provider` or `jakarta.inject.Provider`).
- Refactor `DefaultServerDAO` to inject `@NjallAdmin Provider<SessionFactory>` and obtain sessions on-demand via `sessionFactoryProvider.get().openSession()`.
- Update unit tests in `:data` to use supplier/provider lambdas.

**Non-Goals:**
- Prohibiting bare injection for lightweight, in-memory, or non-network dependencies (the rule is specifically scoped to Hibernate `SessionFactory`).
- Modifying how `SessionModule` configures or provides `SessionFactory` (Guice automatically provides `Provider<T>` for any bound type `T`).
- Altering the public `ServerDAO` interface or domain records.

## Architecture & C4 Component Layout

```
+-----------------------------------------------------------------------------------------+
|                                  C4: COMPONENT DIAGRAM                                  |
|                                                                                         |
|   +---------------------------------------------------------------------------------+   |
|   | Container: :data Module                                                         |   |
|   |                                                                                 |   |
|   |   +--------------------------+                                                  |   |
|   |   |      SessionModule       |                                                  |   |
|   |   | (@Provides SessionFactory|                                                  |   |
|   |   |   with @NjallAdmin)      |                                                  |   |
|   |   +------------+-------------+                                                  |   |
|   |                |                                                                |   |
|   |                | Guice auto-provides                                            |   |
|   |                v Provider<SessionFactory>                                       |   |
|   |   +------------+-------------+               +------------------------------+   |   |
|   |   |     DefaultServerDAO     |  queries via  | PostgreSQL (njall_admin)     |   |   |
|   |   | (@Inject Provider<SF>)   +-------------->| (njall.servers, contacts)    |   |   |
|   |   +--------------------------+  openSession  +------------------------------+   |   |
|   +---------------------------------------------------------------------------------+   |
|                                                                                         |
|   +---------------------------------------------------------------------------------+   |
|   | Container: :integration Module (Test Scope)                                     |   |
|   |                                                                                 |   |
|   |   +-------------------------------------------------------------------------+   |   |
|   |   | ArchitectureTest (package com.larpconnect.njall.integration.arch)       |   |   |
|   |   |   Rule: session_factory_must_not_be_injected_bare                       |   |   |
|   |   |   Analyzes: com.larpconnect.njall.. (All production classes)            |   |   |
|   |   +-------------------------------------------------------------------------+   |   |
|   +---------------------------------------------------------------------------------+   |
+-----------------------------------------------------------------------------------------+
```

## Decisions

### Decision 1: ArchUnit Test Placement in `:integration`
- **Choice**: Place `ArchitectureTest` in `integration/src/test/java/com/larpconnect/njall/integration/arch/ArchitectureTest.java` and add `testImplementation(libs.archunit.junit5)` to `integration/build.gradle.kts`.
- **Rationale**: Per `AGENTS.md` and repository topology, `:integration` sits downstream of all application modules (`:common`, `:data`, `:api`, `:server`). Placing whole-system architectural verification in `:integration` ensures that the class analyzer has access to the complete bytecode classpath of the entire application.
- **Alternatives Considered**:
  - *Colocating in `:data`*: Only protects `:data`, failing to detect accidental injections in `:server` or `:api`.
  - *Placing in `:test`*: `:test` is upstream of `:data` and `:server` (they depend on `:test`), which would violate DAG constraints.

### Decision 2: Target Scope of Invariant (Hibernate Only)
- **Choice**: Scope the prohibition specifically to `org.hibernate.SessionFactory` and its subtypes.
- **Rationale**: Bare injection is standard and idiomatic in Guice for lightweight services, immutable config objects, and utilities. The hazard of eager initialization is specific to heavyweight resources that connect over the network or establish thread/connection pools upon creation (such as Hibernate).
- **Alternatives Considered**:
  - *Enforcing Provider for all dependencies*: Anti-pattern that creates unnecessary indirection for simple services.

### Decision 3: Injection Target Inspection
- **Choice**: Check any constructor, method, or field annotated with `@Inject` (`com.google.inject.Inject`, `jakarta.inject.Inject`, `javax.inject.Inject`), or `@Provides`.
- **Rationale**: Guice injects values into annotated constructors, methods, and fields, as well as parameters of `@Provides` methods. Inspecting all injection vectors ensures zero bypasses.
- **Condition**: Flag if parameter/field raw type is assignable to `org.hibernate.SessionFactory`. Types wrapped in `com.google.inject.Provider<SessionFactory>` or `jakarta.inject.Provider<SessionFactory>` have raw type `Provider`, cleanly passing the check.

### Decision 4: `DefaultServerDAO` Refactoring
- **Choice**:
  ```java
  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultServerDAO(@NjallAdmin Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }
  ```
  And invoke `sessionFactoryProvider.get().openSession()` in `findById` and `list`.
- **Rationale**: Conforms to `nullability-contracts` spec (omitting redundant `requireNonNull` inside `@Inject` constructors). Defers session factory resolution until database methods are invoked. In unit tests, passing `() -> mockSessionFactory` provides a seamless test double.

## Risks / Trade-offs

- **[Risk] ArchUnit scan performance slowdown in integration test phase** -> *Mitigation*: ArchUnit imports bytecode from `com.larpconnect.njall..` which comprises a modest number of classes; caching is handled by JUnit Platform and Gradle task up-to-date checks.
- **[Risk] Accidental call to `.get()` inside constructor** -> *Mitigation*: The constructor only assigns the provider to `this.sessionFactoryProvider`, deferring `.get()` to query execution methods.

## Migration Plan

1. Add `testImplementation(libs.archunit.junit5)` to `integration/build.gradle.kts`.
2. Refactor `DefaultServerDAO` to accept and store `Provider<SessionFactory>`.
3. Update `DefaultServerDAOTest` to pass provider lambda `() -> sessionFactory`.
4. Implement `ArchitectureTest` in `:integration`.
5. Verify build with `./gradlew check build`.

## Open Questions

None. The scope, placement, and constraints have been explored and clarified with the user.
