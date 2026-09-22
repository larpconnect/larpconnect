## ADDED Requirements

### Requirement: Architectural Enforcement Against Bare SessionFactory Injection
The system SHALL enforce via ArchUnit in the `:integration` module that no non-test class in package `com.larpconnect.njall..` injects a raw `org.hibernate.SessionFactory` into `@Inject` constructors, methods, fields, or `@Provides` methods. Any injected dependency on a Hibernate `SessionFactory` MUST be wrapped in a `Provider<SessionFactory>`.

#### Scenario: ArchUnit architecture suite verifies zero bare SessionFactory injections
- **GIVEN** the compiled classes across all production modules
- **WHEN** the ArchUnit test suite in `:integration` executes
- **THEN** zero classes inject a bare `SessionFactory` into an `@Inject` constructor, method, field, or `@Provides` method
- **AND** all existing session factory consumers inject `Provider<SessionFactory>`

## MODIFIED Requirements

### Requirement: Dual Role-Scoped Hibernate Session Factories
The system SHALL provide two isolated Hibernate `SessionFactory` instances managed via Guice: one qualified with `@NjallAdmin` connecting as database role `njall_admin`, and one qualified with `@NjallUsers` connecting as database role `njall_users`. Each session factory SHALL configure dedicated connection pooling with configurable minimum and maximum pool sizes and connection timeout durations. Database passwords SHALL be optional; if a password is not provided or is empty, the system SHALL omit the JDBC password property to support trust-authenticated connections. Consuming components SHALL NOT inject `SessionFactory` instances bare; any component requiring a session factory SHALL inject a `Provider<SessionFactory>` to ensure deferred resolution.

#### Scenario: Inject administrative session factory
- **GIVEN** the Guice dependency injection container is initialized
- **WHEN** a component requests a `Provider<SessionFactory>` annotated with `@NjallAdmin`
- **THEN** the container provides a provider that resolves a `SessionFactory` configured to execute queries with `njall_admin` role credentials

#### Scenario: Inject user session factory
- **GIVEN** the Guice dependency injection container is initialized
- **WHEN** a component requests a `Provider<SessionFactory>` annotated with `@NjallUsers`
- **THEN** the container provides a provider that resolves a `SessionFactory` configured to execute queries with `njall_users` role credentials

### Requirement: Read-Only Server and Contact Domain Models
The system SHALL provide immutable `Server` and `ServerContact` records representing records in `njall.servers` and `njall.server_contacts`. The `ServerDAO` SHALL be read-only, using a `@NjallAdmin Provider<SessionFactory>` to obtain sessions on-demand to query servers and associated contacts without exposing write, update, or delete operations.

#### Scenario: Retrieve server with associated contact records
- **GIVEN** the database contains a server record in `njall.servers` and associated contact records in `njall.server_contacts`
- **WHEN** `ServerDAO.list()` is invoked
- **THEN** the system returns an `ImmutableList<Server>` where each `Server` contains its associated contacts ordered by `ordering` ascending
- **AND** the returned records are immutable and do not allow state mutation
