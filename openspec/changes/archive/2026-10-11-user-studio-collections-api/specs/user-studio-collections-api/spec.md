## ADDED Requirements

### Requirement: Tenanted Studio Collection Creation Endpoint
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/collections` allowing clients to create a **Collections** **Entity** under a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public studio UUID or an **Alias** string, resolving in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL validate the request payload (`name` required non-blank, optional `summary`, and `ownerId` required UUID). The `ownerId` MUST reference an existing, non-deleted **Entity** belonging to the same studio tenant; any referential constraint violation SHALL result in HTTP 400 Bad Request. If another active collection already exists for the same owner with the same name, the system SHALL return HTTP 409 Conflict. If validation succeeds, the system SHALL persist the record in `njall_users.entities` (with `entity_type = 'OrderedCollection'`) and `njall_users.collections` using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created containing the public collection `id`, `type = "OrderedCollection"`, `name`, `summary`, `ownerId`, `totalItems = 0`, `orderedItems = []`, `createdOn`, and `updatedOn`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successfully create collection with all fields
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active individual entity exists with UUID "019273c5-9271-7000-8000-000000000001" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections` with body:
  ```json
  {
    "name": "Featured Cast",
    "summary": "Primary cast members for season 1",
    "ownerId": "019273c5-9271-7000-8000-000000000001"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `type` equal to "OrderedCollection", `name` equal to "Featured Cast", `summary` equal to "Primary cast members for season 1", `ownerId` equal to "019273c5-9271-7000-8000-000000000001", `totalItems` equal to 0, `orderedItems` as an empty list, and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

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
- **THEN** the system returns HTTP 400 Bad Request due to foreign key violation

### Requirement: Tenanted Studio Collection Retrieval Endpoint with Compressed and Expanded Forms
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/collections/{collection-id}` allowing clients to retrieve details of a specific **Collections** **Entity**. The system SHALL resolve `{studio-id}` in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, or if `{collection-id}` is not a valid UUID string, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL query `CollectionDAO` under `app.tenant_id = :tenantId`. If the collection exists and is active (`deleted_on IS NULL`), the system SHALL format the response according to the query parameter `?form={compressed|expanded}`. If `form` is omitted or equal to "compressed", `orderedItems` SHALL contain an array of UUID strings corresponding to the referenced entities in sequence order. If `form=expanded`, `orderedItems` SHALL contain an array of full JSON objects representing each active referenced entity in sequence order. Any referenced entity that has been soft-deleted (`deleted_on IS NOT NULL`) SHALL be excluded from `orderedItems` and decremented from `totalItems`. If the collection does not exist, belongs to another **Tenant**, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Retrieve active collection in default compressed form
- **GIVEN** an active studio "valiant" has an active collection with UUID `collectionId` containing 2 items referencing individuals `id1` and `id2`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/collections/{collectionId}`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** `totalItems` is 2
- **AND** `orderedItems` contains the array of UUID strings `[id1, id2]`

#### Scenario: Retrieve active collection in expanded form
- **GIVEN** an active studio "valiant" has an active collection with UUID `collectionId` containing 2 items referencing individuals `id1` and `id2`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/collections/{collectionId}?form=expanded`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** `totalItems` is 2
- **AND** `orderedItems` contains an array of 2 full JSON objects representing `id1` and `id2` with their respective `id`, `name`, and `type` fields

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

### Requirement: Deliberate Omission of Collection Listing Endpoint
The system SHALL NOT expose a collection listing endpoint on `/api/studios/{studio-id}/v1/collections`. Any HTTP GET request directed to `/api/studios/{studio-id}/v1/collections` SHALL immediately complete with HTTP 404 Not Found and a descriptive message stating that listing collections is not supported.

#### Scenario: Attempting to list collections returns 404
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/collections`
- **THEN** the system returns HTTP 404 Not Found with error message "Listing collections is not supported"

### Requirement: Tenanted Studio Collection Partial Update Endpoint
The system SHALL expose an HTTP PATCH endpoint at `/api/studios/{studio-id}/v1/collections/{collection-id}` allowing clients to partially update a **Collections** **Entity** conforming to Google AIP-134. Updatable fields SHALL include `name` and `summary`. The request MAY supply an optional `update_mask` query parameter. If `name` is updated and matches another existing collection for the same owner, the system SHALL return HTTP 409 Conflict. If the collection exists and update succeeds, the system SHALL update `updated_on` and return HTTP 200 OK with the updated collection details in default compressed form. If the collection does not exist or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully update collection name and summary
- **GIVEN** an active collection exists with UUID `collectionId` and name "Old Name"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/collections/{collectionId}` with body:
  ```json
  {
    "name": "Updated Name",
    "summary": "Updated summary description"
  }
  ```
