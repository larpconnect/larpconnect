## ADDED Requirements

### Requirement: Tenanted Studio Event Creation Endpoint
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/events` allowing authenticated clients to create an **Events** **Entity** under a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public studio UUID or an **Alias** string, resolving in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL validate the request payload (`title` required non-blank, optional `summary`, optional `locationId`, optional `startTime`, and optional `endTime`). If `startTime` and `endTime` are both present, the system SHALL require `endTime >= startTime`. If `locationId` is provided, it MUST reference an existing **Locations** **Entity** belonging to the same studio tenant; any referential constraint violation SHALL result in HTTP 400 Bad Request. If validation succeeds, the system SHALL persist the record in `njall_users.entities` (with `entity_type = 'Event'`) and `njall_users.events` using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created containing the public event `id`, `title`, `summary`, `locationId`, `startTime`, `endTime`, `createdOn`, and `updatedOn`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successfully create event with all fields
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active location exists with UUID "01925b6a-93be-7000-8000-000000000001" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` with body:
  ```json
  {
    "title": "Autumn Harvest Festival",
    "summary": "Annual festival and gathering",
    "locationId": "01925b6a-93be-7000-8000-000000000001",
    "startTime": "2026-10-25T18:00:00Z",
    "endTime": "2026-10-27T12:00:00Z"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `title` equal to "Autumn Harvest Festival", `summary` equal to "Annual festival and gathering", `locationId` equal to "01925b6a-93be-7000-8000-000000000001", `startTime` equal to "2026-10-25T18:00:00Z", `endTime` equal to "2026-10-27T12:00:00Z", and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Successfully create event with minimal fields
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` with body:
  ```json
  {
    "title": "Town Hall Gathering"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `title` equal to "Town Hall Gathering", null `locationId`, null `summary`, null `startTime`, and null `endTime`

#### Scenario: Creating event with blank title returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` with body:
  ```json
  {
    "title": "   "
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating event with invalid time order returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` with body:
  ```json
  {
    "title": "Broken Timeline Event",
    "startTime": "2026-10-27T12:00:00Z",
    "endTime": "2026-10-25T18:00:00Z"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating event with nonexistent location returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` with body:
  ```json
  {
    "title": "Unanchored Event",
    "locationId": "01925b6a-93be-7000-8000-000000000999"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request due to foreign key violation

#### Scenario: Creating event for nonexistent studio returns 404
- **GIVEN** no studio exists with alias "unknown"
- **WHEN** a client sends a POST request to `/api/studios/unknown/v1/events` with a valid event payload
- **THEN** the system returns HTTP 404 Not Found without querying the database

### Requirement: Tenanted Studio Event Collection Listing Endpoint
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/events` allowing clients to retrieve all active **Events** belonging to the specified **Studio**. The system SHALL resolve `{studio-id}` in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL query `EventDAO` under `app.tenant_id = :tenantId` for all events where `entities.deleted_on IS NULL`, ordered by `start_time ASC NULLS LAST, created_on DESC`. The endpoint SHALL return HTTP 200 OK containing a JSON array of active events without tenant identifiers.

#### Scenario: Successfully list active events for studio
- **GIVEN** an active studio exists with alias "valiant"
- **AND** two active events exist for studio "valiant"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/events`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body is a JSON array containing 2 event objects

#### Scenario: Soft-deleted events are excluded from collection list
- **GIVEN** an active studio exists with alias "valiant"
- **AND** one active event and one soft-deleted event exist for studio "valiant"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/events`
- **THEN** the system returns HTTP 200 OK with a JSON array containing only the 1 active event

