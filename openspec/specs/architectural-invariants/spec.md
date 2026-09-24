# architectural-invariants Specification

## Purpose

Enforces architectural invariants and structural code quality across Project Njall, including constructor injection visibility, package dependency hierarchy, and routing component organization.

## Requirements

### Requirement: Non-Public Visibility for Injected Constructors
The system SHALL enforce via ArchUnit in the `:integration` module that any constructor annotated with `@Inject` (`com.google.inject.Inject`, `jakarta.inject.Inject`, or `javax.inject.Inject`) or `@AssistedInject` (`com.google.inject.assistedinject.AssistedInject`) on non-test classes within package `com.larpconnect.njall..` is not declared `public`.

#### Scenario: ArchUnit architecture suite verifies non-public injected constructors
- **GIVEN** the compiled non-test classes in package `com.larpconnect.njall..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero constructors annotated with `@Inject` or `@AssistedInject` are declared `public`

### Requirement: Package Dependencies Down or Out, Never Up
The system SHALL enforce via ArchUnit in the `:integration` module that for any direct class dependency where both origin class and target class reside within the `com.larpconnect.njall` namespace, the target package MUST NOT be an ancestor package of the origin package. All internal package dependencies SHALL proceed only down to subpackages, out to sibling/peer packages, or within the same package.

#### Scenario: ArchUnit architecture suite verifies zero upward package dependencies
- **GIVEN** the compiled non-test classes in package `com.larpconnect.njall..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero classes depend on any class residing in an ancestor package within `com.larpconnect.njall`

### Requirement: RouteProvider Package Location
The system SHALL locate `RouteProvider` in package `com.larpconnect.njall.api.http`. Routing components in `com.larpconnect.njall.api.admin` and `com.larpconnect.njall.api.http` SHALL reference `RouteProvider` from `com.larpconnect.njall.api.http` without introducing upward dependencies on `com.larpconnect.njall.api`.

#### Scenario: RouteProvider is resolved from the HTTP subpackage
- **GIVEN** routing implementations and modules across `:api` implementing or referencing `RouteProvider`
- **WHEN** the codebase is compiled and inspected
- **THEN** `RouteProvider` resides in package `com.larpconnect.njall.api.http`
- **AND** zero classes in `com.larpconnect.njall.api.admin` or `com.larpconnect.njall.api.http` depend on `com.larpconnect.njall.api.RouteProvider`
