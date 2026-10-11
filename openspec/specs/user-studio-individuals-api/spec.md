# user-studio-individuals-api Specification

## Purpose

Tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/individuals` for creating, retrieving, partially updating, and soft-deleting studio individual entities as first-class objects extending entities via Class Table Inheritance (CTI) with PostgreSQL Row-Level Security (RLS) isolation and omission of collection listing.

## Requirements

### Requirement: Create Individual Entity
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/individuals` allowing authenticated clients to create an **Individual** **Entity** under a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public studio UUID or an **Alias** string, resolving in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found without performing database operations. Upon resolving `tenant_id`, the system SHALL validate the request payload (`name` required non-blank, optional `summary`). If `name` is null, empty, or whitespace-only, the system SHALL return HTTP 400 Bad Request. If validation succeeds, the system SHALL persist the record in `njall_users.entities` (with generated UUIDv7, `tenant_id`, `entity_type = 'Individual'`, and timestamps) and `njall_users.individuals` (with `(tenant_id, id)` and `name`) using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created containing the public individual `id`, `name`, `summary`, `createdOn`, and `updatedOn`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successful creation of an individual with summary
- **GIVEN** an active **Studio** with alias "valkyrie_example" exists in `StudioLookupCache`
- **WHEN** a client sends a POST request to `/api/studios/valkyrie_example/v1/individuals` with payload `{"name": "Jane Eyre", "summary": "Visiting scholar"}`
- **THEN** the system returns HTTP 201 Created with JSON body containing generated `id`, `name = "Jane Eyre"`, `summary = "Visiting scholar"`, `createdOn`, and `updatedOn` timestamps
- **AND** the individual is persisted in `njall_users.individuals` and `njall_users.entities` linked to the studio tenant

#### Scenario: Successful creation of an individual without summary
- **GIVEN** an active **Studio** exists with alias "valkyrie_example"
- **WHEN** a client sends a POST request to `/api/studios/valkyrie_example/v1/individuals` with payload `{"name": "Edward Rochester"}`
- **THEN** the system returns HTTP 201 Created with `name = "Edward Rochester"` and null `summary`

#### Scenario: Validation failure on missing or blank name
- **GIVEN** an active **Studio** exists with alias "valkyrie_example"
- **WHEN** a client sends a POST request to `/api/studios/valkyrie_example/v1/individuals` with payload `{"name": "   "}`
- **THEN** the system returns HTTP 400 Bad Request with an error description

#### Scenario: Studio not found or soft-deleted
- **GIVEN** no active studio exists for identifier "nonexistent_studio"
- **WHEN** a client sends a POST request to `/api/studios/nonexistent_studio/v1/individuals` with payload `{"name": "Jane Eyre"}`
- **THEN** the system returns HTTP 404 Not Found directly without executing a database transaction

