## ADDED Requirements

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
