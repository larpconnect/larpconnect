## ADDED Requirements

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
