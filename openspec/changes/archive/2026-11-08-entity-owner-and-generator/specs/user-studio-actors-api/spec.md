## MODIFIED Requirements

### Requirement: Create Actor Entity with Provisioned Inbox and Outbox

The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/actors` allowing authenticated clients to create an **Actor** **Entity** under a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public studio UUID or an **Alias** string, resolving in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found without performing database operations.

Upon resolving `tenant_id`, the system SHALL validate the request payload:
- `generator`: Optional string accepting either a bare **Actor** UUID or a canonical studio **Actor** URI (`/api/studios/{studio-id}/v1/actors/{actor-id}` matching the target `{studio-id}`) referencing an active, non-deleted **Actor** **Entity** (`entity_type = 'Actor'`) belonging to the same studio **Tenant**. When `generator` is omitted or null, the newly created **Actor** SHALL default to self-ownership (`entities.owner = actor.id`).
- `preferredUsername`: Required non-blank string.
- `summary`: Optional string.

If `preferredUsername` is blank or whitespace-only, the system SHALL return HTTP 400 Bad Request. If `generator` is provided and is malformed, references a different studio URI, or does not reference an existing, active **Actor** **Entity** (`entity_type = 'Actor'`) in the same studio **Tenant**, the system SHALL return HTTP 400 Bad Request.

If another active or soft-deleted **Actor** in the same studio **Tenant** already has the same `preferredUsername` (compared case-insensitively via `lower(preferred_username)`), the system SHALL return HTTP 409 Conflict.

If validation succeeds, the system SHALL persist the **Actor** within a single database transaction:
1. Insert root **Entity** into `njall_users.entities` with generated UUIDv7, `tenant_id`, `owner` (resolved `generator` UUID or self `id`), `entity_type = 'Actor'`, `summary`, and timestamps.
2. Insert subtype record into `njall_users.actors` with `(tenant_id, id)` and `preferred_username`.
3. Provision an `inbox` **Collections** entity in `njall_users.collections` (with `owner_id = actor.id`, `name = 'inbox'`, and matching `entities` record with `owner = actor.id`).
4. Provision an `outbox` **Collections** entity in `njall_users.collections` (with `owner_id = actor.id`, `name = 'outbox'`, and matching `entities` record with `owner = actor.id`).

The endpoint SHALL return HTTP 201 Created containing the public **Actor** `id`, `type = "Actor"`, `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{owner-id}"`, `preferredUsername`, `summary`, canonical `inbox` URL, canonical `outbox` URL, empty `endpoints` map, `createdOn`, and `updatedOn`. The response SHALL NOT expose internal `tenant_id`.

#### Scenario: Successfully create actor with inbox and outbox
- **GIVEN** an active studio "mystic-realms" and an active actor entity with UUID "019273c5-8f12-7000-8000-000000000001"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors" with body:
  ```json
  {
    "generator": "019273c5-8f12-7000-8000-000000000001",
    "preferredUsername": "valkyrie_lead",
    "summary": "Lead singer of Valkyrie"
  }
  ```
- **THEN** system responds with HTTP 201 Created
- **AND** response body contains "id", "type": "Actor", "preferredUsername": "valkyrie_lead", "generator": "/api/studios/mystic-realms/v1/actors/019273c5-8f12-7000-8000-000000000001"
- **AND** response body contains "inbox" URL ending in "/inbox" and "outbox" URL ending in "/outbox"
- **AND** response body contains "endpoints": {}

#### Scenario: Successfully create self-owned actor when generator is omitted
- **GIVEN** an active studio "mystic-realms"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors" with body:
  ```json
  {
    "preferredUsername": "valkyrie_self",
    "summary": "Self-owned studio actor"
  }
  ```
- **THEN** system responds with HTTP 201 Created
- **AND** response body contains "id", "type": "Actor", "preferredUsername": "valkyrie_self", and "generator" equal to "/api/studios/mystic-realms/v1/actors/{id}"

#### Scenario: Reject duplicate preferred username within same tenant
- **GIVEN** an active studio "mystic-realms" containing an existing actor with preferred username "valkyrie_lead"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors" with body containing "preferredUsername": "VALKYRIE_LEAD"
- **THEN** system responds with HTTP 409 Conflict

