## ADDED Requirements

### Requirement: Generator-Scoped Tenanted Collection Data Access Object
The system SHALL provide a tenanted `CollectionDAO` in the **Data plane** whose read and mutation methods verify that `{actor-id}` is an active (`deleted_on IS NULL`) `'Actor'` **Entity** in `tenantId` matching `collection.generator()`, returning empty or `false` otherwise. On `create`, `CollectionDAO` SHALL validate active `'Actor'` provenance via `ActorGeneratorValidator` and verify `ownerId` is an active **Entity** in `tenantId`, throwing `IllegalArgumentException` otherwise.

#### Scenario: Retrieve collection succeeds when actor matches collection generator
- **GIVEN** an active **Studio** **Tenant** `tenantId`, an active **Actor** `actorA`, and an active **Collections** record `collectionId` created with `generator = actorA`
- **WHEN** `CollectionDAO` queries `collectionId` scoped to `(tenantId, actorA)`
- **THEN** the **DAO** returns an `Optional` containing the `Collection` record with `generator()` equal to `actorA`

#### Scenario: Retrieve or mutate collection returns empty or false when actor mismatches generator
- **GIVEN** an active **Studio** **Tenant** `tenantId`, two active **Actor** records `actorA` and `actorB`, and an active **Collections** record `collectionId` created with `generator = actorA`
- **WHEN** `CollectionDAO` queries, patches, soft-deletes, or mutates items on `collectionId` scoped to `(tenantId, actorB)`
- **THEN** the **DAO** returns empty or `false` without mutating `collectionId`

### Requirement: Universal Entity Actor Generator Provenance and Validation
The system SHALL map immutable `generator` (`@Column(name = "generator", nullable = false, updatable = false)`) on `EntityBaseEntity` and expose `UUID generator()` across all Common Table Inheritance domain records. Tenanted **DAO** implementations SHALL verify via `ActorGeneratorValidator` that `generator` is an active `'Actor'` **Entity** in `tenantId` (or self `id` for **Actor** creation), throwing `IllegalArgumentException` otherwise.

#### Scenario: Persist entity with valid active Actor generator
- **GIVEN** an active studio **Tenant** `tenantId` and an active **Actor** `actorId` in `njall_users.entities` with `entity_type = 'Actor'`
- **WHEN** a tenanted **DAO** creates a new **Entity** with `generator = actorId`
- **THEN** the **DAO** verifies `actorId` is an active `'Actor'` in `tenantId`
- **AND** persists `njall_users.entities.generator = actorId`
- **AND** returns a domain record whose `generator()` equals `actorId`

#### Scenario: Reject entity creation when generator is not an Actor subtype
- **GIVEN** an active studio **Tenant** `tenantId` and an active **Individual** `individualId` with `entity_type = 'Individual'`
- **WHEN** a tenanted **DAO** attempts to create a new **Entity** with `generator = individualId`
- **THEN** the **DAO** throws `IllegalArgumentException`
- **AND** no new **Entity** row is persisted

#### Scenario: Reject entity creation when generator Actor is soft-deleted or missing
- **GIVEN** an active studio **Tenant** `tenantId` and a soft-deleted **Actor** `deletedActorId` (`deleted_on IS NOT NULL`)
- **WHEN** a tenanted **DAO** attempts to create a new **Entity** with `generator = deletedActorId`
- **THEN** the **DAO** throws `IllegalArgumentException`
- **AND** no new **Entity** row is persisted

#### Scenario: Default self-generation and collection provenance on Actor creation
- **GIVEN** an active studio **Tenant** `tenantId`
- **WHEN** `AgentDAO.create(tenantId, Optional.empty(), "valkyrie_lead", Optional.of("Lead"))` is invoked
- **THEN** the persisted **Actor** has `generator().equals(id())`
- **AND** the provisioned `inbox` and `outbox` **Collections** each have `entities.generator = actor.id` and `collections.owner_id = actor.id`

## MODIFIED Requirements

### Requirement: Tenanted Link Data Access Object
The system SHALL provide an immutable `Link` domain record in the **Data plane** implementing `DatabaseObject` and representing external studio links, including the generating **Actor** `UUID generator()`. The system SHALL provide a tenanted `LinkDAO` non-sealed interface extending `DAO<Link>` with operations: `create(UUID tenantId, UUID generator, String linkType, String url, String mediaType, Optional<String> summary)`, `findById(UUID tenantId, UUID linkId)`, `patch(UUID tenantId, UUID linkId, Optional<String> linkType, Optional<String> url, Optional<String> mediaType, Optional<String> summary)`, and `softDelete(UUID tenantId, UUID linkId)`. All implementations SHALL inject `@NjallUsers Provider<SessionFactory>`. Before executing queries or mutations against `njall_users.entities` and `njall_users.links`, the implementation SHALL set the PostgreSQL local configuration `app.tenant_id` to the provided tenant UUID within the transaction context using `set_config('app.tenant_id', :tenantId, true)` to enforce Row-Level Security isolation. Before inserting a new link, `create` SHALL verify that `generator` references an active `'Actor'` **Entity** within `tenantId`. Soft deletion SHALL update `njall_users.entities.deleted_on` to `CURRENT_TIMESTAMP`. All read queries SHALL filter for `entities.deleted_on IS NULL`. Multi-tenant link listing across all tenants via `list()` SHALL be forbidden and throw `UnsupportedOperationException`.

#### Scenario: Persist and retrieve link via tenanted LinkDAO
- **GIVEN** an active Hibernate session via `@NjallUsers Provider<SessionFactory>` with valid tenant UUID `tenantId` and an active **Actor** `actorId`
- **WHEN** `LinkDAO.create(tenantId, actorId, "website", "https://example.com", "text/html", Optional.of("Homepage"))` is invoked
- **THEN** an entity row with `generator = actorId` and a link row are persisted with matching generated UUIDv7
- **AND** `LinkDAO.findById(tenantId, linkId)` returns an Optional containing the persisted `Link` record with `generator()` equal to `actorId`
- **AND** querying with a different tenant UUID returns empty due to Row-Level Security

#### Scenario: Patch link updates specified attributes and refreshes updated timestamp
- **GIVEN** a persisted active link for tenant `tenantId`
- **WHEN** `LinkDAO.patch(tenantId, linkId, Optional.empty(), Optional.of("https://new.example.com"), Optional.empty(), Optional.of("New Summary"))` is executed
- **THEN** the URL and summary are updated
- **AND** `generator` remains unchanged
- **AND** `updatedOn` timestamp is refreshed

#### Scenario: Soft delete sets deleted_on timestamp and hides link from subsequent lookups
- **GIVEN** a persisted active link for tenant `tenantId`
- **WHEN** `LinkDAO.softDelete(tenantId, linkId)` is executed
- **THEN** the method returns true
- **AND** `LinkDAO.findById(tenantId, linkId)` returns an empty Optional
- **AND** the database row in `njall_users.entities` has a non-null `deleted_on` timestamp

## REMOVED Requirements

### Requirement: Universal Entity Actor Owner Provenance and Validation
**Reason**: Column `njall_users.entities.owner` and domain accessor `UUID owner()` collided with semantic collection ownership (`njall_users.collections.owner_id` / `UUID ownerId()`).

**Migration**: Replaced by `Universal Entity Actor Generator Provenance and Validation` mapping `njall_users.entities.generator` and `UUID generator()`.
