# data-persistence Specification

## Purpose

Provides sealed base DAO and DatabaseObject abstractions, dual role-scoped Hibernate session factories for admin and user roles, and read-only domain queries for servers and contacts.

## Requirements

### Requirement: Sealed Database Object and Base DAO Abstraction
The system SHALL provide a sealed `DatabaseObject` interface representing domain records with a UUID identifier and a sealed `DAO<T extends DatabaseObject>` interface exposing `findById(UUID id)` returning `Optional<T>` and `list()` returning `ImmutableList<T>`.

#### Scenario: Query database object by primary key
- **GIVEN** a persisted entity with a known UUID identifier in the database
- **WHEN** a client invokes `findById(UUID id)` on the DAO
- **THEN** the system returns an `Optional` containing the populated domain record

#### Scenario: Query all database objects
- **GIVEN** one or more persisted entities present in the database
- **WHEN** a client invokes `list()` on the DAO
- **THEN** the system returns an `ImmutableList` containing all corresponding domain records

### Requirement: Dual Role-Scoped Hibernate Session Factories
The system SHALL provide two isolated Hibernate `SessionFactory` instances managed via Guice: one qualified with `@NjallAdmin` connecting as database role `njall_admin`, and one qualified with `@NjallUsers` connecting as database role `njall_users`. Each session factory SHALL configure dedicated connection pooling with configurable minimum and maximum pool sizes and connection timeout durations. Database passwords SHALL be required by default; connecting without a non-blank password SHALL fail fast during configuration ingestion unless `trust-auth = true` is explicitly configured for the profile or globally. If a non-blank password is provided, it SHALL be used for authentication regardless of the `trust-auth` setting. If a password is blank or omitted and `trust-auth = true` is configured, the system SHALL omit the JDBC password property to support trust-authenticated connections. Consuming components SHALL NOT inject `SessionFactory` instances bare; any component requiring a session factory SHALL inject a `Provider<SessionFactory>` to ensure deferred resolution.

#### Scenario: Inject administrative session factory
- **GIVEN** the Guice dependency injection container is initialized
- **WHEN** a component requests a `Provider<SessionFactory>` annotated with `@NjallAdmin`
- **THEN** the container provides a provider that resolves a `SessionFactory` configured to execute queries with `njall_admin` role credentials

#### Scenario: Inject user session factory
- **GIVEN** the Guice dependency injection container is initialized
- **WHEN** a component requests a `Provider<SessionFactory>` annotated with `@NjallUsers`
- **THEN** the container provides a provider that resolves a `SessionFactory` configured to execute queries with `njall_users` role credentials

#### Scenario: Missing session password without trust-auth fails fast during configuration ingestion
- **GIVEN** a database configuration where the `admin` profile has a blank or omitted password
- **AND** `trust-auth` is not set to `true` globally or on the profile
- **WHEN** configuration parsing occurs via `SessionConfig.fromConfig` or `DatabaseConfig.fromConfig`
- **THEN** configuration ingestion throws an `IllegalStateException` identifying the unauthenticated profile

#### Scenario: Session profile connects without password when trust-auth is opted in
- **GIVEN** a database configuration where the `users` profile has an empty password
- **AND** `trust-auth` is explicitly configured as `true`
- **WHEN** the `SessionConfig` is parsed and used to build a Hibernate registry
- **THEN** configuration ingestion succeeds
- **AND** the JDBC password setting is omitted from the session registry

### Requirement: Read-Only Server and Contact Domain Models
The system SHALL provide immutable `Server` and `ServerContact` records representing records in `njall.servers` and `njall.server_contacts`. The `ServerDAO` SHALL be read-only, using a `@NjallAdmin Provider<SessionFactory>` to obtain sessions on-demand to query servers and associated contacts without exposing write, update, or delete operations.

#### Scenario: Retrieve server with associated contact records
- **GIVEN** the database contains a server record in `njall.servers` and associated contact records in `njall.server_contacts`
- **WHEN** `ServerDAO.list()` is invoked
- **THEN** the system returns an `ImmutableList<Server>` where each `Server` contains its associated contacts ordered by `ordering` ascending
- **AND** the returned records are immutable and do not allow state mutation

### Requirement: Architectural Enforcement Against Bare SessionFactory Injection
The system SHALL enforce via ArchUnit in the `:integration` module that no non-test class in package `com.larpconnect.njall..` injects a raw `org.hibernate.SessionFactory` into `@Inject` constructors, methods, fields, or `@Provides` methods. Any injected dependency on a Hibernate `SessionFactory` MUST be wrapped in a `Provider<SessionFactory>`.

