## MODIFIED Requirements

### Requirement: Retrieve Actor Detail by Identifier
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/actors/{actor-id}`
accepting either a canonical **Actor** UUID or a case-insensitive `preferredUsername` in
`{actor-id}`. If `{studio-id}` or `{actor-id}` does not exist or has `deleted_on IS NOT NULL`, the
system SHALL return HTTP 404 Not Found. If active, the system SHALL return HTTP 200 OK containing
the **Actor** representation, including `generator` formatted as
`"/api/studios/{studio-id}/v1/actors/{generator-id}"` and active federated protocol endpoints in an
`endpoints` map of name-to-URI strings. The system SHALL NOT provide a list endpoint at
`/api/studios/{studio-id}/v1/actors`, returning HTTP 404 Not Found for GET requests to the
collection root.

#### Scenario: Successfully retrieve active actor by UUID
- **GIVEN** an active studio "mystic-realms" and an active self-generated actor with UUID
  "019273c5-9b2f-763d-82d8-21d1297e55a1" having endpoint "sharedInbox" =
  "https://fed.example.com/inbox"
- **WHEN** client sends GET to
  "/api/studios/mystic-realms/v1/actors/019273c5-9b2f-763d-82d8-21d1297e55a1"
- **THEN** system responds with HTTP 200 OK
- **AND** response body contains "id": "019273c5-9b2f-763d-82d8-21d1297e55a1"
- **AND** response body contains "generator":
  "/api/studios/mystic-realms/v1/actors/019273c5-9b2f-763d-82d8-21d1297e55a1"
- **AND** response body contains "endpoints" map with "sharedInbox":
  "https://fed.example.com/inbox"

#### Scenario: Successfully retrieve active actor by case-insensitive preferredUsername
- **GIVEN** an active studio "mystic-realms" and an active actor with UUID
  "019273c5-9b2f-763d-82d8-21d1297e55a1" and preferred username "valkyrie_lead"
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/VALKYRIE_LEAD"
- **THEN** system responds with HTTP 200 OK
- **AND** response body contains "id": "019273c5-9b2f-763d-82d8-21d1297e55a1" and
  "preferredUsername": "valkyrie_lead"

#### Scenario: Return not found for non-existent or soft-deleted actor
- **GIVEN** an active studio "mystic-realms" and an actor that has been soft-deleted
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/{actor-id}"
- **THEN** system responds with HTTP 404 Not Found

#### Scenario: Return not found for unknown or soft-deleted studio
- **GIVEN** a studio ID that does not exist in "StudioLookupCache"
- **WHEN** client sends GET to
  "/api/studios/unknown-studio/v1/actors/019273c5-9b2f-763d-82d8-21d1297e55a1"
- **THEN** system responds with HTTP 404 Not Found without performing database queries

### Requirement: Retrieve Actor Inbox and Outbox Collections
The system SHALL expose HTTP GET endpoints at
`/api/studios/{studio-id}/v1/actors/{actor-id}/inbox` (accepting `{actor-id}` by UUID or
case-insensitive `preferredUsername`) and `/api/studios/{studio-id}/v1/actors/{actor-id}/outbox`. If
the **Actor** is missing or soft-deleted, the system SHALL return HTTP 404 Not Found.

When active, the system SHALL retrieve the respective **Collections** entity owned by `{actor-id}`
(named `'inbox'` or `'outbox'`), resolve hydrated item representations via polymorphic entity
lookups, and return HTTP 200 OK containing an expanded `OrderedCollection` response payload
(`type = "OrderedCollection"`, `totalItems`, and `orderedItems` array).

#### Scenario: Successfully retrieve expanded inbox collection
- **GIVEN** an active studio "mystic-realms" and an active actor with an inbox collection
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/{actor-id}/inbox"
- **THEN** system responds with HTTP 200 OK
- **AND** response body contains "type": "OrderedCollection", "name": "inbox", "orderedItems": []

#### Scenario: Successfully retrieve expanded inbox collection by preferredUsername
- **GIVEN** an active studio "mystic-realms" and an active actor with preferred username
  "valkyrie_lead" and an inbox collection
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/valkyrie_lead/inbox"
- **THEN** system responds with HTTP 200 OK
- **AND** response body contains "type": "OrderedCollection", "name": "inbox", "orderedItems": []

#### Scenario: Successfully retrieve expanded outbox collection
- **GIVEN** an active studio "mystic-realms" and an active actor with an outbox collection
- **WHEN** client sends GET to "/api/studios/mystic-realms/v1/actors/{actor-id}/outbox"
- **THEN** system responds with HTTP 200 OK
- **AND** response body contains "type": "OrderedCollection", "name": "outbox", "orderedItems": []

#### Scenario: Return not found when retrieving collections for non-existent actor
- **GIVEN** an active studio "mystic-realms"
- **WHEN** client sends GET to
  "/api/studios/mystic-realms/v1/actors/019273c5-ffff-7000-8000-000000000000/inbox"
- **THEN** system responds with HTTP 404 Not Found

### Requirement: Post Entity Payloads to Actor Inbox and Outbox
The system SHALL expose an HTTP POST endpoint at
`/api/studios/{studio-id}/v1/actors/{actor-id}/outbox` accepting a JSON object payload and returning
HTTP 201 Created when the **Actor** is active, or HTTP 404 Not Found if missing or soft-deleted.
Requests to `POST /api/studios/{studio-id}/v1/actors/{actor-id}/inbox`, `POST .../inbox:message`,
and `POST .../inbox:batchMessage` SHALL delegate to `InboxRoute` (`featherpub-inbox-api`),
validating **FeatherPub** **Activity** payloads, routing via **generator** lineage, publishing to
AMQP 1.0, and returning HTTP 202 Accepted.

#### Scenario: Successfully post JSON payload to actor inbox
- **GIVEN** an active studio "mystic-realms" and an active actor "valkyrie_lead"
- **WHEN** client sends POST to "/api/studios/mystic-realms/v1/actors/valkyrie_lead/inbox" with a
  valid **FeatherPub** `Create` **Activity** JSON payload addressed to "valkyrie_lead"
- **THEN** system queues the **Activity** over AMQP 1.0 and responds with HTTP 202 Accepted

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
- **WHEN** client sends POST to
  "/api/studios/mystic-realms/v1/actors/019273c5-ffff-7000-8000-000000000000/outbox" with a JSON
  payload
- **THEN** system responds with HTTP 404 Not Found
