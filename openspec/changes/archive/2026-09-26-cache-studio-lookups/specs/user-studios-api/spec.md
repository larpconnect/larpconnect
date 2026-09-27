# user-studios-api Specification Delta

## MODIFIED Requirements

### Requirement: User Studio Retrieval Endpoint
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/studio` allowing clients to retrieve details of a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public `studioId` UUID or a unique `alias` string, and SHALL NOT accept internal database `tenant_id` values. The system SHALL resolve `{studio-id}` directly in-memory against the `StudioLookupCache` to determine the corresponding internal `tenant_id`. If `{studio-id}` is not found or is marked as soft-deleted, the system SHALL return HTTP 404 Not Found directly on the HTTP route without database queries or actor dispatching. Upon resolving `tenant_id`, the system SHALL dispatch a request to `StudioActor` to query the **Studio** from the user data layer via `StudioDAO` using an active `@NjallUsers` session configured with `app.tenant_id = :tenant_id`, and SHALL return HTTP 200 OK containing the public `studioId`, `alias`, and `name`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successfully retrieve studio by alias
- **GIVEN** an active studio exists with alias "valiant", public studio ID, and name "Valiant Games"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/studio`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body contains `studioId`, `alias` equal to "valiant", and `name` equal to "Valiant Games"
- **AND** the response body does not contain `tenantId`

#### Scenario: Successfully retrieve studio by public studio ID
- **GIVEN** an active studio exists with known public studio UUID and name "Valiant Games"
- **WHEN** a client sends a GET request to `/api/studios/{studioId}/v1/studio`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body contains `studioId`, `alias`, and `name` equal to "Valiant Games"

#### Scenario: Requesting nonexistent studio returns 404
- **GIVEN** no studio exists with alias "nonexistent"
- **WHEN** a client sends a GET request to `/api/studios/nonexistent/v1/studio`
- **THEN** the system returns HTTP 404 Not Found with an AIP-193 structured error object

#### Scenario: Requesting soft-deleted studio returns 404
- **GIVEN** a studio exists with alias "abandoned" but has a non-null `deleted_at` timestamp
- **WHEN** a client sends a GET request to `/api/studios/abandoned/v1/studio`
- **THEN** the system returns HTTP 404 Not Found with an AIP-193 structured error object