- **THEN** the system returns HTTP 200 OK with `name` equal to "Updated Name" and `summary` equal to "Updated summary description"

### Requirement: Tenanted Studio Collection Soft-Delete Endpoint
The system SHALL expose an HTTP DELETE endpoint at `/api/studios/{studio-id}/v1/collections/{collection-id}` allowing clients to soft-delete a collection resource. Soft deletion SHALL update `njall_users.entities.deleted_on` to the current timestamp. The endpoint SHALL return HTTP 204 No Content. If the collection does not exist, belongs to another studio tenant, or is already soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully soft-delete active collection
- **GIVEN** an active collection exists with UUID `collectionId`
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/collections/{collectionId}`
- **THEN** the system returns HTTP 204 No Content
- **AND** a subsequent GET request for `{collectionId}` returns HTTP 404 Not Found

### Requirement: Tenanted Studio Collection Add Item Custom Method
The system SHALL expose an HTTP POST custom method endpoint at `/api/studios/{studio-id}/v1/collections/{collection-id}:addItem` (AIP-136) allowing clients to insert an entity into a **Collections** resource. The request body SHALL contain required `refersToId` (UUID) and optional placement specifiers `before` (UUID) and `index` (integer). The referenced entity MUST exist, belong to the same studio tenant, and not be soft-deleted. If both `before` and `index` are omitted, the system SHALL prepend the item to the head of the collection (newest first, ordinal 0), shifting existing items. If `before` is supplied, the system SHALL place the item immediately ahead of the item referencing that `before` entity ID. If `index` is supplied, the system SHALL insert the item at that zero-based index. If the item is already present in the collection, the operation SHALL be idempotent and return HTTP 204 No Content without creating duplicates. Upon successful insertion, the endpoint SHALL return HTTP 204 No Content. If the collection or referenced entity does not exist, the system SHALL return HTTP 404 Not Found.

#### Scenario: Prepend item to collection by default
- **GIVEN** an active collection `collectionId` has existing item `itemA` at index 0
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections/{collectionId}:addItem` with body:
  ```json
  {
    "refersToId": "019273c5-9271-7000-8000-000000000002"
  }
  ```
- **THEN** the system returns HTTP 204 No Content
- **AND** a subsequent GET request shows `refersToId` at index 0 and `itemA` at index 1

#### Scenario: Insert item before specified target item
- **GIVEN** an active collection has items `[itemA, itemB]`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections/{collectionId}:addItem` with body:
  ```json
  {
    "refersToId": "019273c5-9271-7000-8000-000000000003",
    "before": "itemB"
  }
  ```
- **THEN** the system returns HTTP 204 No Content
- **AND** a subsequent GET request shows items in sequence `[itemA, refersToId, itemB]`

#### Scenario: Insert item at specific integer index
- **GIVEN** an active collection has items `[itemA, itemB]`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections/{collectionId}:addItem` with body:
  ```json
  {
    "refersToId": "019273c5-9271-7000-8000-000000000003",
    "index": 1
  }
  ```
- **THEN** the system returns HTTP 204 No Content
- **AND** a subsequent GET request shows items in sequence `[itemA, refersToId, itemB]`

#### Scenario: Idempotent add item already present
- **GIVEN** an active collection already contains item `itemA`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections/{collectionId}:addItem` with body:
  ```json
  {
    "refersToId": "itemA"
  }
  ```
- **THEN** the system returns HTTP 204 No Content without creating duplicate items

### Requirement: Tenanted Studio Collection Remove Item Custom Method
The system SHALL expose an HTTP POST custom method endpoint at `/api/studios/{studio-id}/v1/collections/{collection-id}:removeItem` (AIP-136) allowing clients to remove an entity from a **Collections** resource. The request body SHALL contain required `refersToId` (UUID). The system SHALL delete the corresponding junction record from `njall_users.collection_items` and compact remaining ordinals. The operation SHALL be idempotent; if the entity is not present in the collection, the endpoint SHALL return HTTP 204 No Content. If the collection itself does not exist or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully remove item from collection
- **GIVEN** an active collection contains items `[itemA, itemB, itemC]`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections/{collectionId}:removeItem` with body:
  ```json
  {
    "refersToId": "itemB"
  }
  ```
- **THEN** the system returns HTTP 204 No Content
- **AND** a subsequent GET request shows remaining items `[itemA, itemC]`

#### Scenario: Idempotently remove item not present
- **GIVEN** an active collection contains item `itemA` but not `itemB`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/collections/{collectionId}:removeItem` with body:
  ```json
  {
    "refersToId": "itemB"
  }
  ```
- **THEN** the system returns HTTP 204 No Content
