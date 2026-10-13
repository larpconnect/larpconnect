# user-studio-actors-api Specification

## Purpose

Tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/actors` for creating, retrieving, updating, soft-deleting, managing endpoints, and interacting with inbox and outbox collections for studio **Actor** identities using Common Table Inheritance (CTI) and PostgreSQL Row-Level Security (RLS).

## Requirements

### Requirement: Create Actor Entity with Provisioned Inbox and Outbox

The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/actors` allowing authenticated clients to create an **Actor** **Entity** under a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public studio UUID or an **Alias** string, resolving in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found without performing database operations.

Upon resolving `tenant_id`, the system SHALL validate the request payload:
- `ownerId`: Required UUID referencing an active, non-deleted **Entity** belonging to the same studio **Tenant**.
- `preferredUsername`: Required non-blank string.
- `summary`: Optional string.

If `preferredUsername` is blank or whitespace-only, or `ownerId` is null, the system SHALL return HTTP 400 Bad Request. If `ownerId` does not reference an existing, active **Entity** in the same studio **Tenant**, the system SHALL return HTTP 400 Bad Request.

If another active or soft-deleted **Actor** in the same studio **Tenant** already has the same `preferredUsername` (compared case-insensitively via `lower(preferred_username)`), the system SHALL return HTTP 409 Conflict.

If validation succeeds, the system SHALL persist the **Actor** within a single database transaction:
1. Insert root **Entity** into `njall_users.entities` with generated UUIDv7, `tenant_id`, `entity_type = 'Actor'`, `summary`, and timestamps.
2. Insert subtype record into `njall_users.actors` with `(tenant_id, id)`, `owner_id`, and `preferred_username`.
3. Provision an `inbox` **Collections** entity in `njall_users.collections` (with `owner_id = actor.id`, `name = 'inbox'`, and matching `entities` record).
4. Provision an `outbox` **Collections** entity in `njall_users.collections` (with `owner_id = actor.id`, `name = 'outbox'`, and matching `entities` record).

The endpoint SHALL return HTTP 201 Created containing the public **Actor** `id`, `type = "Actor"`, `ownerId`, `preferredUsername`, `summary`, canonical `inbox` URL, canonical `outbox` URL, empty `endpoints` map, `createdOn`, and `updatedOn`. The response SHALL NOT expose internal `tenant_id`.

#### Scenario: Successfully create actor with inbox and outbox
- **GIVEN** an active studio "mystic-realms" and an active individual entity with UUID "019273c5-8f12-7000-8000-000000000001"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors" with body:
  ```json
  {
    "ownerId": "019273c5-8f12-7000-8000-000000000001",
    "preferredUsername": "valkyrie_lead",
    "summary": "Lead singer of Valkyrie"
  }
  ```
- **THEN** system responds with HTTP 201 Created
- **AND** response body contains "id", "type": "Actor", "preferredUsername": "valkyrie_lead", "ownerId": "019273c5-8f12-7000-8000-000000000001"
- **AND** response body contains "inbox" URL ending in "/inbox" and "outbox" URL ending in "/outbox"
- **AND** response body contains "endpoints": {}

#### Scenario: Reject duplicate preferred username within same tenant
- **GIVEN** an active studio "mystic-realms" containing an existing actor with preferred username "valkyrie_lead"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors" with body containing "preferredUsername": "VALKYRIE_LEAD"
- **THEN** system responds with HTTP 409 Conflict

#### Scenario: Reject missing or soft-deleted owner entity
- **GIVEN** an active studio "mystic-realms"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors" with an owner ID that does not exist or has "deleted_on" set
- **THEN** system responds with HTTP 400 Bad Request

#### Scenario: Reject empty or whitespace preferred username
- **GIVEN** an active studio "mystic-realms"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors" with "preferredUsername": "   "
- **THEN** system responds with HTTP 400 Bad Request

---

### Requirement: Retrieve Actor Detail by Identifier

