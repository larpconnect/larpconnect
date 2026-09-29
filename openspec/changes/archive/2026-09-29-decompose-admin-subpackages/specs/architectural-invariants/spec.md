## ADDED Requirements

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
