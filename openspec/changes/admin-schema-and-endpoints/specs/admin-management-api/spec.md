## ADDED Requirements

### Requirement: Studio Management Endpoints
The system SHALL expose HTTP REST endpoints under `/api/admin/v1/studios` allowing administrators to create, list, and retrieve studios. The `POST /api/admin/v1/studios` endpoint SHALL accept a JSON payload containing an `alias` string, generate a UUIDv7 `tenant_id` and random UUIDv4 `studio_id`, and return HTTP 201 Created with the created studio representation. The `GET /api/admin/v1/studios` endpoint SHALL return an array of active studios, excluding soft-deleted studios by default, and including soft-deleted studios when the query parameter `include_deleted=true` is present. The `GET /api/admin/v1/studios/{id}` endpoint SHALL accept either a valid `studio_id` UUID or an `alias` string, returning HTTP 200 OK with the studio representation or HTTP 404 Not Found if the studio does not exist (or is soft-deleted without `include_deleted=true`).

#### Scenario: Successfully create studio with unique alias
- **GIVEN** an administrative client with valid credentials
- **WHEN** the client sends a POST request to `/api/admin/v1/studios` with JSON body `{"alias": "valhalla"}`
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains `tenantId`, `studioId`, `alias` equal to "valhalla", `createdAt`, and `updatedAt`

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

### Requirement: Admin User Management Endpoints
The system SHALL expose HTTP REST endpoints under `/api/admin/v1/users` allowing administrators to create, list, and retrieve administrative users. The `POST /api/admin/v1/users` endpoint SHALL accept a JSON payload with `username` (required), optional `status` (defaulting to `ACTIVE`), and optional initial `roleIds` (array of role UUIDs), returning HTTP 201 Created. The `GET /api/admin/v1/users` endpoint SHALL return all admin users with their assigned roles and status. The `GET /api/admin/v1/users/{id}` endpoint SHALL accept either a user UUID or `username` string, returning HTTP 200 OK with the user and assigned roles, or HTTP 404 Not Found if missing.

#### Scenario: Create admin user with initial role assignments
- **GIVEN** an existing role with a known UUID in `admin_roles`
- **WHEN** the client sends a POST request to `/api/admin/v1/users` with body `{"username": "admin_freyja", "roleIds": ["<role-uuid>"]}`
- **THEN** the system returns HTTP 201 Created with the user representation including assigned roles

#### Scenario: Reject duplicate admin username
- **GIVEN** an admin user with username "admin_freyja" already exists
- **WHEN** the client sends a POST request to `/api/admin/v1/users` with body `{"username": "admin_freyja"}`
- **THEN** the system returns HTTP 409 Conflict with a structured JSON error object

#### Scenario: Retrieve admin user by username
- **GIVEN** an admin user with username "admin_odin" exists
- **WHEN** the client sends a GET request to `/api/admin/v1/users/admin_odin`
- **THEN** the system returns HTTP 200 OK with the user record and assigned roles

### Requirement: Custom Role Assignment Endpoints
The system SHALL expose AIP-136 custom method endpoints `/api/admin/v1/users/{id}:addRole` and `/api/admin/v1/users/{id}:removeRole`. Both endpoints SHALL accept either `roleId` (UUID) or `roleName` (string) in the request payload. The system SHALL idempotently assign or unassign the role, returning HTTP 200 OK with the updated admin user representation. If the referenced role does not exist, the system SHALL return HTTP 400 Bad Request with a structured error object.

#### Scenario: Add role to user by roleName idempotently
- **GIVEN** an admin user and an existing role named "SECURITY_ADMIN"
- **WHEN** the client sends a POST request to `/api/admin/v1/users/{userId}:addRole` with body `{"roleName": "SECURITY_ADMIN"}`
- **THEN** the system returns HTTP 200 OK containing the updated user with "SECURITY_ADMIN" assigned
- **AND** a subsequent identical request returns HTTP 200 OK without duplicate assignment

#### Scenario: Remove role from user by roleId idempotently
- **GIVEN** an admin user assigned to role with UUID `<role-id>`
- **WHEN** the client sends a POST request to `/api/admin/v1/users/{userId}:removeRole` with body `{"roleId": "<role-id>"}`
- **THEN** the system returns HTTP 200 OK with the role removed from the user
- **AND** a subsequent identical request returns HTTP 200 OK

#### Scenario: Reject role assignment for non-existent role
- **GIVEN** an existing admin user
- **WHEN** the client sends a POST request to `/api/admin/v1/users/{userId}:addRole` with body `{"roleName": "NON_EXISTENT_ROLE"}`
- **THEN** the system returns HTTP 400 Bad Request with an error explaining the role does not exist

### Requirement: Role Management Endpoints
The system SHALL expose HTTP REST endpoints under `/api/admin/v1/roles` allowing administrators to create, list, and retrieve roles. The `POST /api/admin/v1/roles` endpoint SHALL accept a JSON payload with `roleName` string, generate a UUIDv7 identifier, and return HTTP 201 Created. The `GET /api/admin/v1/roles` endpoint SHALL return all registered roles. The `GET /api/admin/v1/roles/{id}` endpoint SHALL accept either role UUID or `roleName` string, returning HTTP 200 OK or HTTP 404 Not Found.

#### Scenario: Create role with unique roleName
- **GIVEN** an administrative client
- **WHEN** the client sends a POST request to `/api/admin/v1/roles` with body `{"roleName": "AUDITOR"}`
- **THEN** the system returns HTTP 201 Created with the generated role UUID and roleName

#### Scenario: Reject duplicate role creation
- **GIVEN** a role named "AUDITOR" already exists
- **WHEN** the client sends a POST request to `/api/admin/v1/roles` with body `{"roleName": "AUDITOR"}`
- **THEN** the system returns HTTP 409 Conflict with a structured JSON error object

#### Scenario: Retrieve role by roleName string
- **GIVEN** a role named "AUDITOR" exists
- **WHEN** the client sends a GET request to `/api/admin/v1/roles/AUDITOR`
- **THEN** the system returns HTTP 200 OK with the role representation
