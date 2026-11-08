## MODIFIED Requirements

### Requirement: Tenanted Studio Collection Creation Endpoint
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/collections` allowing clients to create a **Collections** **Entity** under a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public studio UUID or an **Alias** string, resolving in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL validate the request payload (`name` required non-blank, optional `summary`, `ownerId` required UUID for semantic **Entity** attachment, and optional `generator` accepting either a bare **Actor** UUID or a canonical studio **Actor** URI `/api/studios/{studio-id}/v1/actors/{actor-id}` matching `{studio-id}`). The `ownerId` MUST reference an existing, non-deleted **Entity** belonging to the same studio tenant. When `generator` is omitted or null, the system SHALL infer `generator = ownerId`. The resolved `generator` MUST reference an existing, non-deleted **Actor** **Entity** (`entity_type = 'Actor'`) belonging to the same studio **Tenant**; if `ownerId` is not an **Actor** and `generator` is omitted, or if an explicit `generator` is invalid or does not reference an active **Actor** in the same **Tenant**, the system SHALL return HTTP 400 Bad Request. If another active collection already exists for the same semantic `ownerId` with the same `name`, the system SHALL return HTTP 409 Conflict. If validation succeeds, the system SHALL persist the record in `njall_users.entities` (with `owner` set to the resolved `generator` **Actor** UUID and `entity_type = 'OrderedCollection'`) and `njall_users.collections` (with `owner_id` set to `ownerId`) using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created containing the public collection `id`, `type = "OrderedCollection"`, `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{owner-id}"`, `name`, `summary`, `ownerId`, `totalItems = 0`, `orderedItems = []`, `createdOn`, and `updatedOn`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successfully create collection with all fields
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active actor entity exists with UUID "019273c5-9271-7000-8000-000000000001" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections` with body:
  ```json
  {
    "name": "Featured Cast",
    "summary": "Primary cast members for season 1",
    "ownerId": "019273c5-9271-7000-8000-000000000001"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `type` equal to "OrderedCollection", `generator` equal to "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001", `name` equal to "Featured Cast", `summary` equal to "Primary cast members for season 1", `ownerId` equal to "019273c5-9271-7000-8000-000000000001", `totalItems` equal to 0, `orderedItems` as an empty list, and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Successfully create collection attached to a non-Actor entity with explicit Actor generator
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active individual entity exists with UUID "019273c5-9271-7000-8000-000000000001" and an active actor exists with UUID "019273c5-9271-7000-8000-000000000002" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections` with body:
  ```json
  {
    "name": "Character Gallery",
    "ownerId": "019273c5-9271-7000-8000-000000000001",
    "generator": "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000002"
  }
  ```
- **THEN** the system returns HTTP 201 Created
- **AND** `ownerId` is "019273c5-9271-7000-8000-000000000001"
- **AND** `generator` is "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000002"

#### Scenario: Creating collection attached to non-Actor without generator returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active individual entity (non-Actor) exists with UUID "019273c5-9271-7000-8000-000000000001" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections` omitting `generator` with body:
  ```json
  {
    "name": "Character Gallery",
    "ownerId": "019273c5-9271-7000-8000-000000000001"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating collection with duplicate name for same owner returns 409
- **GIVEN** an active collection named "Featured Cast" already exists for owner "019273c5-9271-7000-8000-000000000001" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections` with body:
  ```json
  {
    "name": "Featured Cast",
    "ownerId": "019273c5-9271-7000-8000-000000000001"
  }
  ```
- **THEN** the system returns HTTP 409 Conflict with an AIP-193 structured error response

#### Scenario: Creating collection with blank name returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections` with body:
  ```json
  {
    "name": "   ",
    "ownerId": "019273c5-9271-7000-8000-000000000001"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating collection with nonexistent owner returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections` with body:
  ```json
  {
    "name": "Unanchored Collection",
    "ownerId": "019273c5-9271-7000-8000-000000000999"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request due to foreign key or owner validation failure

### Requirement: Tenanted Studio Collection Retrieval Endpoint with Compressed and Expanded Forms
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/collections/{collection-id}` allowing clients to retrieve details of a specific **Collections** **Entity**. The system SHALL resolve `{studio-id}` in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, or if `{collection-id}` is not a valid UUID string, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL query `CollectionDAO` under `app.tenant_id = :tenantId`. If the collection exists and is active (`deleted_on IS NULL`), the system SHALL format the response according to the query parameter `?form={compressed|expanded}`, always including `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{owner-id}"` alongside `ownerId`. If `form` is omitted or equal to "compressed", `orderedItems` SHALL contain an array of UUID strings corresponding to the referenced entities in sequence order. If `form=expanded`, `orderedItems` SHALL contain an array of full JSON objects representing each active referenced entity in sequence order, where each hydrated item includes its own `generator` field formatted as the **Actor** URI string `"/api/studios/{studio-id}/v1/actors/{item-owner-id}"` (never an embedded **Actor** object). Any referenced entity that has been soft-deleted (`deleted_on IS NOT NULL`) SHALL be excluded from `orderedItems` and decremented from `totalItems`. If the collection does not exist, belongs to another **Tenant**, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Retrieve active collection in default compressed form
- **GIVEN** an active studio "valiant" has an active collection with UUID `collectionId` containing 2 items referencing individuals `id1` and `id2`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/collections/{collectionId}`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** response body contains `generator` matching `/api/studios/valiant/v1/actors/{ownerId}`
- **AND** `totalItems` is 2
- **AND** `orderedItems` contains the array of UUID strings `[id1, id2]`

#### Scenario: Retrieve active collection in expanded form
- **GIVEN** an active studio "valiant" has an active collection with UUID `collectionId` containing 2 items referencing individuals `id1` and `id2` generated by actor `actorId`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/collections/{collectionId}?form=expanded`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** `totalItems` is 2
- **AND** `orderedItems` contains an array of 2 full JSON objects representing `id1` and `id2` with their respective `id`, `name`, `type`, and `generator` equal to `"/api/studios/valiant/v1/actors/{actorId}"`

#### Scenario: Soft-deleted referenced entity is excluded from ordered items
- **GIVEN** an active collection contains item `id1` and item `id2`
- **AND** entity `id2` is soft-deleted
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/collections/{collectionId}`
- **THEN** the system returns HTTP 200 OK
- **AND** `totalItems` is 1
- **AND** `orderedItems` contains only `[id1]`

#### Scenario: Retrieving nonexistent collection returns 404
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/collections/019273c5-9271-7000-8000-000000000999`
- **THEN** the system returns HTTP 404 Not Found
