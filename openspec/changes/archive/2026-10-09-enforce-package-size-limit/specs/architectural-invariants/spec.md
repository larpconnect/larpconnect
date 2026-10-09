## ADDED Requirements

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
