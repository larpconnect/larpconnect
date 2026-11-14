# user-studio-collections-api Specification

## Purpose

Tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/collections` for creating, retrieving (compressed/expanded), partially updating, soft-deleting, and mutating ordered items within studio collections extending entities via Class Table Inheritance (CTI) with PostgreSQL Row-Level Security (RLS) isolation and deliberate omission of collection listing.

## Requirements

### Requirement: Actor-Parented Studio Collection Creation Endpoint
The system SHALL expose `POST /api/studios/{studio-id}/v1/actors/{actor-id}/collections` to create a **Collections** **Entity** with `entities.generator = {actor-id}`, returning 404 if `{studio-id}` or active `'Actor'` `{actor-id}` is missing. `CreateCollectionRequest` SHALL omit `generator`, require non-blank `name`, and accept optional `summary` and optional `ownerId` (defaulting to `{actor-id}`, returning 400 if an explicit `ownerId` is inactive, and 409 on duplicate `(owner_id, name)`).

#### Scenario: Successfully create collection defaulting ownerId to path actor
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active actor entity exists with UUID "019273c5-9271-7000-8000-000000000001" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001/collections` with body:
  ```json
  {
    "name": "Featured Cast",
    "summary": "Primary cast members for season 1"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `type` equal to "OrderedCollection", `generator` equal to "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001", `name` equal to "Featured Cast", `summary` equal to "Primary cast members for season 1", `ownerId` equal to "019273c5-9271-7000-8000-000000000001", `totalItems` equal to 0, `orderedItems` as an empty list, and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Successfully create collection attached to a non-Actor entity under generating Actor path
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active individual entity exists with UUID "019273c5-9271-7000-8000-000000000001" and an active actor exists with UUID "019273c5-9271-7000-8000-000000000002" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000002/collections` with body:
  ```json
  {
    "name": "Character Gallery",
    "ownerId": "019273c5-9271-7000-8000-000000000001"
  }
  ```
- **THEN** the system returns HTTP 201 Created
- **AND** `ownerId` is "019273c5-9271-7000-8000-000000000001"
- **AND** `generator` is "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000002"

#### Scenario: Creating collection under missing, soft-deleted, or non-Actor path returns 404
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active individual entity (non-Actor) exists with UUID "019273c5-9271-7000-8000-000000000001" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001/collections` with body:
  ```json
  {
    "name": "Character Gallery"
  }
  ```
- **THEN** the system returns HTTP 404 Not Found

#### Scenario: Creating collection with duplicate name for same owner returns 409
- **GIVEN** an active collection named "Featured Cast" already exists for owner "019273c5-9271-7000-8000-000000000001" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001/collections` with body:
  ```json
  {
    "name": "Featured Cast"
  }
  ```
- **THEN** the system returns HTTP 409 Conflict with an AIP-193 structured error response

#### Scenario: Creating collection with blank name returns 400
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "019273c5-9271-7000-8000-000000000001"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001/collections` with body:
  ```json
  {
    "name": "   "
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating collection with nonexistent explicit ownerId returns 400
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "019273c5-9271-7000-8000-000000000001"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001/collections` with body:
  ```json
  {
    "name": "Unanchored Collection",
    "ownerId": "019273c5-9271-7000-8000-000000000999"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request due to owner validation failure

### Requirement: Tenanted Studio Collection Retrieval Endpoint with Compressed and Expanded Forms
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}/collections/{collection-id}` allowing clients to retrieve details of a specific **Collections** **Entity** generated by `{actor-id}`. The system SHALL resolve `{studio-id}` in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, or if `{actor-id}` or `{collection-id}` is not a valid UUID string, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL query `CollectionDAO` under `app.tenant_id = :tenantId` scoped to `{actor-id}`. If `{actor-id}` is an active **Actor** and the collection exists, is active (`deleted_on IS NULL`), and has `entities.generator = actorId`, the system SHALL format the response according to the query parameter `?form={compressed|expanded}`, always including `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{actor-id}"` alongside `ownerId`. If `form` is omitted or equal to "compressed", `orderedItems` SHALL contain an array of UUID strings corresponding to the referenced entities in sequence order. If `form=expanded`, `orderedItems` SHALL contain an array of full JSON objects representing each active referenced entity in sequence order (including **Individual**, **Events**, **Games**, **System**, **Campaigns**, **Locations**, **Link**, **Actor**, and **Collections** items hydrated with their `type` and display `name`), where each hydrated item includes its own `generator` field formatted as the **Actor** URI string `"/api/studios/{studio-id}/v1/actors/{item-generator-id}"` (never an embedded **Actor** object). Any referenced entity that has been soft-deleted (`deleted_on IS NOT NULL`) SHALL be excluded from `orderedItems` and decremented from `totalItems`. If `{actor-id}` is missing or soft-deleted, or if the collection does not exist, belongs to another **Tenant**, has a `generator` other than `{actor-id}`, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Retrieve active collection in default compressed form
- **GIVEN** an active studio "valiant" and active actor `actorId` have an active collection with UUID `collectionId` containing 2 items referencing individuals `id1` and `id2`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** response body contains `generator` matching `/api/studios/valiant/v1/actors/{actorId}`
- **AND** `totalItems` is 2
- **AND** `orderedItems` contains the array of UUID strings `[id1, id2]`

#### Scenario: Retrieve active collection in expanded form
- **GIVEN** an active studio "valiant" and active actor `actorId` have an active collection with UUID `collectionId` containing 2 items referencing individuals `id1` and `id2` generated by actor `itemActorId`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}?form=expanded`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** `totalItems` is 2
- **AND** `orderedItems` contains an array of 2 full JSON objects representing `id1` and `id2` with their respective `id`, `name`, `type`, and `generator` equal to `"/api/studios/valiant/v1/actors/{itemActorId}"`

#### Scenario: Retrieve expanded collection containing System, Campaign, and Game items
- **GIVEN** an active studio "valiant" and active actor `actorId` have an active collection `collectionId` containing an active system `sysId` ("Vampire: The Masquerade"), an active campaign `campId` ("Seattle By Night"), and an active game `gameId` ("Opening Elysium")
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}?form=expanded`
- **THEN** the system returns HTTP 200 OK with `totalItems` equal to 3
- **AND** `orderedItems` hydrates `sysId` with `type` "System" and `name` "Vampire: The Masquerade", `campId` with `type` "Campaign" and `name` "Seattle By Night", and `gameId` with `type` "Game" and `name` "Opening Elysium"