#### Scenario: Empty collection returned when studio has no events
- **GIVEN** an active studio exists with alias "valiant"
- **AND** no events exist for studio "valiant"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/events`
- **THEN** the system returns HTTP 200 OK with an empty JSON array `[]`

### Requirement: Tenanted Studio Event Detail Retrieval Endpoint
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/events/{id}` allowing clients to retrieve details of a specific **Events** **Entity**. The system SHALL resolve `{studio-id}` in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, or if `{id}` is not a valid UUID string, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL query `EventDAO` under `app.tenant_id = :tenantId`. If the event exists and is active (`deleted_on IS NULL`), the system SHALL return HTTP 200 OK with the event details. If the event does not exist, belongs to another **Tenant**, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully retrieve active event
- **GIVEN** an active studio "valiant" and an active event with UUID `eventId`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/events/{eventId}`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body matches the event attributes and excludes `tenantId`

#### Scenario: Retrieving soft-deleted event returns 404
- **GIVEN** an active studio "valiant" and an event whose `deleted_on` timestamp is non-null
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/events/{eventId}`
- **THEN** the system returns HTTP 404 Not Found

#### Scenario: Cross-tenant event access returns 404
- **GIVEN** studio "valiant" with an event `eventId`
- **AND** another distinct studio "haven"
- **WHEN** a client sends a GET request to `/api/studios/haven/v1/events/{eventId}`
- **THEN** the system returns HTTP 404 Not Found due to Row-Level Security isolation

### Requirement: Tenanted Studio Event Partial Update Endpoint
The system SHALL expose an HTTP PATCH endpoint at `/api/studios/{studio-id}/v1/events/{id}` allowing partial updates to mutable event fields (`title`, `summary`, `locationId`, `startTime`, `endTime`) conforming to Google AIP-134. If `update_mask` query parameter is provided, only specified fields SHALL be modified; otherwise, all provided fields in the payload SHALL be updated. If `title` is updated, it MUST be non-blank. If `startTime` or `endTime` are updated, the resulting effective time values MUST satisfy `endTime >= startTime` when both are present. If `locationId` is updated to a non-null value, it MUST reference a valid location within the same studio tenant. Upon successfully applying updates, the system SHALL refresh `updated_on` and return HTTP 200 OK with the updated event object. If the event does not exist, belongs to another **Tenant**, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully update event title and time range
- **GIVEN** an active event `eventId` under studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/events/{eventId}` with body:
  ```json
  {
    "title": "Autumn Harvest Festival - Rescheduled",
    "startTime": "2026-10-26T18:00:00Z",
    "endTime": "2026-10-28T12:00:00Z"
  }
  ```
- **THEN** the system returns HTTP 200 OK with the updated title and refreshed `updatedOn`

#### Scenario: Patching with invalid time range returns 400
- **GIVEN** an active event `eventId` under studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/events/{eventId}` with body:
  ```json
  {
    "startTime": "2026-10-29T18:00:00Z",
    "endTime": "2026-10-28T12:00:00Z"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Patching soft-deleted event returns 404
- **GIVEN** an event ID that has been soft-deleted
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/events/{eventId}` with a valid update payload
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Tenanted Studio Event Soft Deletion Endpoint
The system SHALL expose an HTTP DELETE endpoint at `/api/studios/{studio-id}/v1/events/{id}`. Upon invocation, the system SHALL set `entities.deleted_on = CURRENT_TIMESTAMP` for the matching `(tenant_id, id)` row. The row in `njall_users.events` and `njall_users.entities` SHALL NOT be physically removed. If the target event exists and is active, the system SHALL return HTTP 204 No Content. If the event does not exist or is already soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully soft delete event
- **GIVEN** an active event `eventId` under studio "valiant"
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/events/{eventId}`
- **THEN** the system returns HTTP 204 No Content
- **AND** subsequent GET requests to `/api/studios/valiant/v1/events/{eventId}` return HTTP 404 Not Found

#### Scenario: Deleting already soft-deleted event returns 404
- **GIVEN** an event `eventId` that is already soft-deleted under studio "valiant"
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/events/{eventId}`
- **THEN** the system returns HTTP 404 Not Found
