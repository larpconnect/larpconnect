# user-studio-links-api Specification

## Purpose

Tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/links` for creating, retrieving, updating, and soft-deleting studio link entities with Common Table Inheritance (CTI) and PostgreSQL Row-Level Security (RLS) isolation.

## Requirements

### Requirement: Tenanted Studio Link Creation Endpoint
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/links` allowing authenticated clients to create an external **Link** for a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public `studioId` UUID or a unique **Alias** string, and SHALL NOT accept internal database `tenant_id` values. The system SHALL resolve `{studio-id}` directly in-memory against `StudioLookupCache` to determine the corresponding internal `tenant_id`. If `{studio-id}` is not found or is marked as soft-deleted, the system SHALL return HTTP 404 Not Found directly on the HTTP route without database queries or **Actor** dispatching. Upon resolving `tenant_id`, the system SHALL validate the request payload (`linkType`, `url`, `mediaType`, and optional `summary`). If validation succeeds, the system SHALL dispatch a creation command to `LinkActor` to persist the record in `njall_users.entities` (with generated UUIDv7, `tenant_id`, `entity_type = 'Link'`, and audit timestamps) and `njall_users.links` via `LinkDAO` using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created containing the public link `id`, `linkType`, `url`, `mediaType`, `summary`, `createdOn`, and `updatedOn`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successfully create link for studio by alias
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/links` with body:
  ```json
  {
    "linkType": "website",
    "url": "https://valiant.example.com",
    "mediaType": "text/html",
    "summary": "Official Studio Homepage"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `linkType` equal to "website", `url` equal to "https://valiant.example.com", `mediaType` equal to "text/html", `summary` equal to "Official Studio Homepage", and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Successfully create link for studio by public studio ID
- **GIVEN** an active studio exists with public studio UUID `studioId`
- **WHEN** a client sends a POST request to `/api/studios/{studioId}/v1/links` with a valid link payload
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the returned link is persisted under the studio's internal tenant

#### Scenario: Creating link for nonexistent studio returns 404
- **GIVEN** no studio exists with alias "unknown"
- **WHEN** a client sends a POST request to `/api/studios/unknown/v1/links` with a valid link payload
- **THEN** the system returns HTTP 404 Not Found without querying the database

#### Scenario: Creating link with invalid payload returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/links` with an empty URL or invalid URI format
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

### Requirement: Tenanted Studio Link Retrieval Endpoint
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/links/{link-id}` allowing clients to retrieve details of a specific **Link**. The system SHALL resolve `{studio-id}` in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found. If `{link-id}` is not a valid UUID string, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL query the link via `LinkActor` and `LinkDAO` using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. If the link exists under the resolved tenant and has not been soft-deleted (`entities.deleted_on IS NULL`), the system SHALL return HTTP 200 OK with the link's public fields. If the link does not exist under that tenant or has `deleted_on` populated, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully retrieve active link
- **GIVEN** an active studio "valiant" and a persisted link with UUID `linkId`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/links/{linkId}`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body matches the link attributes and excludes `tenantId`

#### Scenario: Retrieving soft-deleted link returns 404
- **GIVEN** an active studio "valiant" and a link whose `deleted_on` timestamp is non-null
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/links/{linkId}`
- **THEN** the system returns HTTP 404 Not Found

#### Scenario: Cross-tenant link access returns 404
- **GIVEN** studio "valiant" with a link `linkId`
- **AND** another distinct studio "haven"
- **WHEN** a client sends a GET request to `/api/studios/haven/v1/links/{linkId}`
- **THEN** the system returns HTTP 404 Not Found due to Row-Level Security isolation

### Requirement: Tenanted Studio Link Modification Endpoint
The system SHALL expose an HTTP PATCH endpoint at `/api/studios/{studio-id}/v1/links/{link-id}` allowing partial updates to mutable link fields (`url`, `linkType`, `mediaType`, `summary`) adhering to Google AIP-134. The system SHALL support an optional `update_mask` parameter. Upon receiving a valid request for an active link within the studio's tenant boundary, the system SHALL update the specified fields in `njall_users.links` and `njall_users.entities`, refresh `entities.updated_on` to `CURRENT_TIMESTAMP`, and return HTTP 200 OK with the updated link object. If the link does not exist, belongs to another tenant, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully update link URL and summary
- **GIVEN** an active link with ID `linkId` for studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/links/{linkId}` with body:
  ```json
  {
    "url": "https://updated.example.com",
    "summary": "Updated Homepage"
  }
  ```
- **THEN** the system returns HTTP 200 OK
- **AND** the response reflects the updated URL and summary with refreshed `updatedOn`

#### Scenario: Patching nonexistent or soft-deleted link returns 404
- **GIVEN** a link ID that has been soft-deleted or does not exist
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/links/{linkId}`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Tenanted Studio Link Soft Deletion Endpoint
The system SHALL expose an HTTP DELETE endpoint at `/api/studios/{studio-id}/v1/links/{link-id}`. Upon invocation, the system SHALL set `entities.deleted_on = CURRENT_TIMESTAMP` for the matching `(tenant_id, id)` row via `LinkDAO`. The row in `njall_users.links` and `njall_users.entities` SHALL NOT be physically removed. If the target link exists and is active, the system SHALL return HTTP 200 OK or HTTP 204 No Content. If the link does not exist or is already soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully soft delete link
- **GIVEN** an active link `linkId` under studio "valiant"
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/links/{linkId}`
- **THEN** the system returns HTTP 200 OK or 204 No Content
- **AND** subsequent GET requests to `/api/studios/valiant/v1/links/{linkId}` return HTTP 404 Not Found
- **AND** the row in `njall_users.entities` has a non-null `deleted_on` timestamp

### Requirement: Disallow Unfiltered Studio Links Listing
The system SHALL NOT expose an unfiltered list route at `GET /api/studios/{studio-id}/v1/links`. Requests to `GET /api/studios/{studio-id}/v1/links` or `GET /api/studios/{studio-id}/v1/links/` SHALL return HTTP 404 Not Found or HTTP 405 Method Not Allowed.

#### Scenario: Requesting root links path with GET returns not found or method not allowed
- **GIVEN** an active studio "valiant"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/links`
- **THEN** the system returns HTTP 404 or 405 without executing any database listing query
