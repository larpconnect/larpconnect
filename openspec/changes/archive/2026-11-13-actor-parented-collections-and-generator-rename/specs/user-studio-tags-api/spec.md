## MODIFIED Requirements

### Requirement: Tenanted Studio Hashtag Idempotent Creation Endpoint
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/tags` allowing authenticated clients to idempotently create or retrieve a **Hashtag** for a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public `studioId` UUID or a unique **Alias** string, and SHALL NOT accept internal database `tenant_id` values. The system SHALL resolve `{studio-id}` directly in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found directly on the HTTP route without querying the database or dispatching to an **Actor**.

Upon resolving `tenant_id`, the system SHALL validate the request payload (`generator` required string accepting either a bare **Actor** UUID or a canonical studio **Actor** URI `/api/studios/{studio-id}/v1/actors/{actor-id}` matching `{studio-id}`, `tag`, and optional `summary`). The `generator` field MUST resolve to an existing, active **Actor** **Entity** (`entity_type = 'Actor'`) belonging to the same studio **Tenant**; if `generator` is missing, blank, malformed, references another studio, or does not resolve to an active **Actor** in the same **Tenant**, the system SHALL return HTTP 400 Bad Request. The `tag` field MUST be between 1 and 32 characters in length and contain only web-safe Unicode characters (excluding whitespace, control characters, and URI delimiters). If a leading `#` display symbol is provided in the `tag` string, the system SHALL sanitize and strip the leading `#`. If validation fails, the system SHALL return HTTP 400 Bad Request.

If validation succeeds, the system SHALL dispatch a creation command to `TagActor`. The system SHALL check for an existing active record matching `LOWER(tag)` under the studio's **Tenant** boundary:
- If a matching active hashtag already exists, the endpoint SHALL return HTTP 200 OK with the existing hashtag details (preserving the original `generator` URI).
- If a matching hashtag exists but was soft-deleted (`deleted_on IS NOT NULL`), the system SHALL reactivate the record by clearing `deleted_on` and updating `summary` if provided (preserving the immutable `entities.generator` provenance), returning HTTP 200 OK.
- If no matching hashtag exists, the system SHALL persist a new record in `njall_users.entities` (with generated UUIDv7, `tenant_id`, `generator` set to the resolved **generator** **Actor** UUID, `entity_type = 'Hashtag'`, and audit timestamps), `njall_users.links` (with `link_type = 'hashtag'`, canonical `url = '/api/studios/{studio-id}/v1/tags/{tag-id}'`, and `media_type = 'application/json'`), and `njall_users.hashtags` (preserving original casing) via `HashtagDAO` using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created with the persisted hashtag including `generator` formatted as `"/api/studios/{studio-id}/v1/actors/{generator-id}"`. The response body SHALL NOT expose internal `tenant_id`.

#### Scenario: Successfully create new hashtag by alias
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "tag": "SolarPunk",
    "summary": "Speculative eco-futuristic aesthetic"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `generator` equal to "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010", `tag` equal to "SolarPunk", `linkType` equal to "hashtag", canonical `url` matching `/api/studios/valiant/v1/tags/SolarPunk`, `mediaType` equal to "application/json", `summary` equal to "Speculative eco-futuristic aesthetic", and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Idempotent creation returns existing hashtag with 200 OK
- **GIVEN** an active studio "valiant" with existing hashtag "SolarPunk" and an active actor with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "tag": "solarpunk"
  }
  ```
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the returned hashtag `tag` preserves the original casing "SolarPunk" and returns the existing `id` and `generator`

#### Scenario: Normalize tag by stripping leading display symbol
- **GIVEN** an active studio exists with alias "valiant" and an active actor exists with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags` with body:
  ```json
  {
    "generator": "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010",
    "tag": "#CyberLarp"
  }
  ```
- **THEN** the system returns HTTP 201 Created with `tag` equal to "CyberLarp" and `generator` equal to "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010"

#### Scenario: Creating hashtag for nonexistent studio returns 404
- **GIVEN** no studio exists with alias "unknown"
- **WHEN** a client sends a POST request to `/api/studios/unknown/v1/tags` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "tag": "NordicLarp"
  }
  ```
- **THEN** the system returns HTTP 404 Not Found without querying the database

#### Scenario: Creating hashtag with invalid length returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags` with a tag exceeding 32 characters or a missing/non-Actor `generator`
- **THEN** the system returns HTTP 400 Bad Request

### Requirement: Tenanted Studio Hashtag Batch Creation Endpoint
The system SHALL expose an HTTP POST custom method endpoint at `/api/studios/{studio-id}/v1/tags:batchCreate` allowing clients to bulk-provision hashtags conforming to Google AIP-233. The request body SHALL contain a `requests` array of hashtag creation items, where each item requires `generator` (accepting either a bare **Actor** UUID or a canonical studio **Actor** URI `/api/studios/{studio-id}/v1/actors/{actor-id}` referencing an active **Actor** in the same **Tenant**) and `tag`, plus optional `summary`. The system SHALL process all items idempotently within a single transaction: persisting newly encountered hashtags with their resolved **generator** **Actor** UUID in `njall_users.entities.generator` and resolving existing hashtags. The system SHALL return HTTP 200 OK with a `tags` array containing all resolved and created hashtag objects including each item's `generator` URI. If any item fails tag or `generator` validation, the entire batch transaction SHALL be rolled back and the system SHALL return HTTP 400 Bad Request.

#### Scenario: Successfully bulk provision hashtags
- **GIVEN** an active studio "valiant" with existing hashtag "SolarPunk" and an active actor with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags:batchCreate` with body:
  ```json
  {
    "requests": [
      { "generator": "01925b6a-93be-7000-8000-000000000010", "tag": "solarpunk" },
      { "generator": "01925b6a-93be-7000-8000-000000000010", "tag": "DieselPunk", "summary": "Retro-futuristic diesel aesthetic" },
      { "generator": "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010", "tag": "CyberLarp" }
    ]
  }
  ```
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body contains a `tags` array with 3 items, each containing `generator` equal to `"/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010"`
- **AND** the first item has `tag` equal to "SolarPunk"
- **AND** the second item has `tag` equal to "DieselPunk" and `summary` equal to "Retro-futuristic diesel aesthetic"
- **AND** the third item has `tag` equal to "CyberLarp"

#### Scenario: Batch creation with invalid item returns 400
- **GIVEN** an active studio "valiant" and an active actor with UUID "01925b6a-93be-7000-8000-000000000010"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags:batchCreate` with body:
  ```json
  {
    "requests": [
      { "generator": "01925b6a-93be-7000-8000-000000000010", "tag": "ValidTag" },
      { "generator": "01925b6a-93be-7000-8000-000000000010", "tag": "" }
    ]
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request
- **AND** no new hashtags are persisted from the batch

