# admin-management-api Specification Delta

## ADDED Requirements

### Requirement: Default Studio Roles Administration
The system SHALL expose administrative HTTP REST endpoints under kebab-cased path `/api/admin/v1/studio-roles` allowing administrators to manage system-wide default studio roles. The `GET /api/admin/v1/studio-roles` endpoint SHALL return all default studio roles sorted by name. The `GET /api/admin/v1/studio-roles/{id}` endpoint SHALL return the single role matching the UUID or HTTP 404 Not Found if missing. The `POST /api/admin/v1/studio-roles` endpoint SHALL accept a JSON payload `{"name": "<role-name>"}`, validate that `name` is non-blank, create the role, and return HTTP 201 Created with the role resource representation. If the role name already exists, the system SHALL return HTTP 409 Conflict. The `PATCH /api/admin/v1/studio-roles/{id}` endpoint SHALL update an existing role conforming to Google AIP-134, accepting an optional `update_mask` query parameter. If `update_mask` is omitted or contains `name`, the system SHALL update the role name, returning HTTP 200 OK with the updated role representation. If the role ID does not exist, the system SHALL return HTTP 404 Not Found.

#### Scenario: List default studio roles
- **GIVEN** existing default studio roles in the database
- **WHEN** an admin sends a GET request to `/api/admin/v1/studio-roles`
- **THEN** the system returns HTTP 200 OK with the array of default studio roles

#### Scenario: Retrieve single default studio role by ID
- **GIVEN** a default studio role exists with known ID
- **WHEN** an admin sends a GET request to `/api/admin/v1/studio-roles/{id}`
- **THEN** the system returns HTTP 200 OK with the matching role representation

#### Scenario: Create default studio role
- **GIVEN** an admin client with valid credentials
- **WHEN** an admin sends a POST request to `/api/admin/v1/studio-roles` with body `{"name": "ORGANIZER"}`
- **THEN** the system returns HTTP 201 Created with the created role representation

#### Scenario: Reject duplicate default studio role name
- **GIVEN** a default studio role exists with name "ORGANIZER"
- **WHEN** an admin sends a POST request to `/api/admin/v1/studio-roles` with body `{"name": "ORGANIZER"}`
- **THEN** the system returns HTTP 409 Conflict with an AIP-193 structured error object

#### Scenario: Update default studio role via AIP-134 PATCH
- **GIVEN** an existing default studio role with known ID and name "ORGANIZER"
- **WHEN** an admin sends a PATCH request to `/api/admin/v1/studio-roles/{id}?update_mask=name` with body `{"name": "LEAD_ORGANIZER"}`
- **THEN** the system returns HTTP 200 OK with the updated role representation showing name "LEAD_ORGANIZER"

#### Scenario: PATCH nonexistent default studio role returns 404
- **GIVEN** no default studio role exists with a random UUID
- **WHEN** an admin sends a PATCH request to `/api/admin/v1/studio-roles/{randomId}` with body `{"name": "NEW_NAME"}`
- **THEN** the system returns HTTP 404 Not Found with an AIP-193 structured error object

## MODIFIED Requirements

### Requirement: Studio Management Endpoints
The system SHALL expose HTTP REST endpoints under `/api/admin/v1/studios` allowing administrators to create, list, and retrieve studios. The `POST /api/admin/v1/studios` endpoint SHALL accept a JSON payload containing an `alias` string and an optional `name` string (defaulting to `alias` if omitted), validate that `alias` is strictly alphanumeric lowercase and underscores starting with a lowercase letter (`^[a-z][a-z0-9_]*$`), generate a UUIDv7 `tenant_id` and random UUIDv4 `studio_id`, persist the **Tenant** studio into `njall_users.studios` and the lookup routing entry into `njall_admin.studios_lookup`, and return HTTP 201 Created with the created studio representation. If the alias format is invalid, the system SHALL return HTTP 400 Bad Request. The `GET /api/admin/v1/studios` endpoint SHALL return an array of active studios, excluding soft-deleted studios by default, and including soft-deleted studios when the query parameter `include_deleted=true` is present. The `GET /api/admin/v1/studios/{id}` endpoint SHALL accept either a valid `studio_id` UUID or an `alias` string, returning HTTP 200 OK with the studio representation or HTTP 404 Not Found if the studio does not exist (or is soft-deleted without `include_deleted=true`).

#### Scenario: Successfully create studio with unique alias
- **GIVEN** an administrative client with valid credentials
- **WHEN** the client sends a POST request to `/api/admin/v1/studios` with JSON body `{"alias": "valhalla"}`
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains `tenantId`, `studioId`, `alias` equal to "valhalla", `createdAt`, and `updatedAt`

#### Scenario: Reject invalid studio alias format
- **GIVEN** an administrative client
- **WHEN** the client sends a POST request to `/api/admin/v1/studios` with JSON body `{"alias": "123_invalid"}`
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured JSON error object

#### Scenario: Reject duplicate studio alias
- **GIVEN** a studio with alias "valhalla" already exists in the database
- **WHEN** the client sends a POST request to `/api/admin/v1/studios` with JSON body `{"alias": "valhalla"}`
- **THEN** the system returns HTTP 409 Conflict with an AIP-193 structured JSON error object

#### Scenario: List active studios excluding soft-deleted records
- **GIVEN** one active studio and one soft-deleted studio exist in the database
- **WHEN** the client sends a GET request to `/api/admin/v1/studios`
- **THEN** the system returns HTTP 200 OK with an array containing only the active studio

#### Scenario: List studios including soft-deleted records
- **GIVEN** one active studio and one soft-deleted studio exist in the database
- **WHEN** the client sends a GET request to `/api/admin/v1/studios?include_deleted=true`
- **THEN** the system returns HTTP 200 OK with an array containing both the active and soft-deleted studios

#### Scenario: Retrieve studio by alias string
- **GIVEN** an active studio exists with alias "midgard"
- **WHEN** the client sends a GET request to `/api/admin/v1/studios/midgard`
- **THEN** the system returns HTTP 200 OK with the studio matching alias "midgard"

#### Scenario: Retrieve studio by UUID identifier
- **GIVEN** an active studio exists with a known `studioId` UUID
- **WHEN** the client sends a GET request to `/api/admin/v1/studios/{studioId}`
- **THEN** the system returns HTTP 200 OK with the studio matching that UUID
