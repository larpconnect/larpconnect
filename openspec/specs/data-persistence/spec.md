# data-persistence Specification

## Purpose

Provides sealed base DAO and DatabaseObject abstractions, dual role-scoped Hibernate session factories for admin and user roles, and read-only domain queries for servers and contacts.

## Requirements

### Requirement: Sealed Database Object and Base DAO Abstraction
The system SHALL provide a sealed `DatabaseObject` interface representing domain records with a UUID identifier and a sealed `DAO<T extends DatabaseObject>` interface exposing `findById(UUID id)` returning `Optional<T>` and `list()` returning `ImmutableList<T>`. The `DAO` sealed interface SHALL permit `ServerDAO`, `AdminUserDAO`, `AdminRoleDAO`, `StudioLookupDAO`, `StudioDAO`, `StudioRoleDAO`, `LinkDAO`, `LocationDAO`, and `AddressDAO`.

#### Scenario: Query database object by primary key
- **GIVEN** a persisted entity with a known UUID identifier in the database
- **WHEN** a client invokes `findById(UUID id)` on the **DAO**
- **THEN** the system returns an `Optional` containing the populated domain record

#### Scenario: Query all database objects
- **GIVEN** one or more persisted entities present in the database
- **WHEN** a client invokes `list()` on the **DAO**
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
The system SHALL provide `AdminUserDAO`, `AdminRoleDAO`, and `StudioLookupDAO` interfaces extending `DAO<T>`. In addition to standard `findById(UUID id)` and `list()`, `AdminUserDAO` SHALL provide `findByUsername(String username)`, `create(String username, AdminUserStatus status, List<UUID> roleIds)`, `addRole(UUID userId, UUID roleId)`, and `removeRole(UUID userId, UUID roleId)`. `AdminRoleDAO` SHALL provide `findByRoleName(String roleName)` and `create(String roleName)`. `StudioLookupDAO` SHALL provide `findByAlias(String alias, DeletionFilter filter)`, `findById(UUID studioId, DeletionFilter filter)`, `list(DeletionFilter filter)`, `create(String alias, String name)`, `create(String alias)`, and `softDelete(UUID studioId)`. Parameterless overloads `findByAlias(String alias)`, `findById(UUID studioId)`, and `list()` SHALL default to `DeletionFilter.ACTIVE_ONLY`. When creating a new studio lookup, `StudioLookupDAO` SHALL insert the corresponding **Tenant** record into `njall_users.studios` before inserting the routing entry into `njall_admin.studios_lookup`. All implementations SHALL inject `@NjallAdmin Provider<SessionFactory>`.

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
- **WHEN** `StudioLookupDAO.list(DeletionFilter.ACTIVE_ONLY)` or `StudioLookupDAO.list()` is invoked
- **THEN** the soft-deleted studio is excluded from the returned list
- **AND** `StudioLookupDAO.list(DeletionFilter.INCLUDE_DELETED)` includes the soft-deleted studio

### Requirement: Admin JPA Entities Registration
The system SHALL register `AdminUserEntity`, `AdminRoleEntity`, and `StudioLookupEntity` in the `@NjallAdmin` entity multibinder within `DaoModule`, making them accessible to the Hibernate `@NjallAdmin SessionFactory`.

#### Scenario: Admin JPA entities are registered in DaoModule
- **GIVEN** the Guice injector with `DataModule` installed
- **WHEN** the `@NjallAdmin Set<Class<?>>` entity multibinding is resolved
- **THEN** the set contains `ServerEntity.class`, `ServerContactEntity.class`, `AdminUserEntity.class`, `AdminRoleEntity.class`, and `StudioLookupEntity.class`

### Requirement: Administrative Database Health Probe
The system SHALL provide an administrative database health probe implementing Dropwizard `HealthCheck` in the **Data plane** (`com.larpconnect.njall.data.health.AdminDatabaseHealthCheck`). The health probe SHALL evaluate database availability by executing a native query equivalent to `SELECT 1` with an explicit 1-second query timeout against the Hibernate `SessionFactory` configured for the `njall_admin` role. The probe SHALL cache probe results in an in-memory Caffeine cache with an expiration duration of 10 seconds (`expireAfterWrite`) to limit query frequency to at most once every 10 seconds. The probe SHALL support deterministic testing by accepting an optional custom cache duration and `Ticker`.

#### Scenario: Database ping returns healthy
- **GIVEN** the PostgreSQL database is reachable and accepting connections for the `njall_admin` role
- **WHEN** the administrative database health probe is evaluated
- **THEN** the probe executes `SELECT 1` on the `njall_admin` session
- **AND** the probe returns a healthy `Result`

