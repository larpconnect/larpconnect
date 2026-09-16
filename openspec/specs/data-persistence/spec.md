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
The system SHALL provide two isolated Hibernate `SessionFactory` instances managed via Guice: one qualified with `@NjallAdmin` connecting as database role `njall_admin`, and one qualified with `@NjallUsers` connecting as database role `njall_users`. Each session factory SHALL configure dedicated connection pooling with configurable minimum and maximum pool sizes and connection timeout durations.

#### Scenario: Inject administrative session factory
- **GIVEN** the Guice dependency injection container is initialized
- **WHEN** a component requests a `SessionFactory` annotated with `@NjallAdmin`
- **THEN** the container provides a `SessionFactory` configured to execute queries with `njall_admin` role credentials

#### Scenario: Inject user session factory
- **GIVEN** the Guice dependency injection container is initialized
- **WHEN** a component requests a `SessionFactory` annotated with `@NjallUsers`
- **THEN** the container provides a `SessionFactory` configured to execute queries with `njall_users` role credentials

### Requirement: Read-Only Server and Contact Domain Models
The system SHALL provide immutable `Server` and `ServerContact` records representing records in `njall.servers` and `njall.server_contacts`. The `ServerDAO` SHALL be read-only, using the `@NjallAdmin` session factory to query servers and associated contacts without exposing write, update, or delete operations.

#### Scenario: Retrieve server with associated contact records
- **GIVEN** the database contains a server record in `njall.servers` and associated contact records in `njall.server_contacts`
- **WHEN** `ServerDAO.list()` is invoked
- **THEN** the system returns an `ImmutableList<Server>` where each `Server` contains its associated contacts ordered by `ordering` ascending
- **AND** the returned records are immutable and do not allow state mutation
