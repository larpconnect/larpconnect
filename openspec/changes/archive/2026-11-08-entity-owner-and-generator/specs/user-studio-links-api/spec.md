## MODIFIED Requirements

### Requirement: Tenanted Studio Link Creation Endpoint
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/links` allowing authenticated clients to create an external **Link** for a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public `studioId` UUID or a unique **Alias** string, and SHALL NOT accept internal database `tenant_id` values. The system SHALL resolve `{studio-id}` directly in-memory against `StudioLookupCache` to determine the corresponding internal `tenant_id`. If `{studio-id}` is not found or is marked as soft-deleted, the system SHALL return HTTP 404 Not Found directly on the HTTP route without database queries or **Actor** dispatching. Upon resolving `tenant_id`, the system SHALL validate the request payload (`generator` required string accepting either a bare **Actor** UUID or a canonical studio **Actor** URI `/api/studios/{studio-id}/v1/actors/{actor-id}` matching `{studio-id}`, `linkType`, `url`, `mediaType`, and optional `summary`). The `generator` field MUST resolve to an existing, active **Actor** **Entity** (`entity_type = 'Actor'`) belonging to the same studio **Tenant**; if `generator` is missing, blank, malformed, references another studio, or does not resolve to an active **Actor** in the same **Tenant**, the system SHALL return HTTP 400 Bad Request. If validation succeeds, the system SHALL dispatch a creation command to `LinkActor` to persist the record in `njall_users.entities` (with generated UUIDv7, `tenant_id`, `owner` set to the resolved `generator` **Actor** UUID, `entity_type = 'Link'`, and audit timestamps) and `njall_users.links` via `LinkDAO` using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created containing the public link `id`, `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{owner-id}"`, `linkType`, `url`, `mediaType`, `summary`, `createdOn`, and `updatedOn`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successfully create link for studio by alias
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/links` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "linkType": "website",
    "url": "https://valiant.example.com",
    "mediaType": "text/html",
    "summary": "Official Studio Homepage"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `generator` equal to "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010", `linkType` equal to "website", `url` equal to "https://valiant.example.com", `mediaType` equal to "text/html", `summary` equal to "Official Studio Homepage", and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Successfully create link for studio by public studio ID
- **GIVEN** an active studio exists with public studio UUID `studioId` and an active actor exists with UUID `actorId`
- **WHEN** a client sends a POST request to `/api/studios/{studioId}/v1/links` with a valid link payload including `"generator": "/api/studios/{studioId}/v1/actors/{actorId}"`
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the returned link is persisted under the studio's internal tenant with `generator` equal to `"/api/studios/{studioId}/v1/actors/{actorId}"`

#### Scenario: Creating link for nonexistent studio returns 404
- **GIVEN** no studio exists with alias "unknown"
- **WHEN** a client sends a POST request to `/api/studios/unknown/v1/links` with a valid link payload
- **THEN** the system returns HTTP 404 Not Found without querying the database

#### Scenario: Creating link with invalid payload returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/links` with an empty URL, invalid URI format, or missing/non-Actor `generator`
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response
