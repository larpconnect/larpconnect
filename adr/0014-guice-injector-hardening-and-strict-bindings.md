# 0014: Guice Injector Hardening and Strict Bindings

- Status: accepted
- Date: 2026-09-22

## Context

Google Guice default settings allow runtime circular proxy generation, fallback to unannotated zero-argument constructors, and implicit Just-In-Time (JIT) binding synthesis for undeclared types. These fallbacks can mask missing module bindings, allow unintentional circular dependencies, and bypass architectural invariants.

Project Njall requires strict Directed Acyclic Graph (DAG) topology (AGENTS.md Section 4) and explicit dependency declaration across all application layers.

## Decision

1. **Root Injector Hardening in ServerModule**:
   - The root application module (`ServerModule`) MUST install the following hardening modules from `com.google.inject.util.Modules`:
     - `Modules.disableCircularProxiesModule()`: Prohibits dynamic proxy creation for circular dependencies.
     - `Modules.requireAtInjectOnConstructorsModule()`: Disables fallback to unannotated constructors, requiring `@Inject` on all constructed classes.
     - `Modules.requireExplicitBindingsModule()`: Disables JIT binding generation, requiring all injected types to be explicitly bound.
     - `Modules.requireExactBindingAnnotationsModule()`: Enforces exact matching on binding annotations.
2. **Explicit Component Bindings and Constructor Annotations**:
   - Injected concrete classes (such as `StudioAdminRoute`, `UserAdminRoute`, and `RoleAdminRoute` in `AdminModule`) MUST be explicitly bound in their owning module.
   - Any class instantiated by Guice constructor injection MUST declare a constructor annotated with `@Inject`, observing package-private visibility per ADR 0011 and ArchUnit rules.

## Consequences

### Positive
- Enforces strict DAG topology at injector bootstrap time with immediate fail-fast error reporting.
- Eliminates silent JIT binding synthesis, ensuring all dependencies are deliberately declared in their owning package modules.
- Prevents accidental instantiation of unannotated classes.
- Deepens compliance with project architectural invariants without adding external runtime dependencies.

### Negative
- Any newly injected concrete class must be explicitly bound in its module, slightly increasing module boilerplate.
- Classes constructed by Guice cannot rely on compiler-default no-argument constructors and must explicitly declare `@Inject` constructors.
