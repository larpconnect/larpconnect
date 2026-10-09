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

### Requirement: Admin Subpackage Structure and Upward Dependency Invariant
The system SHALL organize administrative components under `com.larpconnect.njall.api.admin` into domain-focused subpackages: `health`, `servers`, `studios`, `studioroles`, `users`, `roles`, and `common`. Non-test classes within each subpackage SHALL NOT depend upward on the ancestor package `com.larpconnect.njall.api.admin`. Shared administrative error payloads and validation logic SHALL reside in `com.larpconnect.njall.api.admin.common`. Every administrative subpackage containing public types SHALL include a `package-info.java` file annotated with `@org.jspecify.annotations.NullMarked`.

#### Scenario: ArchUnit architecture suite verifies zero upward dependencies from admin subpackages
- **GIVEN** the compiled non-test classes in `com.larpconnect.njall.api.admin..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero classes in `com.larpconnect.njall.api.admin.*` depend on any class residing in ancestor package `com.larpconnect.njall.api.admin`

#### Scenario: Administrative subpackages contain package-info with NullMarked
- **GIVEN** source packages `health`, `servers`, `studios`, `studioroles`, `users`, `roles`, and `common` under `com.larpconnect.njall.api.admin`
- **WHEN** the source directory tree is inspected
- **THEN** each subpackage contains a `package-info.java` file
- **AND** each `package-info.java` is annotated with `@NullMarked`

### Requirement: Admin Subpackage Guice Module Composition
The system SHALL provide a dedicated Guice **module** in each administrative subpackage (`HealthAdminModule`, `ServersAdminModule`, `StudiosAdminModule`, `StudioRolesAdminModule`, `UsersAdminModule`, `RolesAdminModule`, and `AdminCommonModule`). The top-level `AdminModule` in `com.larpconnect.njall.api.admin` SHALL install each subpackage **module** exactly one layer down, bind `AdminRoute` to `DefaultAdminRoute`, and bind `DefaultAdminRoute` into the `RouteProvider` multibinder. Individual subpackage modules SHALL NOT install sibling subpackage modules.

#### Scenario: Guice injector instantiates complete administrative routing graph
- **GIVEN** a Guice injector configured with `AdminModule` and its dependencies
- **WHEN** `AdminRoute` and `RouteProvider` instances are retrieved from the injector
- **THEN** the injector successfully instantiates `DefaultAdminRoute`
- **AND** `DefaultAdminRoute` aggregates routes provided by `HealthAdminRoute`, `ServersAdminRoute`, `StudioAdminRoute`, `StudioRoleAdminRoute`, `UserAdminRoute`, and `RoleAdminRoute`

#### Scenario: AdminModule exclusively installs direct subpackage modules
- **GIVEN** the `AdminModule` in package `com.larpconnect.njall.api.admin`
- **WHEN** `configure()` executes
- **THEN** `AdminModule` installs subpackage modules one level down (`HealthAdminModule`, `ServersAdminModule`, `StudiosAdminModule`, `StudioRolesAdminModule`, `UsersAdminModule`, `RolesAdminModule`, `AdminCommonModule`)
- **AND** does not directly register subpackage actor factories or subroutes

### Requirement: Package Size Limit and Composition Constraints
The system SHALL enforce via ArchUnit in the `:integration` **library module** that any package within the `com.larpconnect.njall..` namespace contains at most 20 combined records, interfaces, and classes. Enums SHALL NOT count toward the package type count. A class named `Default<InterfaceName>` paired with an `<InterfaceName>` residing in the same package SHALL count together as exactly 1 type. Static inner classes SHALL count toward the limit, excluding true `private` inner classes. Non-public Guice modules SHALL NOT count toward the limit. Every production package containing classes SHALL include a `package-info.java` file annotated with `@org.jspecify.annotations.NullMarked`, and `package-info` SHALL NOT count toward the limit.

#### Scenario: ArchUnit architecture suite verifies all production packages do not exceed twenty types
- **GIVEN** compiled non-test production classes within the `com.larpconnect.njall..` namespace
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero packages have an effective type count exceeding 20

#### Scenario: Paired default implementation and interface count as a single type
- **GIVEN** a package containing interface `StudioActorFactory` and implementation class `DefaultStudioActorFactory`
- **WHEN** the ArchUnit package size rule calculates the package type count
- **THEN** the interface and default implementation pair increments the count by exactly 1

#### Scenario: Enums and package-info are excluded from the type count
- **GIVEN** a package containing enum declarations and a `@NullMarked` `package-info.java`
- **WHEN** the ArchUnit package size rule calculates the package type count
- **THEN** enums and `package-info` do not increment the count

#### Scenario: Production package missing package-info is flagged as a violation
- **GIVEN** a package containing production classes within `com.larpconnect.njall..` lacking `package-info.java`
- **WHEN** the ArchUnit package size rule executes
- **THEN** ArchUnit detects and reports a condition violation for the missing `package-info.java`

#### Scenario: Private static inner classes do not increment package type count
- **GIVEN** a class declaring a `private static` inner class or record
- **WHEN** the ArchUnit package size rule calculates the package type count
- **THEN** the private static inner class is excluded from the type count

### Requirement: Studios Subpackage Structure and Route Aggregation
The system SHALL organize studio user-plane components under `com.larpconnect.njall.api.studios` into domain-focused subpackages: `common`, `links`, `locations`, and `addresses`. Classes within `common`, `links`, `locations`, and `addresses` SHALL NOT depend upward on the ancestor package `com.larpconnect.njall.api.studios`. The root `StudiosRoute` in `com.larpconnect.njall.api.studios` SHALL aggregate routes provided by `LinksRoute`, `LocationsRoute`, and `AddressesRoute`. Each subpackage containing public types SHALL expose a dedicated Guice **module** installed one layer down by `StudiosModule`.

#### Scenario: ArchUnit verifies zero upward dependencies from studios subpackages
- **GIVEN** compiled non-test classes in `com.larpconnect.njall.api.studios.*`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero classes in subpackages depend on any class residing in ancestor package `com.larpconnect.njall.api.studios`

#### Scenario: StudiosModule installs direct subpackage modules
- **GIVEN** the `StudiosModule` in package `com.larpconnect.njall.api.studios`
- **WHEN** Guice injector initialization executes
- **THEN** `StudiosModule` installs `LinksModule`, `LocationsModule`, and `AddressesModule` one level down

### Requirement: DAO Subpackage Structure and Persistence Composition
The system SHALL organize data access components under `com.larpconnect.njall.data.dao` into domain-focused subpackages: `common`, `studios`, `admin`, and `servers`. Non-test classes within each subpackage SHALL NOT depend upward on the ancestor package `com.larpconnect.njall.data.dao`. Base entity classes and common DAO interfaces SHALL reside in `com.larpconnect.njall.data.dao.common`. The top-level `DaoModule` in `com.larpconnect.njall.data.dao` SHALL install direct subpackage modules (`DaoCommonModule`, `StudiosDaoModule`, `AdminDaoModule`, and `ServersDaoModule`).

#### Scenario: ArchUnit verifies zero upward dependencies from DAO subpackages
- **GIVEN** compiled non-test classes in `com.larpconnect.njall.data.dao.*`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero classes in subpackages depend on any class residing in ancestor package `com.larpconnect.njall.data.dao`

#### Scenario: DaoModule installs direct subpackage DAO modules
- **GIVEN** the `DaoModule` in package `com.larpconnect.njall.data.dao`
- **WHEN** Guice injector initialization executes
- **THEN** `DaoModule` installs `DaoCommonModule`, `StudiosDaoModule`, `AdminDaoModule`, and `ServersDaoModule` one level down


