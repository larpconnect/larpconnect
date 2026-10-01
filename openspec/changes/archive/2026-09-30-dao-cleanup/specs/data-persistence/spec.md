# data-persistence Delta Specification

## ADDED Requirements

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

## MODIFIED Requirements

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
