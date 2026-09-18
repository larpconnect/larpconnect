# guice-singleton-scoping Specification

## Purpose

Enforces explicit module-level singleton scoping for Guice-managed services and components, prohibiting class-level `@Singleton` annotations on implementation classes.

## Requirements

### Requirement: Module-Level Singleton Scoping for HTTP Server Service
The system SHALL configure singleton lifecycle scoping for `HttpServerService` exclusively within `HttpServerModule` using `.in(Scopes.SINGLETON)`. The implementation class `DefaultHttpServerService` SHALL NOT declare the `@Singleton` annotation.

#### Scenario: HTTP server service is resolved as a singleton from Guice
- **GIVEN** the Guice injector is initialized with `HttpServerModule`
- **WHEN** `HttpServerService` is requested from the injector multiple times
- **THEN** the injector returns the identical instance for all requests
- **AND** `DefaultHttpServerService` declares no class-level `@Singleton` annotation

### Requirement: Class-Level Scope Annotation Prohibition on Active Session Factories
The system SHALL manage `ActiveSessionFactories` singleton lifecycle exclusively via module configuration in `SessionModule`. The `ActiveSessionFactories` class definition SHALL NOT declare the `@Singleton` annotation.

#### Scenario: Active session factories is resolved as a singleton without class annotation
- **GIVEN** the Guice injector is initialized with `SessionModule`
- **WHEN** `ActiveSessionFactories` is requested from the injector multiple times
- **THEN** the injector returns the identical instance for all requests
- **AND** `ActiveSessionFactories` declares no class-level `@Singleton` annotation

### Requirement: Strict Module-Governed Singleton Scoping Convention
All implementation classes across Project Njall modules SHALL rely strictly on Guice module declarations (`.in(Scopes.SINGLETON)` or `@Provides @Singleton`) to establish singleton lifecycles. No Java class definition SHALL declare `@Singleton`.

#### Scenario: Zero class definitions declare Singleton annotations
- **GIVEN** the Java source code across `:api`, `:common`, `:data`, and `:server`
- **WHEN** class definitions are inspected for scope annotations
- **THEN** zero class definitions declare the `@Singleton` annotation
- **AND** any singleton services or components are scoped explicitly in their respective Guice modules