#### Scenario: Soft-deleted referenced entity is excluded from ordered items
- **GIVEN** an active collection generated by `actorId` contains item `id1` and item `id2`
- **AND** entity `id2` is soft-deleted
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}`
- **THEN** the system returns HTTP 200 OK
- **AND** `totalItems` is 1
- **AND** `orderedItems` contains only `[id1]`

#### Scenario: Retrieving collection under mismatched actor generator returns 404
- **GIVEN** an active studio "valiant" has two active actors `actorA` and `actorB`
- **AND** collection `collectionId` was created with `generator = actorA`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/actors/{actorB}/collections/{collectionId}`
- **THEN** the system returns HTTP 404 Not Found

#### Scenario: Retrieving nonexistent collection returns 404
- **GIVEN** an active studio exists with alias "valiant" and an active actor `actorId`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/actors/{actorId}/collections/019273c5-9271-7000-8000-000000000999`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Deliberate Omission of Collection Listing Endpoint
The system SHALL NOT expose a collection listing endpoint on `/api/studios/{studio-id}/v1/actors/{actor-id}/collections`. Any HTTP GET request directed to `/api/studios/{studio-id}/v1/actors/{actor-id}/collections` SHALL immediately complete with HTTP 404 Not Found and a descriptive message stating that listing collections is not supported.

#### Scenario: Attempting to list collections returns 404
- **GIVEN** an active studio exists with alias "valiant" and an active actor `actorId`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/actors/{actorId}/collections`
- **THEN** the system returns HTTP 404 Not Found with error message "Listing collections is not supported"

### Requirement: Tenanted Studio Collection Partial Update Endpoint
The system SHALL expose an HTTP PATCH endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}/collections/{collection-id}` allowing clients to partially update a **Collections** **Entity** generated by `{actor-id}` conforming to Google AIP-134. Updatable fields SHALL include `name` and `summary`. The request MAY supply an optional `update_mask` query parameter. If `name` is updated and matches another existing collection for the same semantic **owner** (`owner_id`), the system SHALL return HTTP 409 Conflict. If `{actor-id}` is active and matches the collection's `generator` and the update succeeds, the system SHALL update `updated_on` and return HTTP 200 OK with the updated collection details in default compressed form. If `{actor-id}` is missing or soft-deleted, or if the collection does not exist, has a `generator` other than `{actor-id}`, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully update collection name and summary
- **GIVEN** an active collection generated by `actorId` exists with UUID `collectionId` and name "Old Name"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}` with body:
  ```json
  {
    "name": "Updated Name",
    "summary": "Updated summary description"
  }
  ```
- **THEN** the system returns HTTP 200 OK with `name` equal to "Updated Name" and `summary` equal to "Updated summary description"