### Requirement: Retrieve Individual Entity by ID
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/individuals/{individual-id}` allowing clients to retrieve an **Individual** **Entity**. The `{individual-id}` path segment SHALL accept a UUID string. If `{individual-id}` is not a valid UUID format, the system SHALL return HTTP 404 Not Found. If the individual does not exist, belongs to another **Tenant**, or has a non-null `deleted_on` timestamp in `njall_users.entities`, the system SHALL return HTTP 404 Not Found. If the individual exists and is active within the resolved studio tenant, the system SHALL return HTTP 200 OK with the individual representation.

#### Scenario: Retrieve an active individual by ID
- **GIVEN** an active **Individual** with ID `01925b42-1111-7000-8000-000000000001` exists under studio "valkyrie_example"
- **WHEN** a client sends a GET request to `/api/studios/valkyrie_example/v1/individuals/01925b42-1111-7000-8000-000000000001`
- **THEN** the system returns HTTP 200 OK with the individual's `id`, `name`, `summary`, `createdOn`, and `updatedOn`

#### Scenario: Retrieve a non-existent individual
- **GIVEN** an active **Studio** exists with alias "valkyrie_example"
- **WHEN** a client sends a GET request to `/api/studios/valkyrie_example/v1/individuals/01925b42-9999-7000-8000-000000000999`
- **THEN** the system returns HTTP 404 Not Found

#### Scenario: Retrieve a soft-deleted individual
- **GIVEN** an **Individual** exists under studio "valkyrie_example" but has `deleted_on` set to a non-null timestamp
- **WHEN** a client sends a GET request to `/api/studios/valkyrie_example/v1/individuals/01925b42-1111-7000-8000-000000000001`
- **THEN** the system returns HTTP 404 Not Found

#### Scenario: Cross-tenant isolation on retrieval
- **GIVEN** an **Individual** exists under studio "ironwood_fake"
- **WHEN** a client sends a GET request to `/api/studios/valkyrie_example/v1/individuals/{that-id}`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Partial Update of Individual Entity
The system SHALL expose an HTTP PATCH endpoint at `/api/studios/{studio-id}/v1/individuals/{individual-id}` allowing partial updates to mutable fields (`name`, `summary`) adhering to Google AIP-134. The system SHALL support an optional query parameter `update_mask`. If `update_mask` is omitted, all non-null fields in the request body SHALL be applied. If `name` is specified in the update, it MUST NOT be blank; otherwise, the system SHALL return HTTP 400 Bad Request. Upon receiving a valid request for an active individual within the studio tenant boundary, the system SHALL update `njall_users.individuals` and `njall_users.entities`, set `entities.updated_on = CURRENT_TIMESTAMP`, and return HTTP 200 OK with the updated individual representation. If the individual does not exist, belongs to another tenant, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successful partial update with update_mask
- **GIVEN** an active **Individual** exists with name "Jane Eyre" and summary "Scholar"
- **WHEN** a client sends a PATCH request to `/api/studios/valkyrie_example/v1/individuals/{id}?update_mask=name` with payload `{"name": "Jane Rochester"}`
- **THEN** the system returns HTTP 200 OK with `name = "Jane Rochester"` and unchanged summary "Scholar"
- **AND** `updatedOn` is refreshed to a more recent timestamp

#### Scenario: Validation failure when updating name to blank
- **GIVEN** an active **Individual** exists under studio "valkyrie_example"
- **WHEN** a client sends a PATCH request to `/api/studios/valkyrie_example/v1/individuals/{id}` with payload `{"name": "   "}`
- **THEN** the system returns HTTP 400 Bad Request

#### Scenario: Patching a soft-deleted individual
- **GIVEN** an **Individual** has `deleted_on` set to a non-null timestamp
- **WHEN** a client sends a PATCH request to `/api/studios/valkyrie_example/v1/individuals/{id}` with payload `{"name": "Updated Name"}`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Soft Delete Individual Entity
The system SHALL expose an HTTP DELETE endpoint at `/api/studios/{studio-id}/v1/individuals/{individual-id}`. Upon invocation, the system SHALL set `entities.deleted_on = CURRENT_TIMESTAMP` for the matching `(tenant_id, id)` row via `IndividualDAO`. The row in `njall_users.individuals` and `njall_users.entities` SHALL NOT be physically removed. If the target individual exists and is active, the system SHALL return HTTP 204 No Content. If the individual does not exist or is already soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Soft delete an active individual
- **GIVEN** an active **Individual** exists under studio "valkyrie_example"
- **WHEN** a client sends a DELETE request to `/api/studios/valkyrie_example/v1/individuals/{id}`
- **THEN** the system returns HTTP 204 No Content
- **AND** the row in `njall_users.entities` has a non-null `deleted_on` timestamp

#### Scenario: Soft delete an already deleted individual
- **GIVEN** an **Individual** under studio "valkyrie_example" has a non-null `deleted_on` timestamp
- **WHEN** a client sends a DELETE request to `/api/studios/valkyrie_example/v1/individuals/{id}`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Omission of Collection List Endpoint
The system SHALL NOT expose a list endpoint on the collection path `/api/studios/{studio-id}/v1/individuals`. HTTP GET requests hitting `/api/studios/{studio-id}/v1/individuals` or `/api/studios/{studio-id}/v1/individuals/` SHALL return HTTP 404 Not Found.

#### Scenario: Attempting to list individuals on the collection route
- **GIVEN** an active **Studio** exists with alias "valkyrie_example"
- **WHEN** a client sends a GET request to `/api/studios/valkyrie_example/v1/individuals`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Database Persistence and Row-Level Security
The system SHALL apply Flyway database migration `V9__individuals.sql` provisioning the `njall_users.individuals` table. The table SHALL declare composite primary key `(tenant_id, id)` where `(tenant_id, id)` references `njall_users.entities(tenant_id, id)` with `ON DELETE CASCADE`, enforces unique constraint `unq_individuals_id` on `id`, and declares `name VARCHAR NOT NULL`. The system SHALL enable Row-Level Security (RLS) on `njall_users.individuals`. The system SHALL install RLS policy `rls_individuals` for role `njall_users` restricting rows to `tenant_id = current_setting('app.tenant_id', true)::uuid`. The system SHALL install RLS policy `rls_individuals_admin` for role `njall_admin` permitting all operations without tenant restriction. The system SHALL grant `SELECT, INSERT, UPDATE, DELETE` on `njall_users.individuals` to role `njall_users`, and grant `ALL` to role `njall_admin`.

#### Scenario: RLS boundary enforces tenant isolation under njall_users
- **GIVEN** a database session authenticated as role `njall_users` sets `app.tenant_id` to Studio A's UUID
- **WHEN** the session executes a query against `njall_users.individuals`
- **THEN** only individuals belonging to Studio A are visible, and Studio B's rows are filtered out by RLS

#### Scenario: Administrative role bypasses tenant restriction
- **GIVEN** a database session authenticated as role `njall_admin`
- **WHEN** the session executes a query against `njall_users.individuals`
- **THEN** individuals across all studios are accessible without setting `app.tenant_id`