The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}`. If `{studio-id}` or `{actor-id}` does not exist or has `deleted_on IS NOT NULL`, the system SHALL return HTTP 404 Not Found. If active, the system SHALL return HTTP 200 OK containing the **Actor** representation, including active federated protocol endpoints in an `endpoints` map of name-to-URI strings. The system SHALL NOT provide a list endpoint at `/api/studios/{studio-id}/v1/actors`, returning HTTP 404 Not Found for GET requests to the collection root.

#### Scenario: Successfully retrieve active actor by UUID
- **GIVEN** an active studio "mystic-realms" and an active actor with UUID "019273c5-9b2f-763d-82d8-21d1297e55a1" having endpoint "sharedInbox" = "https://fed.example.com/inbox"
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/019273c5-9b2f-763d-82d8-21d1297e55a1"
- **THEN** system responds with HTTP 200 OK
- **AND** response body contains "id": "019273c5-9b2f-763d-82d8-21d1297e55a1"
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

Permitted update paths SHALL be `preferredUsername` and `summary`. If `preferredUsername` is updated, the system SHALL validate non-blank characters and case-insensitive uniqueness within the **Tenant**, returning HTTP 409 Conflict if taken by another **Actor**. Modifying `ownerId` SHALL NOT be supported. Upon successful update, the system SHALL update `updated_on` to `CURRENT_TIMESTAMP` and return HTTP 200 OK with the updated **Actor** representation.

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
- **AND** response body reflects the updated summary and preferred username

#### Scenario: Reject update with conflicting preferred username
- **GIVEN** an active studio "mystic-realms" containing actors "actor_one" and "actor_two"
- **WHEN** client sends PATCH to update "actor_two" with "preferredUsername": "actor_one"
- **THEN** system responds with HTTP 409 Conflict

#### Scenario: Return not found when patching non-existent actor
- **GIVEN** an active studio "mystic-realms"
- **WHEN** client sends PATCH to "/api/studios/mystic-realms/v1/actors/019273c5-ffff-7000-8000-000000000000"
- **THEN** system responds with HTTP 404 Not Found

---

### Requirement: Soft Delete Actor Entity and Associated Collections

The system SHALL expose an HTTP DELETE endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}`. If `{actor-id}` does not exist or has already been soft-deleted, the system SHALL return HTTP 404 Not Found.

Upon deletion, the system SHALL set `deleted_on = CURRENT_TIMESTAMP` on the **Actor** record in `njall_users.entities`, and cascade soft deletion to the paired `inbox` and `outbox` records in `njall_users.entities`. The system SHALL return HTTP 204 No Content.

#### Scenario: Successfully soft-delete actor and cascade to inbox and outbox
- **GIVEN** an active studio "mystic-realms" and an active actor
- **WHEN** client sends DELETE to "/api/studios/mystic-realms/v1/actors/{actor-id}"
- **THEN** system responds with HTTP 204 No Content
- **AND** subsequent GET requests to the actor, its inbox, or its outbox return HTTP 404 Not Found

#### Scenario: Return not found when deleting already deleted actor
- **GIVEN** an active studio "mystic-realms" and an actor already soft-deleted
- **WHEN** client sends DELETE to "/api/studios/mystic-realms/v1/actors/{actor-id}"
- **THEN** system responds with HTTP 404 Not Found

---

### Requirement: Retrieve Actor Inbox and Outbox Collections

The system SHALL expose HTTP GET endpoints at `/api/studios/{studio-id}/v1/actors/{actor-id}/inbox` and `/api/studios/{studio-id}/v1/actors/{actor-id}/outbox`. If the **Actor** is missing or soft-deleted, the system SHALL return HTTP 404 Not Found.

When active, the system SHALL retrieve the respective **Collections** entity owned by `{actor-id}` (named `'inbox'` or `'outbox'`), resolve hydrated item representations via polymorphic entity lookups, and return HTTP 200 OK containing an expanded `OrderedCollection` response payload (`type = "OrderedCollection"`, `totalItems`, and `orderedItems` array).

#### Scenario: Successfully retrieve expanded inbox collection
- **GIVEN** an active studio "mystic-realms" and an active actor with an inbox collection
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/{actor-id}/inbox"
- **THEN** system responds with HTTP 200 OK
- **AND** response body contains "type": "OrderedCollection", "name": "inbox", "orderedItems": []

#### Scenario: Successfully retrieve expanded outbox collection
- **GIVEN** an active studio "mystic-realms" and an active actor with an outbox collection
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/{actor-id}/outbox"
- **THEN** system responds with HTTP 200 OK
- **AND** response body contains "type": "OrderedCollection", "name": "outbox", "orderedItems": []

#### Scenario: Return not found when retrieving collections for non-existent actor
- **GIVEN** an active studio "mystic-realms"
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/019273c5-ffff-7000-8000-000000000000/inbox"
- **THEN** system responds with HTTP 404 Not Found

---

### Requirement: Post Entity Payloads to Actor Inbox and Outbox

The system SHALL expose HTTP POST endpoints at `/api/studios/{studio-id}/v1/actors/{actor-id}/inbox` and `/api/studios/{studio-id}/v1/actors/{actor-id}/outbox`. If the **Actor** does not exist or has been soft-deleted, the system SHALL return HTTP 404 Not Found.

The endpoints SHALL accept an arbitrary JSON object payload in the request body. The system SHALL accept the JSON payload and return HTTP 201 Created.

#### Scenario: Successfully post JSON payload to actor inbox
- **GIVEN** an active studio "mystic-realms" and an active actor
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors/{actor-id}/inbox" with body:
  ```json
  {
    "type": "Create",
    "summary": "Notification of incoming interaction"
  }
  ```
- **THEN** system responds with HTTP 201 Created

#### Scenario: Successfully post JSON payload to actor outbox
- **GIVEN** an active studio "mystic-realms" and an active actor
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors/{actor-id}/outbox" with body:
  ```json
  {
    "type": "Note",
    "content": "Published message"
  }
  ```
- **THEN** system responds with HTTP 201 Created

