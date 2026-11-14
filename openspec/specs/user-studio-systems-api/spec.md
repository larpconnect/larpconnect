# user-studio-systems-api Specification

## Purpose

Tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/systems` for creating, retrieving, listing, partially updating, and soft-deleting studio game system ruleset entities extending entities via Common Table Inheritance (CTI) with PostgreSQL Row-Level Security (RLS) isolation.

## Requirements

### Requirement: Tenanted Studio System Creation Endpoint
The system SHALL expose `POST /api/studios/{studio-id}/v1/systems` (`404` on missing/deleted studio **Alias** or UUID) requiring active `'Actor'` `generator` (UUID or studio URI) and non-blank `name` plus optional `summary` (`400` on invalid input, `409` on case-insensitive per-**Tenant** name collision), persisting an active `'System'` **Entity** and returning `201 Created` with canonical `generator` URI and no `tenantId`.

#### Scenario: Successfully create system with bare UUID generator and summary
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/systems` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "name": "Vampire: The Masquerade",
    "summary": "Fifth Edition Mind's Eye Theatre ruleset"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `generator` equal to "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010", `name` equal to "Vampire: The Masquerade", `summary` equal to "Fifth Edition Mind's Eye Theatre ruleset", and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Successfully create system with canonical generator URI and minimal fields
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/systems` with body:
  ```json
  {
    "generator": "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010",
    "name": "Nordic Freeform"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains `generator` equal to "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010", `name` equal to "Nordic Freeform", and null `summary`

#### Scenario: Creating system with case-insensitive duplicate name in same studio returns 409
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010"
- **AND** a system named "Vampire: The Masquerade" already exists under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/systems` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "name": "vampire: the masquerade"
  }
  ```
- **THEN** the system returns HTTP 409 Conflict with an AIP-193 structured error response

#### Scenario: Creating system with blank name returns 400
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/systems` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "name": "   "
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating system with non-Actor generator returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active individual entity exists with UUID "01925b6a-93be-7000-8000-000000000099" under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/systems` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000099",
    "name": "Cthulhu Live"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating system for nonexistent studio returns 404
- **GIVEN** no studio exists with alias "unknown"
- **WHEN** a client sends a POST request to `/api/studios/unknown/v1/systems` with a valid system payload
- **THEN** the system returns HTTP 404 Not Found without querying the database

### Requirement: Tenanted Studio System Collection Listing Endpoint
The system SHALL expose `GET /api/studios/{studio-id}/v1/systems` (`404` if `{studio-id}` is missing or soft-deleted) querying **GameSystem** persistence (`GameSystemDAO`) under `app.tenant_id = :tenantId` and returning `200 OK` with active `'System'` entities ordered by `lower(name) ASC, created_on DESC` without tenant identifiers.

#### Scenario: Successfully list active systems ordered alphabetically by name
- **GIVEN** an active studio exists with alias "valiant"
- **AND** active systems named "Werewolf: The Apocalypse" and "Changeling: The Dreaming" exist for studio "valiant"
- **AND** a soft-deleted system named "Archived Rules" exists for studio "valiant"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/systems`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body is a JSON array containing 2 system objects ordered as "Changeling: The Dreaming" followed by "Werewolf: The Apocalypse"

#### Scenario: Empty collection returned when studio has no active systems
- **GIVEN** an active studio exists with alias "valiant" with no systems
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/systems`
- **THEN** the system returns HTTP 200 OK with an empty JSON array `[]`

### Requirement: Tenanted Studio System Detail Retrieval Endpoint
The system SHALL expose `GET /api/studios/{studio-id}/v1/systems/{system-id}` returning `200 OK` with the **System** details when `{studio-id}` is active and `{system-id}` is a valid UUID of an active `'System'` **Entity** in the resolved **Tenant**, or `404 Not Found` otherwise.

#### Scenario: Successfully retrieve active system
- **GIVEN** an active studio "valiant" and an active system with UUID `systemId`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/systems/{systemId}`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body matches the system attributes with `generator` formatted as a canonical studio **Actor** URI and excludes `tenantId`

#### Scenario: Cross-tenant or soft-deleted system retrieval returns 404
- **GIVEN** studio "valiant" with a system `systemId` and another distinct studio "haven"
- **WHEN** a client sends a GET request to `/api/studios/haven/v1/systems/{systemId}`
- **THEN** the system returns HTTP 404 Not Found due to Row-Level Security isolation

### Requirement: Tenanted Studio System Partial Update Endpoint
The system SHALL expose `PATCH /api/studios/{studio-id}/v1/systems/{system-id}` conforming to AIP-134 (`update_mask` for `name` and nullable `summary`), rejecting blank `name` (`400`) or case-insensitive **Tenant** name collisions (`409`), refreshing `updated_on`, and returning `200 OK` (or `404` if studio or active system is missing).

#### Scenario: Successfully update system name and summary
- **GIVEN** an active system `systemId` named "VTM" under studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/systems/{systemId}` with body:
  ```json
  {
    "name": "Vampire: The Masquerade 5th Edition",
    "summary": "Updated chronicle ruleset"
  }
  ```
- **THEN** the system returns HTTP 200 OK with `name` equal to "Vampire: The Masquerade 5th Edition", `summary` equal to "Updated chronicle ruleset", and refreshed `updatedOn`

#### Scenario: Patching system name to collide with another system in studio returns 409
- **GIVEN** two active systems `systemA` named "Vampire" and `systemB` named "Werewolf" under studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/systems/{systemB}` with body `{"name": "VAMPIRE"}`
- **THEN** the system returns HTTP 409 Conflict with an AIP-193 structured error response

### Requirement: Tenanted Studio System Soft Deletion Endpoint
The system SHALL expose `DELETE /api/studios/{studio-id}/v1/systems/{system-id}` setting `entities.deleted_on` and `updated_on` to `CURRENT_TIMESTAMP` on the active `'System'` row without physical removal, returning `204 No Content` on success or `404 Not Found` if missing or already soft-deleted.

#### Scenario: Successfully soft delete system
- **GIVEN** an active system `systemId` under studio "valiant"
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/systems/{systemId}`
- **THEN** the system returns HTTP 204 No Content
- **AND** subsequent GET requests to `/api/studios/valiant/v1/systems/{systemId}` return HTTP 404 Not Found