#### Scenario: Patching collection under mismatched actor returns 404
- **GIVEN** an active collection `collectionId` generated by `actorA` and another active actor `actorB`
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/actors/{actorB}/collections/{collectionId}` with body `{"name": "Hijacked"}`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Tenanted Studio Collection Soft-Delete Endpoint
The system SHALL expose an HTTP DELETE endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}/collections/{collection-id}` allowing clients to soft-delete a collection resource generated by `{actor-id}`. Soft deletion SHALL update `njall_users.entities.deleted_on` to the current timestamp. The endpoint SHALL return HTTP 204 No Content. If `{actor-id}` is missing or soft-deleted, or if the collection does not exist, belongs to another studio tenant, has a `generator` other than `{actor-id}`, or is already soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully soft-delete active collection
- **GIVEN** an active collection generated by `actorId` exists with UUID `collectionId`
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}`
- **THEN** the system returns HTTP 204 No Content
- **AND** a subsequent GET request for `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}` returns HTTP 404 Not Found

### Requirement: Tenanted Studio Collection Add Item Custom Method
The system SHALL expose an HTTP POST custom method endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}/collections/{collection-id}:addItem` (AIP-136) allowing clients to insert an entity into a **Collections** resource generated by `{actor-id}`. The request body SHALL contain required `refersToId` (UUID) and optional placement specifiers `before` (UUID) and `index` (integer). The referenced entity MUST exist, belong to the same studio tenant, and not be soft-deleted. If both `before` and `index` are omitted, the system SHALL prepend the item to the head of the collection (newest first, ordinal 0), shifting existing items. If `before` is supplied, the system SHALL place the item immediately ahead of the item referencing that `before` entity ID. If `index` is supplied, the system SHALL insert the item at that zero-based index. If the item is already present in the collection, the operation SHALL be idempotent and return HTTP 204 No Content without creating duplicates. Upon successful insertion, the endpoint SHALL return HTTP 204 No Content. If `{actor-id}` is missing or soft-deleted, or if the collection does not exist, has a `generator` other than `{actor-id}`, or is soft-deleted, or if the referenced entity does not exist, the system SHALL return HTTP 404 Not Found.

#### Scenario: Prepend item to collection by default
- **GIVEN** an active collection `collectionId` generated by `actorId` has existing item `itemA` at index 0
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}:addItem` with body:
  ```json
  {
    "refersToId": "019273c5-9271-7000-8000-000000000002"
  }
  ```
- **THEN** the system returns HTTP 204 No Content
- **AND** a subsequent GET request shows `refersToId` at index 0 and `itemA` at index 1

#### Scenario: Insert item before specified target item
- **GIVEN** an active collection generated by `actorId` has items `[itemA, itemB]`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}:addItem` with body:
  ```json
  {
    "refersToId": "019273c5-9271-7000-8000-000000000003",
    "before": "itemB"
  }
  ```
- **THEN** the system returns HTTP 204 No Content
- **AND** a subsequent GET request shows items in sequence `[itemA, refersToId, itemB]`

#### Scenario: Insert item at specific integer index
- **GIVEN** an active collection generated by `actorId` has items `[itemA, itemB]`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}:addItem` with body:
  ```json
  {
    "refersToId": "019273c5-9271-7000-8000-000000000003",
    "index": 1
  }
  ```
- **THEN** the system returns HTTP 204 No Content
- **AND** a subsequent GET request shows items in sequence `[itemA, refersToId, itemB]`

#### Scenario: Idempotent add item already present
- **GIVEN** an active collection generated by `actorId` already contains item `itemA`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}:addItem` with body:
  ```json
  {
    "refersToId": "itemA"
  }
  ```
- **THEN** the system returns HTTP 204 No Content without creating duplicate items

### Requirement: Tenanted Studio Collection Remove Item Custom Method
The system SHALL expose an HTTP POST custom method endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}/collections/{collection-id}:removeItem` (AIP-136) allowing clients to remove an entity from a **Collections** resource generated by `{actor-id}`. The request body SHALL contain required `refersToId` (UUID). The system SHALL delete the corresponding junction record from `njall_users.collection_items` and compact remaining ordinals. The operation SHALL be idempotent; if the entity is not present in the collection, the endpoint SHALL return HTTP 204 No Content. If `{actor-id}` is missing or soft-deleted, or if the collection itself does not exist, has a `generator` other than `{actor-id}`, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully remove item from collection
- **GIVEN** an active collection generated by `actorId` contains items `[itemA, itemB, itemC]`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}:removeItem` with body:
  ```json
  {
    "refersToId": "itemB"
  }
  ```
- **THEN** the system returns HTTP 204 No Content
- **AND** a subsequent GET request shows remaining items `[itemA, itemC]`

#### Scenario: Idempotently remove item not present
- **GIVEN** an active collection generated by `actorId` contains item `itemA` but not `itemB`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/actors/{actorId}/collections/{collectionId}:removeItem` with body:
  ```json
  {
    "refersToId": "itemB"
  }
  ```
- **THEN** the system returns HTTP 204 No Content
