## MODIFIED Requirements

### Requirement: Create Individual Entity
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/individuals` allowing authenticated clients to create an **Individual** **Entity** under a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public studio UUID or an **Alias** string, resolving in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found without performing database operations. Upon resolving `tenant_id`, the system SHALL validate the request payload (`name` required non-blank, `generator` required string accepting either a bare **Actor** UUID or a canonical studio **Actor** URI `/api/studios/{studio-id}/v1/actors/{actor-id}` matching `{studio-id}`, and optional `summary`). If `name` is null, empty, or whitespace-only, or if `generator` is missing, blank, malformed, references another studio, or does not reference an existing, active **Actor** **Entity** (`entity_type = 'Actor'`) in the same studio **Tenant**, the system SHALL return HTTP 400 Bad Request. If validation succeeds, the system SHALL persist the record in `njall_users.entities` (with generated UUIDv7, `tenant_id`, `owner` set to the resolved `generator` **Actor** UUID, `entity_type = 'Individual'`, and timestamps) and `njall_users.individuals` (with `(tenant_id, id)` and `name`) using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created containing the public individual `id`, `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{owner-id}"`, `name`, `summary`, `createdOn`, and `updatedOn`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successful creation of an individual with summary
- **GIVEN** an active **Studio** with alias "valkyrie_example" exists in `StudioLookupCache` and an active **Actor** exists with UUID "01925b42-0000-7000-8000-000000000001"
- **WHEN** a client sends a POST request to `/api/studios/valkyrie_example/v1/individuals` with payload `{"generator": "01925b42-0000-7000-8000-000000000001", "name": "Jane Eyre", "summary": "Visiting scholar"}`
- **THEN** the system returns HTTP 201 Created with JSON body containing generated `id`, `generator = "/api/studios/valkyrie_example/v1/actors/01925b42-0000-7000-8000-000000000001"`, `name = "Jane Eyre"`, `summary = "Visiting scholar"`, `createdOn`, and `updatedOn` timestamps
- **AND** the individual is persisted in `njall_users.individuals` and `njall_users.entities` linked to the studio tenant and actor owner

#### Scenario: Successful creation of an individual without summary
- **GIVEN** an active **Studio** exists with alias "valkyrie_example" and an active **Actor** exists with UUID "01925b42-0000-7000-8000-000000000001"
- **WHEN** a client sends a POST request to `/api/studios/valkyrie_example/v1/individuals` with payload `{"generator": "/api/studios/valkyrie_example/v1/actors/01925b42-0000-7000-8000-000000000001", "name": "Edward Rochester"}`
- **THEN** the system returns HTTP 201 Created with `name = "Edward Rochester"`, `generator = "/api/studios/valkyrie_example/v1/actors/01925b42-0000-7000-8000-000000000001"`, and null `summary`

#### Scenario: Validation failure on missing or invalid generator
- **GIVEN** an active **Studio** exists with alias "valkyrie_example"
- **WHEN** a client sends a POST request to `/api/studios/valkyrie_example/v1/individuals` omitting `generator` or referencing a non-Actor entity
- **THEN** the system returns HTTP 400 Bad Request with an error description

#### Scenario: Validation failure on missing or blank name
- **GIVEN** an active **Studio** exists with alias "valkyrie_example" and an active **Actor** exists with UUID "01925b42-0000-7000-8000-000000000001"
- **WHEN** a client sends a POST request to `/api/studios/valkyrie_example/v1/individuals` with payload `{"generator": "01925b42-0000-7000-8000-000000000001", "name": "   "}`
- **THEN** the system returns HTTP 400 Bad Request with an error description

#### Scenario: Studio not found or soft-deleted
- **GIVEN** no active studio exists for identifier "nonexistent_studio"
- **WHEN** a client sends a POST request to `/api/studios/nonexistent_studio/v1/individuals` with payload `{"generator": "01925b42-0000-7000-8000-000000000001", "name": "Jane Eyre"}`
- **THEN** the system returns HTTP 404 Not Found directly without executing a database transaction

### Requirement: Retrieve Individual Entity by ID
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/individuals/{individual-id}` allowing clients to retrieve an **Individual** **Entity**. The `{individual-id}` path segment SHALL accept a UUID string. If `{individual-id}` is not a valid UUID format, the system SHALL return HTTP 404 Not Found. If the individual does not exist, belongs to another **Tenant**, or has a non-null `deleted_on` timestamp in `njall_users.entities`, the system SHALL return HTTP 404 Not Found. If the individual exists and is active within the resolved studio tenant, the system SHALL return HTTP 200 OK with the individual representation including `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{owner-id}"`.

#### Scenario: Retrieve an active individual by ID
- **GIVEN** an active **Individual** with ID `01925b42-1111-7000-8000-000000000001` generated by actor `01925b42-0000-7000-8000-000000000001` exists under studio "valkyrie_example"
- **WHEN** a client sends a GET request to `/api/studios/valkyrie_example/v1/individuals/01925b42-1111-7000-8000-000000000001`
- **THEN** the system returns HTTP 200 OK with the individual's `id`, `generator` equal to `"/api/studios/valkyrie_example/v1/actors/01925b42-0000-7000-8000-000000000001"`, `name`, `summary`, `createdOn`, and `updatedOn`

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
