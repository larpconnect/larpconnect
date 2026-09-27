# studio-lookup-cache Specification

## Purpose

Provides a high-performance, non-blocking in-memory Caffeine cache of multi-tenant studio lookup metadata with pre-warming on server startup, periodic background synchronization, and on-demand refresh on administrative mutations.

## Requirements

### Requirement: In-Memory Studio Lookup Caching
The system SHALL provide a `StudioLookupCache` component in the **Data plane** providing non-blocking in-memory lookups for active and soft-deleted studios. The cache SHALL support retrieval by public studio **ID** (`findById(UUID studioId)`), by unique studio **Alias** (`findByAlias(String alias)`), by mixed identifier (`findByIdOrAlias(String idOrAlias)`), and listing all active studios (`listActive()`). The system SHALL pre-populate the cache during service startup before any incoming network traffic is accepted by querying all studios from `StudioLookupDAO.list(DeletionFilter.INCLUDE_DELETED)`. If the initial database query fails during startup, the cache service SHALL throw an exception to halt server initialization.

#### Scenario: Successfully retrieve studio by alias from cache
- **GIVEN** a studio lookup cache populated with an active studio having alias "valiant" and studio ID "11111111-1111-1111-1111-111111111111"
- **WHEN** `findByAlias("valiant")` or `findByIdOrAlias("valiant")` is invoked
- **THEN** the cache returns an Optional containing the matching `StudioLookup` record in-memory without executing a database query

#### Scenario: Successfully retrieve studio by studio UUID from cache
- **GIVEN** a studio lookup cache populated with an active studio having studio ID "11111111-1111-1111-1111-111111111111"
- **WHEN** `findById(studioId)` or `findByIdOrAlias(studioId.toString())` is invoked
- **THEN** the cache returns an Optional containing the matching `StudioLookup` record in-memory without executing a database query

#### Scenario: Lookup nonexistent studio returns empty Optional
- **GIVEN** a populated studio lookup cache
- **WHEN** `findByAlias("nonexistent")` or `findById(nonexistentUuid)` is invoked
- **THEN** the cache returns an empty Optional

#### Scenario: Startup fails when initial cache pre-population fails
- **GIVEN** the database is unreachable or query execution throws an exception during startup
- **WHEN** `DefaultStudioLookupCacheService.startUp()` executes
- **THEN** an exception is thrown and the service transitions to FAILED state

### Requirement: Periodic Background Studio Lookup Cache Refresh
The system SHALL run `DefaultStudioLookupCacheService` as a Guava `AbstractScheduledService` that periodically reloads the entirety of the studio lookup cache from the database. The refresh interval SHALL be configurable via HOCON `larpconnect.data.cache.studio-lookup.refresh-interval` defaulting to 5 minutes. Each periodic iteration SHALL execute `StudioLookupDAO.list(DeletionFilter.INCLUDE_DELETED)` and atomically swap the cached index so that stale, updated, or newly created studio entries are reflected without exposing partial cache states or evicting entries during queries. If a periodic refresh attempt encounters a database failure, the system SHALL log an error and retain the existing in-memory cache contents intact.

#### Scenario: Periodic refresh updates modified and deleted studios
- **GIVEN** a running cache service with an initial snapshot of studios
- **WHEN** a scheduled refresh iteration runs after studios have been added or updated in the database
- **THEN** the cache executes `list(INCLUDE_DELETED)` and atomically updates the in-memory lookup index
- **AND** subsequent lookups immediately return the refreshed data

#### Scenario: Periodic refresh tolerates transient database failure
- **GIVEN** a running cache service with populated studio entries
- **WHEN** a scheduled refresh iteration encounters a database timeout or connection error
- **THEN** the error is logged without throwing an uncaught exception
- **AND** the existing in-memory cache entries remain available for lookups

### Requirement: On-Demand Cache Invalidation and Refresh
The system SHALL expose a `refresh()` method on `StudioLookupCache` allowing callers to trigger an immediate, synchronous reload of the in-memory cache from `StudioLookupDAO`. Administrative actors executing studio provisioning or lifecycle mutations SHALL invoke `refresh()` after database persistence succeeds, ensuring immediate single-node consistency for subsequent lookup operations.

#### Scenario: Administrative studio creation triggers immediate cache refresh
- **GIVEN** an administrative actor successfully creates a new studio via `StudioLookupDAO.create`
- **WHEN** `refresh()` is invoked on the `StudioLookupCache`
- **THEN** the cache synchronously queries `StudioLookupDAO` and updates its in-memory index
- **AND** the newly created studio is immediately queryable via `findByAlias` and `findById`
