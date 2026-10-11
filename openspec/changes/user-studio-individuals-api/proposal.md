## Why

In LarpConnect (Njall), a **Studio** needs to track individual participants, players, and non-player character performers who may attend events or interact with the studio without necessarily possessing a registered user account. Introducing the **Individual** entity establishes this core person concept within the single-tenant user API layer under `/api/studios/{studio-id}/v1/individuals`, supported by Class Table Inheritance and PostgreSQL Row-Level Security.

## What Changes

- Add the `njall_users.individuals` table via Flyway migration `V9__individuals.sql`, subordinate to `njall_users.entities` using Class Table Inheritance (CTI) with `(tenant_id, id)` composite primary key, `ON DELETE CASCADE`, unique constraint on `id`, Row-Level Security (RLS) policies for roles `njall_users` and `njall_admin`, and corresponding role permissions.
- Provide data layer mappings in `:data` including JPA `IndividualEntity`, immutable domain record `Individual`, and `IndividualDAO` / `DefaultIndividualDAO` operating under `@NjallUsers` session factories with tenant isolation (`app.tenant_id`).
- Expose single-tenant HTTP REST endpoints under `/api/studios/{studio-id}/v1/individuals`:
  - `POST /api/studios/{studio-id}/v1/individuals`: Creates an individual entity with server-generated UUIDv7, validating non-blank `name` and optional `summary`. Returns HTTP 201 Created.
  - `GET /api/studios/{studio-id}/v1/individuals/{individual-id}`: Retrieves an active individual by ID. Returns HTTP 200 OK or HTTP 404 Not Found if missing or soft-deleted.
  - `PATCH /api/studios/{studio-id}/v1/individuals/{individual-id}`: Applies partial updates (`name`, `summary`) adhering to Google AIP-134 with optional `update_mask`. Returns HTTP 200 OK or HTTP 404 Not Found.
  - `DELETE /api/studios/{studio-id}/v1/individuals/{individual-id}`: Performs soft deletion by marking `entities.deleted_on = CURRENT_TIMESTAMP`. Returns HTTP 204 No Content or HTTP 404 Not Found if missing or already soft-deleted.
  - Intentionally omit collection `GET` (list functionality is not supported for individuals).
- Implement Pekko Typed actor `IndividualActor` and HTTP route `IndividualsRoute` in `:api` mounted into `StudiosRoute`.
- Update the OpenAPI specification (`api/src/main/resources/openapi.yaml`) with `/api/studios/{studio-id}/v1/individuals` endpoints and schemas.
- Add Cucumber integration features in `:integration` verifying route validation, tenant isolation, and data persistence boundaries.

## Capabilities

### New Capabilities
- `user-studio-individuals-api`: Defines the HTTP API contracts, actor commands, and data layer behaviors for creating, retrieving, partially updating, and soft-deleting studio **Individual** entities without list access.

### Modified Capabilities
<!-- No existing capabilities require behavioral modification. -->

## Impact

- **Database**: Adds table `njall_users.individuals` in new migration `V9__individuals.sql`.
- **APIs**: Extends `/api/studios/{studio-id}/v1` with new `individuals` endpoints in `openapi.yaml`.
- **Backend Components**:
  - `:data`: Adds `Individual`, `IndividualEntity`, `IndividualDAO`, `DefaultIndividualDAO`, and registers them in `StudiosDaoModule`.
  - `:api`: Adds `IndividualActor`, `IndividualCommand`, `IndividualActorResponse`, `IndividualsRoute`, `IndividualsModule`, and mounts route into `StudiosRoute`.
- **Dependencies**: No new external dependencies required; uses existing Pekko, Hibernate, Jackson, and Guice infrastructure.
