## Why

The **Server** administrative runtime health probe at `/api/admin/v1/health` currently evaluates only the Pekko ActorSystem lifecycle via `PekkoHealthCheck`. If the PostgreSQL database or the `njall_admin` connection pool becomes unavailable or unresponsive, the health probe continues to return HTTP 200 OK. To ensure accurate operational monitoring and infrastructure readiness in **Njall**, an administrative database health check running an equivalent to `SELECT 1` against the `njall_admin` database role must be introduced, with probe results cached via Caffeine to prevent connection pool exhaustion and query storms during frequent health polling.

## What Changes

- Add `AdminDatabaseHealthCheck` in the **Data plane** (`com.larpconnect.njall.data.health`) implementing Dropwizard `HealthCheck`.
- Inject `@NjallAdmin Provider<SessionFactory>` to execute a native `SELECT 1` query against PostgreSQL with an explicit 1-second query timeout.
- Cache database ping probe results using an in-memory Caffeine `LoadingCache` with a 10-second expiration duration (`expireAfterWrite`) to limit query frequency to at most once every 10 seconds.
- Support deterministic testing by accepting an optional `Ticker` and cache `Duration` in a package-private constructor.
- Add `DataHealthModule` in `:data` (`com.larpconnect.njall.data.health`) to bind `AdminDatabaseHealthCheck` in `Scopes.SINGLETON` and register it into Guice `Multibinder<HealthCheck>`.
- Install `DataHealthModule` from `DataModule` in `:data`.
- Add `api(libs.caffeine)` dependency to the `:data` **Library module**.
- Update integration test harness (`HealthEndpointSteps`) to stub database session pinging and add Cucumber scenarios covering database health probe failure and result caching.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `admin-healthcheck`: Extends `/api/admin/v1/health` probe evaluation to reflect database connectivity and cached probe results alongside Pekko runtime status.
- `data-persistence`: Adds administrative database health check verification and Caffeine caching requirements for the `njall_admin` session factory.

## Impact

- **Affected Modules**: `:data` (persisted health probe and module wiring), `:integration` (cucumber steps and scenarios).
- **APIs**: `/api/admin/v1/health` now returns HTTP 500 when `njall_admin` database connectivity fails.
- **Dependencies**: `:data` adds `api(libs.caffeine)` (already pinned in catalog and constrained in `:parent`).
