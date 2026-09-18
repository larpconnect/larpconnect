# 0008: Module-Level Singleton Scoping Convention

## Status

Accepted

## Date

2026-09-17

## Context

In Project Njall, Google Guice is the foundational dependency injection framework. Object lifecycles and scopes (specifically singletons) govern how services, factories, and components are instantiated and shared across the application.

Previously, scoping was inconsistent:
- Some classes (such as `DefaultHttpServerService` and `ActiveSessionFactories`) declared `@Singleton` directly on the class definition.
- Some modules (such as `HttpServerModule`) lacked explicit scope declarations on bindings, relying on the class annotation.
- Other modules (such as `SessionModule` and `MigrationModule`) explicitly bound components `.in(Singleton.class)`.

Annotating implementation classes directly couples domain and service classes to the DI framework, obscures lifecycle scopes during module inspection, and makes test isolation or alternate scoping difficult.

## Decision

1. **Prohibit `@Singleton` on Class Definitions**: Implementation classes across all modules (`:common`, `:data`, `:api`, `:server`) must not declare the `@Singleton` annotation. Implementation classes must remain plain Java classes decoupled from scope annotations.
2. **Mandate Module-Level Singleton Scoping**: All singleton bindings must be declared explicitly in their governing Guice module using `.in(Singleton.class)`.
3. **Permit `@Singleton` on Provider Methods**: Module provider methods (`@Provides @Singleton`) remain standard and permitted because they live directly inside the Guice module configuration layer.
4. **Standardize Scoping Syntax**: New and updated bindings must use `.in(Singleton.class)` consistently across all modules.

## Consequences

- **Positive**: Complete separation of concerns between business/infrastructure logic and dependency injection scoping; all component lifecycles are visible and auditable directly from module classes; unit testing and test-scoped overriding become simpler; eliminated redundancy.
- **Negative**: Removing `@Singleton` from an existing class requires ensuring the corresponding module binding explicitly specifies `.in(Singleton.class)` to prevent accidental prototype instantiation.
