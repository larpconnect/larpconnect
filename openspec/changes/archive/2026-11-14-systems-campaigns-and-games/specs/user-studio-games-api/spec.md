## ADDED Requirements

### Requirement: Tenanted Studio Game Creation Endpoint
The system SHALL expose `POST /api/studios/{studio-id}/v1/games` (`404` on missing **Studio** **Alias**/UUID) requiring active `'Actor'` `generator`, active `'System'` `system`, and non-blank `title`, plus optional `'Campaign'` `campaign` (independent of `campaign.system`), **Locations** `locationId`, `summary`, and `startTime`/`endTime` (`endTime >= startTime`; `400` if invalid), persisting a 2nd-level CTI **Games** **Entity** and returning `201 Created` (`eventType = "GAME"`).

#### Scenario: Successfully create game with system, campaign, location, and schedule
- **GIVEN** an active studio exists with alias "valiant"
- **AND** active actor `actorId`, active system `systemA`, active campaign `campaignId` (configured with a distinct system `systemB`), and active location `locationId` exist under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/games` with body:
  ```json
  {
    "generator": "{actorId}",
    "system": "/api/studios/valiant/v1/systems/{systemA}",
    "campaign": "{campaignId}",
    "title": "Elysium at the Museum",
    "summary": "Opening court session",
    "locationId": "{locationId}",
    "startTime": "2026-11-20T19:00:00Z",
    "endTime": "2026-11-20T23:30:00Z"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains generated UUID `id`, `eventType` equal to "GAME", `generator` equal to "/api/studios/valiant/v1/actors/{actorId}", `system` equal to "/api/studios/valiant/v1/systems/{systemA}", `campaign` equal to "/api/studios/valiant/v1/campaigns/{campaignId}", `title` equal to "Elysium at the Museum", `locationId` equal to "{locationId}", `startTime` equal to "2026-11-20T19:00:00Z", `endTime` equal to "2026-11-20T23:30:00Z", and non-null `createdOn` and `updatedOn`

#### Scenario: Successfully create standalone one-shot game without campaign
- **GIVEN** an active studio "valiant", active actor `actorId`, and active system `systemId`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/games` with body:
  ```json
  {
    "generator": "{actorId}",
    "system": "{systemId}",
    "title": "Winter Solstice One-Shot"
  }
  ```
- **THEN** the system returns HTTP 201 Created with `eventType` equal to "GAME", `system` equal to "/api/studios/valiant/v1/systems/{systemId}", and null `campaign`

#### Scenario: Creating game without active system returns 400
- **GIVEN** an active studio "valiant" and active actor `actorId`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/games` omitting `system`
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating game with invalid time order returns 400
- **GIVEN** an active studio "valiant", active actor `actorId`, and active system `systemId`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/games` with `startTime` after `endTime`
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

### Requirement: Tenanted Studio Game Collection Listing Endpoint
The system SHALL expose `GET /api/studios/{studio-id}/v1/games` (`404` on missing studio) returning active `'Game'` entities ordered by `start_time ASC NULLS LAST, created_on DESC` (excluding `'Event'` **Events**), with optional `?systemId` and `?campaignId` UUID-or-URI filters (`400` if malformed or cross-studio).

#### Scenario: List active games filtered by systemId and campaignId
- **GIVEN** an active studio "valiant" has active game `game1` (`systemA`, `campaign1`), active game `game2` (`systemA`, `campaign2`), and active non-game workshop event `event1`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/games?systemId={systemA}&campaignId={campaign1}`
- **THEN** the system returns HTTP 200 OK with a JSON array containing only `game1`

### Requirement: Tenanted Studio Game Detail Retrieval Endpoint
The system SHALL expose `GET /api/studios/{studio-id}/v1/games/{game-id}` returning `200 OK` when `{studio-id}` is active and `{game-id}` resolves to an active `'Game'` **Entity** in the **Tenant**, or `404 Not Found` if missing, soft-deleted, cross-tenant, malformed, or referencing a non-game `'Event'` **Entity**.

#### Scenario: Retrieving non-game event ID via games endpoint returns 404
- **GIVEN** an active studio "valiant" and an active non-game workshop event `eventId` (`entity_type = 'Event'`)
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/games/{eventId}`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Tenanted Studio Game Partial Update Endpoint
The system SHALL expose `PATCH /api/studios/{studio-id}/v1/games/{game-id}` conforming to AIP-134 (`update_mask` over required `system`/`title` and nullable `campaign`, `summary`, `locationId`, `startTime`, `endTime`), validating active references and `endTime >= startTime` (`400` otherwise) and returning `200 OK` (`404` if missing).

#### Scenario: Successfully reassign game system and clear optional campaign via update_mask
- **GIVEN** an active game `gameId` associated with `systemA` and `campaign1` under studio "valiant"
- **AND** another active system `systemB` exists under studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/games/{gameId}?update_mask=system,campaign` with body:
  ```json
  {
    "system": "/api/studios/valiant/v1/systems/{systemB}",
    "campaign": null
  }
  ```
- **THEN** the system returns HTTP 200 OK with `system` equal to "/api/studios/valiant/v1/systems/{systemB}", `campaign` equal to `null`, and `eventType` equal to "GAME"

### Requirement: Tenanted Studio Game Soft Deletion Endpoint
The system SHALL expose `DELETE /api/studios/{studio-id}/v1/games/{game-id}` setting `entities.deleted_on` and `updated_on` to `CURRENT_TIMESTAMP` on the active `'Game'` **Entity** without physical deletion, returning `204 No Content` on success or `404 Not Found` if missing, non-game, or already soft-deleted.

#### Scenario: Successfully soft delete game
- **GIVEN** an active game `gameId` under studio "valiant"
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/games/{gameId}`
- **THEN** the system returns HTTP 204 No Content
- **AND** subsequent GET requests to `/api/studios/valiant/v1/games/{gameId}` return HTTP 404 Not Found
