## MODIFIED Requirements

### Requirement: Tenanted Studio Location Creation Endpoint
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/locations` allowing authenticated clients to create a **Locations** **Entity** for a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public studio UUID or an **Alias** string, resolving in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL validate the request payload (`name` required non-blank, `generator` required string accepting either a bare **Actor** UUID or a canonical studio **Actor** URI `/api/studios/{studio-id}/v1/actors/{actor-id}` matching `{studio-id}`, and optional `summary`). The `generator` field MUST resolve to an existing, active **Actor** **Entity** (`entity_type = 'Actor'`) belonging to the same studio **Tenant**; if `generator` is missing, blank, malformed, references another studio, or does not resolve to an active **Actor** in the same **Tenant**, the system SHALL return HTTP 400 Bad Request. If validation succeeds, the system SHALL persist the record in `njall_users.entities` (with generated UUIDv7, `tenant_id`, `owner` set to the resolved `generator` **Actor** UUID, `entity_type = 'Location'`) and `njall_users.locations` (with `(tenant_id, id)` and `name`) using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created containing the public location `id`, `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{owner-id}"`, `name`, `summary`, `createdOn`, and `updatedOn`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successfully create location for studio by alias
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/locations` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "name": "Camp Whispering Pines",
    "summary": "Main outdoor event campsite"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `generator` equal to "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010", `name` equal to "Camp Whispering Pines", `summary` equal to "Main outdoor event campsite", and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Creating location with blank name returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/locations` with a blank `name` or missing/non-Actor `generator`
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating location for nonexistent studio returns 404
- **GIVEN** no studio exists with alias "unknown"
- **WHEN** a client sends a POST request to `/api/studios/unknown/v1/locations` with a valid location payload
- **THEN** the system returns HTTP 404 Not Found without querying the database
