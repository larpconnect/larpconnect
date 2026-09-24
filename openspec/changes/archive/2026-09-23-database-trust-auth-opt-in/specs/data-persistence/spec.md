## MODIFIED Requirements

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
