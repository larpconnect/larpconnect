## ADDED Requirements

### Requirement: Universal Entity Actor Owner Provenance and Validation
The system SHALL map `owner` (`UUID`, `nullable = false`, `updatable = false`) on `EntityBaseEntity` and expose `UUID owner()` on all **CTI** domain records. Before persisting a new **Entity**, tenanted **DAO** implementations SHALL verify within the `@NjallUsers` transaction that `owner` references an active (`deleted_on IS NULL`) `'Actor'` **Entity** in the same `tenant_id` (or `owner.equals(id)` for a self-owned **Actor**), throwing `IllegalArgumentException` otherwise.

#### Scenario: Persist entity with valid active Actor owner
- **GIVEN** an active studio **Tenant** `tenantId` and an active **Actor** `actorId` in `njall_users.entities` with `entity_type = 'Actor'`
- **WHEN** a tenanted **DAO** creates a new **Entity** with `owner = actorId`
- **THEN** the **DAO** verifies `actorId` is an active `'Actor'` in `tenantId`
- **AND** persists `njall_users.entities.owner = actorId`
- **AND** returns a domain record whose `owner()` equals `actorId`

#### Scenario: Reject entity creation when owner is not an Actor subtype
- **GIVEN** an active studio **Tenant** `tenantId` and an active **Individual** `individualId` with `entity_type = 'Individual'`
- **WHEN** a tenanted **DAO** attempts to create a new **Entity** with `owner = individualId`
- **THEN** the **DAO** throws `IllegalArgumentException`
- **AND** no new **Entity** row is persisted

#### Scenario: Reject entity creation when owner Actor is soft-deleted or missing
- **GIVEN** an active studio **Tenant** `tenantId` and a soft-deleted **Actor** `deletedActorId` (`deleted_on IS NOT NULL`)
- **WHEN** a tenanted **DAO** attempts to create a new **Entity** with `owner = deletedActorId`
- **THEN** the **DAO** throws `IllegalArgumentException`
- **AND** no new **Entity** row is persisted

#### Scenario: Default self-ownership and collection provenance on Actor creation
- **GIVEN** an active studio **Tenant** `tenantId`
- **WHEN** `AgentDAO.create(tenantId, Optional.empty(), "valkyrie_lead", Optional.of("Lead"))` is invoked
- **THEN** the persisted **Actor** has `owner().equals(id())`
- **AND** the provisioned `inbox` and `outbox` **Collections** each have `entities.owner = actor.id` and `collections.owner_id = actor.id`

## MODIFIED Requirements

### Requirement: Tenanted Link Data Access Object
The system SHALL provide an immutable `Link` domain record in the **Data plane** implementing `DatabaseObject` and representing external studio links, including the generating **Actor** `UUID owner()`. The system SHALL provide a tenanted `LinkDAO` non-sealed interface extending `DAO<Link>` with operations: `create(UUID tenantId, UUID owner, String linkType, String url, String mediaType, Optional<String> summary)`, `findById(UUID tenantId, UUID linkId)`, `patch(UUID tenantId, UUID linkId, Optional<String> linkType, Optional<String> url, Optional<String> mediaType, Optional<String> summary)`, and `softDelete(UUID tenantId, UUID linkId)`. All implementations SHALL inject `@NjallUsers Provider<SessionFactory>`. Before executing queries or mutations against `njall_users.entities` and `njall_users.links`, the implementation SHALL set the PostgreSQL local configuration `app.tenant_id` to the provided tenant UUID within the transaction context using `set_config('app.tenant_id', :tenantId, true)` to enforce Row-Level Security isolation. Before inserting a new link, `create` SHALL verify that `owner` references an active `'Actor'` **Entity** within `tenantId`. Soft deletion SHALL update `njall_users.entities.deleted_on` to `CURRENT_TIMESTAMP`. All read queries SHALL filter for `entities.deleted_on IS NULL`. Multi-tenant link listing across all tenants via `list()` SHALL be forbidden and throw `UnsupportedOperationException`.

#### Scenario: Persist and retrieve link via tenanted LinkDAO
- **GIVEN** an active Hibernate session via `@NjallUsers Provider<SessionFactory>` with valid tenant UUID `tenantId` and an active **Actor** `actorId`
- **WHEN** `LinkDAO.create(tenantId, actorId, "website", "https://example.com", "text/html", Optional.of("Homepage"))` is invoked
- **THEN** an entity row with `owner = actorId` and a link row are persisted with matching generated UUIDv7
- **AND** `LinkDAO.findById(tenantId, linkId)` returns an Optional containing the persisted `Link` record with `owner()` equal to `actorId`
- **AND** querying with a different tenant UUID returns empty due to Row-Level Security

#### Scenario: Patch link updates specified attributes and refreshes updated timestamp
- **GIVEN** a persisted active link for tenant `tenantId`
- **WHEN** `LinkDAO.patch(tenantId, linkId, Optional.empty(), Optional.of("https://new.example.com"), Optional.empty(), Optional.of("New Summary"))` is executed
- **THEN** the URL and summary are updated
- **AND** `owner` remains unchanged
- **AND** `updatedOn` timestamp is refreshed

#### Scenario: Soft delete sets deleted_on timestamp and hides link from subsequent lookups
- **GIVEN** a persisted active link for tenant `tenantId`
- **WHEN** `LinkDAO.softDelete(tenantId, linkId)` is executed
- **THEN** the method returns true
- **AND** `LinkDAO.findById(tenantId, linkId)` returns an empty Optional
- **AND** the database row in `njall_users.entities` has a non-null `deleted_on` timestamp
