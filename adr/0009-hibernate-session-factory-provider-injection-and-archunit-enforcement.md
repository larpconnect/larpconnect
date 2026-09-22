# 0009: Hibernate SessionFactory Provider Injection and ArchUnit Enforcement

## Status

Accepted

## Date

2026-09-21

## Context

Hibernate's `SessionFactory` is a heavyweight, stateful resource that establishes database connections and dialect metadata upon initialization. In Project Njall, dual session factories are bound in `:data` for `@NjallAdmin` and `@NjallUsers` (ADR 0005).

Previously, `DefaultServerDAO` injected `@NjallAdmin SessionFactory` directly as a bare constructor parameter. Direct ("bare") injection of `SessionFactory` forces Guice to resolve and build the session factory immediately upon constructing the dependent component or initializing eager bindings. This eagerly couples component instantiation to database availability and prevents lazy initialization.

Furthermore, without automated architectural guardrails, subsequent DAOs or components developed across the system could inadvertently reintroduce bare `SessionFactory` injections.

## Decision

1. **Mandate Provider-Wrapped SessionFactory Injection**: Any component consuming a Hibernate `SessionFactory` must inject it wrapped in a Guice/Jakarta `Provider<SessionFactory>` (e.g. `@NjallAdmin Provider<SessionFactory>`). Components obtain sessions on-demand via `sessionFactoryProvider.get().openSession()`, deferring resolution until database operations are executed.
2. **ArchUnit Architectural Verification in `:integration`**: Introduce ArchUnit testing within `:integration` (package `com.larpconnect.njall.integration.arch.ArchitectureTest`). The architecture suite inspects all non-test classes across `com.larpconnect.njall..` to enforce that no constructor, method, or field annotated with `@Inject` (or method annotated with `@Provides`) declares a parameter or field of raw type `org.hibernate.SessionFactory`.
3. **Downstream Integration Verification**: `:integration` is chosen for architectural rule verification because it sits downstream of `:common`, `:data`, `:api`, and `:server`, providing complete visibility across the compiled application bytecode graph without introducing circular Gradle dependencies.

## Consequences

- **Positive**: Eliminates eager database connection initialization during Guice injector graph composition; enables lazy session factory initialization; enforces consistency across all present and future DAO implementations; provides automated compile/test-time failure via ArchUnit if bare injections are attempted.
- **Negative**: Adds a minor indirection (`sessionFactoryProvider.get().openSession()` instead of `sessionFactory.openSession()`); requires maintaining `archunit-junit5` test dependencies in `:integration`.
- **Follow-up**: Maintain the ArchUnit rule as additional DAOs and data-layer components are implemented.