#### Scenario: Return not found when posting to non-existent actor
- **GIVEN** an active studio "mystic-realms"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors/019273c5-ffff-7000-8000-000000000000/outbox" with a JSON payload
- **THEN** system responds with HTTP 404 Not Found

---

### Requirement: Add or Upsert Federated Actor Endpoint

The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}:addEndpoint` adhering to Google AIP-136 custom method conventions. If `{actor-id}` does not exist or has been soft-deleted, the system SHALL return HTTP 404 Not Found.

The request payload SHALL require:
- `name`: Required alphanumeric string with length strictly less than 32 characters (`^[a-zA-Z0-9]{1,31}$`).
- `endpoint`: Required non-blank URI string.

If `name` contains non-alphanumeric characters, has length >= 32, or is blank, or `endpoint` is blank, the system SHALL return HTTP 400 Bad Request.

If an endpoint with the specified `name` already exists for the **Actor**, the system SHALL overwrite the existing record with the new `endpoint` value (upsert semantics). The system SHALL return HTTP 204 No Content.

#### Scenario: Successfully add new endpoint to actor
- **GIVEN** an active studio "mystic-realms" and an active actor
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors/{actor-id}:addEndpoint" with body:
  ```json
  {
    "name": "sharedInbox",
    "endpoint": "https://fed.example.com/sharedInbox"
  }
  ```
- **THEN** system responds with HTTP 204 No Content
- **AND** subsequent GET to "/api/studios/mystic-realms/v1/actors/{actor-id}" contains "sharedInbox" in "endpoints"

#### Scenario: Successfully overwrite existing endpoint with same name
- **GIVEN** an active studio "mystic-realms" and an actor with existing endpoint "sharedInbox" = "https://old.example.com/inbox"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors/{actor-id}:addEndpoint" with body:
  ```json
  {
    "name": "sharedInbox",
    "endpoint": "https://new.example.com/inbox"
  }
  ```
- **THEN** system responds with HTTP 204 No Content
- **AND** subsequent GET to "/api/studios/mystic-realms/v1/actors/{actor-id}" reflects the updated URL "https://new.example.com/inbox"

#### Scenario: Reject endpoint with invalid name format or excessive length
- **GIVEN** an active studio "mystic-realms" and an active actor
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors/{actor-id}:addEndpoint" with name "invalid-name-with-hyphens" or a name of 32 characters
- **THEN** system responds with HTTP 400 Bad Request

---

### Requirement: Remove Federated Actor Endpoint

The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}:removeEndpoint` adhering to Google AIP-136 custom method conventions. If `{actor-id}` does not exist or has been soft-deleted, the system SHALL return HTTP 404 Not Found.

The request payload SHALL require `name` (non-blank string). If `name` is blank or null, the system SHALL return HTTP 400 Bad Request.

If an endpoint with the specified `name` exists for the **Actor**, the system SHALL delete the record from `njall_users.actor_endpoints`. If no endpoint exists with that `name`, the operation SHALL complete idempotently. In all successful cases, the system SHALL return HTTP 204 No Content.

#### Scenario: Successfully remove existing endpoint from actor
- **GIVEN** an active studio "mystic-realms" and an actor with existing endpoint "sharedInbox"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors/{actor-id}:removeEndpoint" with body:
  ```json
  {
    "name": "sharedInbox"
  }
  ```
- **THEN** system responds with HTTP 204 No Content
- **AND** subsequent GET to "/api/studios/mystic-realms/v1/actors/{actor-id}" does not contain "sharedInbox" in "endpoints"

#### Scenario: Return no content when removing non-existent endpoint idempotently
- **GIVEN** an active studio "mystic-realms" and an active actor without endpoint "nonExistentEndpoint"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors/{actor-id}:removeEndpoint" with body:
  ```json
  {
    "name": "nonExistentEndpoint"
  }
  ```
- **THEN** system responds with HTTP 204 No Content

---

### Requirement: Enforce Tenancy and Row-Level Security on Actors and Endpoints

The system SHALL enforce PostgreSQL Row-Level Security on `njall_users.actors` and `njall_users.actor_endpoints` using policy `tenant_id = current_setting('app.tenant_id', true)::uuid` for the `njall_users` role. The system SHALL define bypass policies for the `njall_admin` role. All queries executed by **AgentDAO** SHALL execute under an active `@NjallUsers` session configured with the requesting studio's `tenant_id`.

#### Scenario: Prevent cross-tenant actor access under row-level security
- **GIVEN** studio "alpha" with tenant UUID "019273c5-aaaa-7000-8000-000000000000" and studio "beta" with tenant UUID "019273c5-bbbb-7000-8000-000000000000"
- **AND** an actor created under studio "alpha"
- **WHEN** a database session configured with studio "beta" tenant context attempts to query or mutate the actor
- **THEN** PostgreSQL Row-Level Security filters out the record, returning zero rows

#### Scenario: Admin role bypasses row-level security
- **GIVEN** an actor created under any studio tenant
- **WHEN** an administrative database session executing as role "njall_admin" queries "njall_users.actors"
- **THEN** the administrative session successfully accesses the actor across tenants