#### Scenario: Subsequent health check evaluations within cache window return cached result
- **GIVEN** the administrative database health probe executed and cached a healthy result
- **WHEN** the probe is evaluated again within 10 seconds of the prior execution
- **THEN** the probe returns the cached healthy `Result` without opening a new Hibernate session or issuing a query

#### Scenario: Health check re-queries database after cache expiration
- **GIVEN** a cached probe result has exceeded the 10-second cache expiration window
- **WHEN** the probe is evaluated
- **THEN** the probe issues a fresh `SELECT 1` query to the database
- **AND** updates the cache with the new result

#### Scenario: Database query failure or timeout reports unhealthy
- **GIVEN** the PostgreSQL database is unreachable, the connection pool is exhausted, or the query exceeds the 1-second timeout
- **WHEN** the administrative database health probe is evaluated
- **THEN** the probe returns an unhealthy `Result` containing the error details
- **AND** the unhealthy result is cached for the 10-second cache window

### Requirement: Tenanted User Space StudioDAO
The system SHALL provide a tenanted `StudioDAO` interface extending `DAO<Studio>` in the **Data plane** operating on the user schema. The `StudioDAO` SHALL provide `findById(UUID tenantId)` and `getStudio(UUID tenantId)`. All implementations SHALL inject `@NjallUsers Provider<SessionFactory>`. Before executing queries against `njall_users.studios`, the implementation SHALL set the PostgreSQL local configuration `app.tenant_id` to the provided tenant UUID within the transaction context using `set_config('app.tenant_id', :tenantId, true)` to enforce Row-Level Security isolation.

#### Scenario: Retrieve studio via tenanted StudioDAO with RLS
- **GIVEN** a persisted studio in `njall_users.studios` with tenant UUID and name "Valiant Games"
- **WHEN** `StudioDAO.findById(tenantId)` is invoked with that tenant UUID
- **THEN** the DAO sets `app.tenant_id` to `tenantId` in the session
- **AND** the DAO returns an Optional containing the `Studio` record with matching name
- **AND** querying with a different tenant UUID returns empty due to Row-Level Security filtering

### Requirement: Default Studio Role DAO
The system SHALL provide a `StudioRoleDAO` interface extending `DAO<DefaultStudioRole>` in the persistence layer. In addition to standard `findById(UUID id)` and `list()`, `StudioRoleDAO` SHALL provide `findByName(String name)`, `create(String name)`, and `update(UUID id, String name)`. All implementations SHALL inject `@NjallAdmin Provider<SessionFactory>` and execute queries against `njall_users.default_studio_roles`. The concrete implementation SHALL be named `DefaultStudioRoleDAO`.

#### Scenario: Create and list default studio roles
- **GIVEN** an active Hibernate session via `@NjallAdmin Provider<SessionFactory>`
- **WHEN** `StudioRoleDAO.create("ADMIN")` is executed
- **THEN** a new default studio role is persisted with a generated UUIDv7
- **AND** `StudioRoleDAO.list()` returns the role in the list

#### Scenario: Update default studio role name
- **GIVEN** an existing default studio role with name "MEMBER"
- **WHEN** `StudioRoleDAO.update(roleId, "PLAYER")` is executed
- **THEN** the role record is updated with name "PLAYER"
- **AND** `StudioRoleDAO.findById(roleId)` reflects the updated name

### Requirement: Tenanted Link Data Access Object
The system SHALL provide an immutable `Link` domain record in the **Data plane** implementing `DatabaseObject` and representing external studio links. The system SHALL provide a tenanted `LinkDAO` non-sealed interface extending `DAO<Link>` with operations: `create(UUID tenantId, String linkType, String url, String mediaType, Optional<String> summary)`, `findById(UUID tenantId, UUID linkId)`, `patch(UUID tenantId, UUID linkId, Optional<String> linkType, Optional<String> url, Optional<String> mediaType, Optional<String> summary)`, and `softDelete(UUID tenantId, UUID linkId)`. All implementations SHALL inject `@NjallUsers Provider<SessionFactory>`. Before executing queries or mutations against `njall_users.entities` and `njall_users.links`, the implementation SHALL set the PostgreSQL local configuration `app.tenant_id` to the provided tenant UUID within the transaction context using `set_config('app.tenant_id', :tenantId, true)` to enforce Row-Level Security isolation. Soft deletion SHALL update `njall_users.entities.deleted_on` to `CURRENT_TIMESTAMP`. All read queries SHALL filter for `entities.deleted_on IS NULL`. Multi-tenant link listing across all tenants via `list()` SHALL be forbidden and throw `UnsupportedOperationException`.

