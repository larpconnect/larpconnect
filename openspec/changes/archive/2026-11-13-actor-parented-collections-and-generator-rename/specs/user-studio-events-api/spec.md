## MODIFIED Requirements

### Requirement: Tenanted Studio Event Creation Endpoint
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/events` allowing authenticated clients to create an **Events** **Entity** under a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public studio UUID or an **Alias** string, resolving in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL validate the request payload (`title` required non-blank, `generator` required string accepting either a bare **Actor** UUID or a canonical studio **Actor** URI `/api/studios/{studio-id}/v1/actors/{actor-id}` matching `{studio-id}`, optional `summary`, optional `locationId`, optional `startTime`, and optional `endTime`). The `generator` field MUST resolve to an existing, active **Actor** **Entity** (`entity_type = 'Actor'`) belonging to the same studio **Tenant**; if `generator` is missing, blank, malformed, references another studio, or does not resolve to an active **Actor** in the same **Tenant**, the system SHALL return HTTP 400 Bad Request. If `startTime` and `endTime` are both present, the system SHALL require `endTime >= startTime`. If `locationId` is provided, it MUST reference an existing **Locations** **Entity** belonging to the same studio tenant; any referential constraint violation SHALL result in HTTP 400 Bad Request. If validation succeeds, the system SHALL persist the record in `njall_users.entities` (with `generator` set to the resolved **generator** **Actor** UUID and `entity_type = 'Event'`) and `njall_users.events` using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created containing the public event `id`, `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{generator-id}"`, `title`, `summary`, `locationId`, `startTime`, `endTime`, `createdOn`, and `updatedOn`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successfully create event with all fields
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010" and an active location exists with UUID "01925b6a-93be-7000-8000-000000000001" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "title": "Autumn Harvest Festival",
    "summary": "Annual festival and gathering",
    "locationId": "01925b6a-93be-7000-8000-000000000001",
    "startTime": "2026-10-25T18:00:00Z",
    "endTime": "2026-10-27T12:00:00Z"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `generator` equal to "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010", `title` equal to "Autumn Harvest Festival", `summary` equal to "Annual festival and gathering", `locationId` equal to "01925b6a-93be-7000-8000-000000000001", `startTime` equal to "2026-10-25T18:00:00Z", `endTime` equal to "2026-10-27T12:00:00Z", and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Successfully create event with minimal fields
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` with body:
  ```json
  {
    "generator": "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010",
    "title": "Town Hall Gathering"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `generator` equal to "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010", `title` equal to "Town Hall Gathering", null `locationId`, null `summary`, null `startTime`, and null `endTime`

#### Scenario: Creating event with missing or non-Actor generator returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` omitting `generator` or referencing a non-Actor entity
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating event with blank title returns 400
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "title": "   "
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating event with invalid time order returns 400
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "title": "Broken Timeline Event",
    "startTime": "2026-10-27T12:00:00Z",
    "endTime": "2026-10-25T18:00:00Z"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating event with nonexistent location returns 400
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/events` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "title": "Unanchored Event",
    "locationId": "01925b6a-93be-7000-8000-000000000999"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request due to foreign key violation

#### Scenario: Creating event for nonexistent studio returns 404
- **GIVEN** no studio exists with alias "unknown"
- **WHEN** a client sends a POST request to `/api/studios/unknown/v1/events` with a valid event payload
- **THEN** the system returns HTTP 404 Not Found without querying the database