#### Scenario: ArchUnit architecture suite verifies zero bare SessionFactory injections
- **GIVEN** the compiled classes across all production modules
- **WHEN** the ArchUnit test suite in `:integration` executes
- **THEN** zero classes inject a bare `SessionFactory` into an `@Inject` constructor, method, field, or `@Provides` method
- **AND** all existing session factory consumers inject `Provider<SessionFactory>`

### Requirement: Admin Domain Records and Sealed DatabaseObject Hierarchy
The system SHALL expand the sealed `DatabaseObject` hierarchy to permit `Server`, `AdminUser`, `AdminRole`, and `StudioLookup`. All domain records SHALL be immutable and provide UUID primary identifiers via `id()`. `StudioLookup` SHALL expose both `tenantId()` and `studioId()`, with `id()` mapping to `studioId()`.

#### Scenario: AdminUser implements DatabaseObject
- **GIVEN** an `AdminUser` record instance
- **WHEN** checked against `DatabaseObject`
- **THEN** it is an instance of `DatabaseObject` and returns a non-null UUID identifier from `id()`

#### Scenario: StudioLookup exposes tenantId and studioId
- **GIVEN** a `StudioLookup` record created from persistent data
- **WHEN** methods `tenantId()` and `studioId()` are invoked
- **THEN** both return valid non-null UUIDs, and `id()` equals `studioId()`

### Requirement: Admin DAOs with Mutation Operations
The system SHALL provide `AdminUserDAO`, `AdminRoleDAO`, and `StudioDAO` interfaces extending `DAO<T>`. In addition to standard `findById(UUID id)` and `list()`, `AdminUserDAO` SHALL provide `findByUsername(String username)`, `create(String username, AdminUserStatus status, List<UUID> roleIds)`, `addRole(UUID userId, UUID roleId)`, and `removeRole(UUID userId, UUID roleId)`. `AdminRoleDAO` SHALL provide `findByRoleName(String roleName)` and `create(String roleName)`. `StudioDAO` SHALL provide `findByAlias(String alias, DeletionFilter filter)`, `findById(UUID studioId, DeletionFilter filter)`, `list(DeletionFilter filter)`, `create(String alias)`, and `softDelete(UUID studioId)`. Parameterless overloads `findByAlias(String alias)`, `findById(UUID studioId)`, and `list()` SHALL default to `DeletionFilter.ACTIVE_ONLY`. All implementations SHALL inject `@NjallAdmin Provider<SessionFactory>`.

#### Scenario: Create and retrieve admin user via AdminUserDAO
- **GIVEN** an active Hibernate session via `@NjallAdmin Provider<SessionFactory>`
- **WHEN** `AdminUserDAO.create("admin_tyr", AdminUserStatus.ACTIVE, List.of())` is invoked
- **THEN** a new `admin_users` record is persisted with a generated UUIDv7
- **AND** `AdminUserDAO.findByUsername("admin_tyr")` returns the persisted user

#### Scenario: Assign and unassign role via AdminUserDAO
- **GIVEN** a persisted admin user and persisted role
- **WHEN** `AdminUserDAO.addRole(userId, roleId)` is executed
- **THEN** an entry is inserted into `admin_role_assignments`
- **AND** subsequent `removeRole(userId, roleId)` deletes the junction entry

#### Scenario: StudioDAO filters soft-deleted records by default
- **GIVEN** a studio with `deleted_at` timestamp set
- **WHEN** `StudioDAO.list(DeletionFilter.ACTIVE_ONLY)` or `StudioDAO.list()` is invoked
- **THEN** the soft-deleted studio is excluded from the returned list
- **AND** `StudioDAO.list(DeletionFilter.INCLUDE_DELETED)` includes the soft-deleted studio

### Requirement: Admin JPA Entities Registration
The system SHALL register `AdminUserEntity`, `AdminRoleEntity`, and `StudioLookupEntity` in the `@NjallAdmin` entity multibinder within `DaoModule`, making them accessible to the Hibernate `@NjallAdmin SessionFactory`.

#### Scenario: Admin JPA entities are registered in DaoModule
- **GIVEN** the Guice injector with `DataModule` installed
- **WHEN** the `@NjallAdmin Set<Class<?>>` entity multibinding is resolved
- **THEN** the set contains `ServerEntity.class`, `ServerContactEntity.class`, `AdminUserEntity.class`, `AdminRoleEntity.class`, and `StudioLookupEntity.class`