#### Scenario: Reject missing or soft-deleted owner entity
- **GIVEN** an active studio "mystic-realms"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors" with a "generator" referencing an entity that does not exist, has "deleted_on" set, or is not an Actor
- **THEN** system responds with HTTP 400 Bad Request

#### Scenario: Reject empty or whitespace preferred username
- **GIVEN** an active studio "mystic-realms"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors" with "preferredUsername": "   "
- **THEN** system responds with HTTP 400 Bad Request

---

### Requirement: Retrieve Actor Detail by Identifier

The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}`. If `{studio-id}` or `{actor-id}` does not exist or has `deleted_on IS NOT NULL`, the system SHALL return HTTP 404 Not Found. If active, the system SHALL return HTTP 200 OK containing the **Actor** representation, including `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{owner-id}"` and active federated protocol endpoints in an `endpoints` map of name-to-URI strings. The system SHALL NOT provide a list endpoint at `/api/studios/{studio-id}/v1/actors`, returning HTTP 404 Not Found for GET requests to the collection root.

#### Scenario: Successfully retrieve active actor by UUID
- **GIVEN** an active studio "mystic-realms" and an active self-owned actor with UUID "019273c5-9b2f-763d-82d8-21d1297e55a1" having endpoint "sharedInbox" = "https://fed.example.com/inbox"
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/019273c5-9b2f-763d-82d8-21d1297e55a1"
- **THEN** system responds with HTTP 200 OK
- **AND** response body contains "id": "019273c5-9b2f-763d-82d8-21d1297e55a1"
- **AND** response body contains "generator": "/api/studios/mystic-realms/v1/actors/019273c5-9b2f-763d-82d8-21d1297e55a1"
- **AND** response body contains "endpoints" map with "sharedInbox": "https://fed.example.com/inbox"

#### Scenario: Return not found for non-existent or soft-deleted actor
- **GIVEN** an active studio "mystic-realms" and an actor that has been soft-deleted
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/{actor-id}"
- **THEN** system responds with HTTP 404 Not Found

#### Scenario: Return not found for unknown or soft-deleted studio
- **GIVEN** a studio ID that does not exist in "StudioLookupCache"
- **WHEN** client sends GET to "/api/studios/unknown-studio/v1/actors/019273c5-9b2f-763d-82d8-21d1297e55a1"
- **THEN** system responds with HTTP 404 Not Found without performing database queries

---

### Requirement: Partial Update of Actor Properties

The system SHALL expose an HTTP PATCH endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}` accepting an optional `update_mask` query parameter adhering to Google AIP-134. If `{actor-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found.

Permitted update paths SHALL be `preferredUsername` and `summary`. If `preferredUsername` is updated, the system SHALL validate non-blank characters and case-insensitive uniqueness within the **Tenant**, returning HTTP 409 Conflict if taken by another **Actor**. Modifying `generator` (`entities.owner`) SHALL NOT be supported, as entity provenance is strictly immutable. Upon successful update, the system SHALL update `updated_on` to `CURRENT_TIMESTAMP` and return HTTP 200 OK with the updated **Actor** representation including `generator`.

#### Scenario: Successfully update actor summary and username via update mask
- **GIVEN** an active studio "mystic-realms" and an active actor with preferred username "valkyrie_lead"
- **WHEN** client sends PATCH to "/api/studios/mystic-realms/v1/actors/{actor-id}?update_mask=summary,preferredUsername" with body:
  ```json
  {
    "summary": "Updated bio text",
    "preferredUsername": "valkyrie_commander"
  }
  ```
- **THEN** system responds with HTTP 200 OK
- **AND** response body reflects the updated summary and preferred username while preserving "generator"

#### Scenario: Reject update with conflicting preferred username
- **GIVEN** an active studio "mystic-realms" containing actors "actor_one" and "actor_two"
- **WHEN** client sends PATCH to update "actor_two" with "preferredUsername": "actor_one"
- **THEN** system responds with HTTP 409 Conflict

#### Scenario: Return not found when patching non-existent actor
- **GIVEN** an active studio "mystic-realms"
- **WHEN** client sends PATCH to "/api/studios/mystic-realms/v1/actors/019273c5-ffff-7000-8000-000000000000"
- **THEN** system responds with HTTP 404 Not Found