#### Scenario: Persist and retrieve link via tenanted LinkDAO
- **GIVEN** an active Hibernate session via `@NjallUsers Provider<SessionFactory>` with valid tenant UUID `tenantId`
- **WHEN** `LinkDAO.create(tenantId, "website", "https://example.com", "text/html", Optional.of("Homepage"))` is invoked
- **THEN** an entity row and a link row are persisted with matching generated UUIDv7
- **AND** `LinkDAO.findById(tenantId, linkId)` returns an Optional containing the persisted `Link` record
- **AND** querying with a different tenant UUID returns empty due to Row-Level Security

#### Scenario: Patch link updates specified attributes and refreshes updated timestamp
- **GIVEN** a persisted active link for tenant `tenantId`
- **WHEN** `LinkDAO.patch(tenantId, linkId, Optional.empty(), Optional.of("https://new.example.com"), Optional.empty(), Optional.of("New Summary"))` is executed
- **THEN** the URL and summary are updated
- **AND** `updatedOn` timestamp is refreshed

#### Scenario: Soft delete sets deleted_on timestamp and hides link from subsequent lookups
- **GIVEN** a persisted active link for tenant `tenantId`
- **WHEN** `LinkDAO.softDelete(tenantId, linkId)` is executed
- **THEN** the method returns true
- **AND** `LinkDAO.findById(tenantId, linkId)` returns an empty Optional
- **AND** the database row in `njall_users.entities` has a non-null `deleted_on` timestamp

### Requirement: Direct Primary Key Session Lookups
The system SHALL execute single-entity lookups by primary key across all **DAO** implementations using Hibernate `Session.find` rather than HQL query execution (`createQuery`), utilizing first-level session cache and identifier resolution.

#### Scenario: Primary key lookup executes via session find
- **GIVEN** a valid primary key identifier
- **WHEN** a **DAO** executes `findById`
- **THEN** the entity is retrieved directly via `Session.find` using the entity class and identifier
- **AND** no HQL query string parsing occurs for the lookup

### Requirement: Safe Coordinate Value Parsing
The system SHALL parse GeoJSON coordinates into domain records safely without throwing uncaught runtime exceptions. When encountering invalid numeric formats, out-of-range floating point values, or blank strings, coordinate parsing SHALL return an empty `Optional` rather than throwing a `NumberFormatException`.

#### Scenario: Parse valid GeoJSON coordinate pair
- **GIVEN** a GeoJSON point string with valid decimal coordinates `[-122.4194, 37.7749]`
- **WHEN** coordinate parsing is performed
- **THEN** an `Optional` containing the parsed coordinate point with longitude `-122.4194` and latitude `37.7749` is returned

#### Scenario: Handle malformed coordinate numbers gracefully
- **GIVEN** a GeoJSON string containing non-numeric coordinate text or overflow exponents
- **WHEN** coordinate parsing is performed
- **THEN** an empty `Optional` is returned
- **AND** no `NumberFormatException` is thrown

### Requirement: Entity Column Immutability and Setter Restriction
Persistent entities in the persistence layer SHALL NOT expose mutator methods (setters) for any persistent field mapped to a database column marked with `updatable = false`. Any entity where all persistent fields are marked with `updatable = false` SHALL be annotated with Hibernate `@Immutable`.

#### Scenario: Non-updatable entity fields omit setters
- **GIVEN** a JPA entity mapped to persistent storage with identifier, tenant, or audit creation columns configured as `updatable = false`
- **WHEN** inspecting the public and package-private API of the entity class
- **THEN** zero mutator methods exist for those non-updatable fields

#### Scenario: Fully immutable entities declare Immutable annotation
- **GIVEN** an entity whose fields are all non-updatable after initial persistence
- **WHEN** inspecting the class-level annotations
- **THEN** the entity is annotated with `@org.hibernate.annotations.Immutable`

### Requirement: Static Query Definition Standardization
The system SHALL define SQL and HQL queries using named queries (`@NamedQuery`, `@NamedNativeQuery`) or static constants rather than dynamically constructed inline string literals inside method bodies, ensuring queries are centrally managed and validated.

#### Scenario: Named queries validated at startup
- **GIVEN** the Hibernate `SessionFactory` initialization sequence
- **WHEN** JPA entity metadata is loaded
- **THEN** all named HQL and native queries declared on the entity classes are validated for syntax and parameter bindings

### Requirement: Parameter Threshold Enforcement for Non-Records
The system SHALL enforce via static analysis that methods and constructors in non-record classes declare at most 8 parameters. Classes requiring more than 8 attributes during construction SHALL use builders, parameter objects, or split identifiers.

#### Scenario: Constructor with more than 8 parameters fails static check
- **GIVEN** a non-record class declaring a method or constructor with 9 or more parameters
- **WHEN** Checkstyle analysis executes
- **THEN** Checkstyle emits a build failure identifying the parameter count violation
