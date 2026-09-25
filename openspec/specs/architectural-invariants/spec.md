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

### Requirement: Record Objects Must Not Declare Static Factory Methods
The system SHALL enforce via ArchUnit in the `:integration` **library module** that any `record` class within the `com.larpconnect.njall..` namespace MUST NOT declare static factory methods that return either the record's own type or an `Optional` containing the record's type. All record construction SHALL occur through public canonical or overloaded constructors, or through external Guice-managed factories or providers. Static utility methods returning types other than the record or `Optional` of the record SHALL remain permitted.

#### Scenario: ArchUnit architecture suite detects zero static factory methods on record classes
- **GIVEN** the compiled non-test classes in package `com.larpconnect.njall..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero `record` classes declare static methods whose return type is the record type or an `Optional` of the record type

#### Scenario: Static utility functions on record classes are permitted
- **GIVEN** a `record` class within `com.larpconnect.njall..` that declares a static method returning a boolean, primitive, or unrelated type
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** ArchUnit treats the method as an allowed utility function and passes without violations

### Requirement: Record Objects Must Be Annotated with ErrorProne Immutable
The system SHALL enforce via ArchUnit in the `:integration` **library module** that any `record` class within the `com.larpconnect.njall..` namespace (excluding test classes) MUST be annotated with `@com.google.errorprone.annotations.Immutable`.

#### Scenario: ArchUnit architecture suite verifies all record classes are annotated with @Immutable
- **GIVEN** the compiled non-test classes in package `com.larpconnect.njall..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero `record` classes lack the `@com.google.errorprone.annotations.Immutable` annotation

#### Scenario: Non-record classes are not required to have @Immutable
- **GIVEN** compiled non-test classes in package `com.larpconnect.njall..` that are not records
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** ArchUnit ignores non-record classes for this rule and passes without violations

#### Scenario: Test records are exempted from @Immutable enforcement
- **GIVEN** record classes located within test packages or test source directories
- **WHEN** the ArchUnit architecture test suite executes with test exclusion import options
- **THEN** ArchUnit does not evaluate test records for `@Immutable`

### Requirement: Record Components Must Not Be Annotated With Nullable
The system SHALL enforce via ArchUnit in the `:integration` **library module** that any `record` class within the `com.larpconnect.njall..` namespace (excluding test classes) MUST NOT declare record components annotated with `org.jspecify.annotations.Nullable`. All record state SHALL be held in non-null types using `java.util.Optional<T>`, empty immutable collections, or sentinel enumeration constants. Record constructors MAY accept `@Nullable` parameters to perform boundary normalization.

#### Scenario: ArchUnit architecture suite detects zero record components annotated with @Nullable
- **GIVEN** the compiled non-test classes in package `com.larpconnect.njall..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero `record` classes declare record components annotated with `@Nullable`

#### Scenario: Record constructors with @Nullable parameters are permitted for boundary normalization
- **GIVEN** a `record` class within `com.larpconnect.njall..` declaring an overloaded constructor or compact constructor accepting a parameter annotated with `@Nullable`
- **WHEN** the ArchUnit architecture test suite in `:integration` evaluates the record class
- **THEN** ArchUnit passes without violation provided the record component itself lacks `@Nullable`
