## ADDED Requirements

### Requirement: Tenanted Studio Hashtag Idempotent Creation Endpoint
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/tags` allowing authenticated clients to idempotently create or retrieve a **Hashtag** for a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public `studioId` UUID or a unique **Alias** string, and SHALL NOT accept internal database `tenant_id` values. The system SHALL resolve `{studio-id}` directly in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found directly on the HTTP route without querying the database or dispatching to an **Actor**.

Upon resolving `tenant_id`, the system SHALL validate the request payload (`tag` and optional `summary`). The `tag` field MUST be between 1 and 32 characters in length and contain only web-safe Unicode characters (excluding whitespace, control characters, and URI delimiters). If a leading `#` display symbol is provided in the `tag` string, the system SHALL sanitize and strip the leading `#`. If validation fails, the system SHALL return HTTP 400 Bad Request.

If validation succeeds, the system SHALL dispatch a creation command to `TagActor`. The system SHALL check for an existing active record matching `LOWER(tag)` under the studio's **Tenant** boundary:
- If a matching active hashtag already exists, the endpoint SHALL return HTTP 200 OK with the existing hashtag details.
- If a matching hashtag exists but was soft-deleted (`deleted_on IS NOT NULL`), the system SHALL reactivate the record by clearing `deleted_on` and updating `summary` if provided, returning HTTP 200 OK.
- If no matching hashtag exists, the system SHALL persist a new record in `njall_users.entities` (with generated UUIDv7, `tenant_id`, `entity_type = 'Hashtag'`, and audit timestamps), `njall_users.links` (with `link_type = 'hashtag'`, canonical `url = '/api/studios/{studio-id}/v1/tags/{tag-id}'`, and `media_type = 'application/json'`), and `njall_users.hashtags` (preserving original casing) via `HashtagDAO` using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created with the persisted hashtag. The response body SHALL NOT expose internal `tenant_id`.

#### Scenario: Successfully create new hashtag by alias
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags` with body:
  ```json
  {
    "tag": "SolarPunk",
    "summary": "Speculative eco-futuristic aesthetic"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `tag` equal to "SolarPunk", `linkType` equal to "hashtag", canonical `url` matching `/api/studios/valiant/v1/tags/SolarPunk`, `mediaType` equal to "application/json", `summary` equal to "Speculative eco-futuristic aesthetic", and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Idempotent creation returns existing hashtag with 200 OK
- **GIVEN** an active studio "valiant" with existing hashtag "SolarPunk"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags` with body:
  ```json
  {
    "tag": "solarpunk"
  }
  ```
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the returned hashtag `tag` preserves the original casing "SolarPunk" and returns the existing `id`

#### Scenario: Normalize tag by stripping leading display symbol
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags` with body:
  ```json
  {
    "tag": "#CyberLarp"
  }
  ```
- **THEN** the system returns HTTP 201 Created with `tag` equal to "CyberLarp"

#### Scenario: Creating hashtag for nonexistent studio returns 404
- **GIVEN** no studio exists with alias "unknown"
- **WHEN** a client sends a POST request to `/api/studios/unknown/v1/tags` with body:
  ```json
  {
    "tag": "NordicLarp"
  }
  ```
- **THEN** the system returns HTTP 404 Not Found without querying the database

#### Scenario: Creating hashtag with invalid length returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags` with body:
  ```json
  {
    "tag": "this_tag_name_exceeds_the_maximum_allowed_length_of_thirty_two_characters"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request

### Requirement: Tenanted Studio Hashtag Batch Creation Endpoint
The system SHALL expose an HTTP POST custom method endpoint at `/api/studios/{studio-id}/v1/tags:batchCreate` allowing clients to bulk-provision hashtags conforming to Google AIP-233. The request body SHALL contain a `requests` array of hashtag creation items. The system SHALL process all items idempotently within a single transaction: persisting newly encountered hashtags and resolving existing hashtags. The system SHALL return HTTP 200 OK with a `tags` array containing all resolved and created hashtag objects. If any item fails validation, the entire batch transaction SHALL be rolled back and the system SHALL return HTTP 400 Bad Request.

#### Scenario: Successfully bulk provision hashtags
- **GIVEN** an active studio "valiant" with existing hashtag "SolarPunk"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags:batchCreate` with body:
  ```json
  {
    "requests": [
      { "tag": "solarpunk" },
      { "tag": "DieselPunk", "summary": "Retro-futuristic diesel aesthetic" },
      { "tag": "CyberLarp" }
    ]
  }
  ```
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body contains a `tags` array with 3 items
- **AND** the first item has `tag` equal to "SolarPunk"
- **AND** the second item has `tag` equal to "DieselPunk" and `summary` equal to "Retro-futuristic diesel aesthetic"
- **AND** the third item has `tag` equal to "CyberLarp"

#### Scenario: Batch creation with invalid item returns 400
- **GIVEN** an active studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/tags:batchCreate` with body:
  ```json
  {
    "requests": [
      { "tag": "ValidTag" },
      { "tag": "" }
    ]
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request
- **AND** no new hashtags are persisted from the batch

### Requirement: Tenanted Studio Hashtag Dual-Identifier Retrieval Endpoint
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/tags/{tag-id}` allowing clients to retrieve details of a specific **Hashtag**. The system SHALL resolve `{studio-id}` in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found.

The `{tag-id}` path segment SHALL accept either a valid UUID string or a string tag name. If `{tag-id}` is a UUID string, the system SHALL query by `(tenant_id, id)`. If `{tag-id}` is not a UUID, the system SHALL query case-insensitively by `(tenant_id, LOWER(tag))`. If the matching hashtag exists and has not been soft-deleted (`deleted_on IS NULL`), the system SHALL return HTTP 200 OK with the hashtag's public fields. If the hashtag does not exist under that tenant or has `deleted_on` populated, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully retrieve hashtag by UUID
- **GIVEN** an active studio "valiant" and an active hashtag with UUID `tagId` and name "SolarPunk"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/tags/{tagId}`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body contains `id` equal to `tagId` and `tag` equal to "SolarPunk"

#### Scenario: Successfully retrieve hashtag by case-insensitive tag name
- **GIVEN** an active studio "valiant" and an active hashtag with name "SolarPunk"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/tags/solarpunk`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body preserves the case-preserved `tag` value "SolarPunk"

#### Scenario: Cross-tenant hashtag retrieval returns 404
- **GIVEN** studio "valiant" with hashtag "SolarPunk"
- **AND** another distinct studio "haven"
- **WHEN** a client sends a GET request to `/api/studios/haven/v1/tags/solarpunk`
- **THEN** the system returns HTTP 404 Not Found due to Row-Level Security isolation

#### Scenario: Retrieving soft-deleted hashtag returns 404
- **GIVEN** an active studio "valiant" with a hashtag whose `deleted_on` timestamp is non-null
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/tags/SolarPunk`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Tenanted Studio Hashtag Listing Endpoint
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/tags` allowing clients to list all active hashtags associated with the **Studio**. The system SHALL resolve `{studio-id}` against `StudioLookupCache`. Upon resolving `tenant_id`, the system SHALL query all hashtags in the tenant where `deleted_on IS NULL`, ordered alphabetically by `tag`. The system SHALL return HTTP 200 OK with a JSON array of hashtag objects. If no active hashtags exist for the studio, the endpoint SHALL return HTTP 200 OK with an empty array.

#### Scenario: Successfully list all active hashtags for studio
- **GIVEN** an active studio "valiant" with active hashtags "SolarPunk" and "DieselPunk"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/tags`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body contains an array of 2 hashtags

#### Scenario: Listing excludes soft-deleted hashtags
- **GIVEN** an active studio "valiant" with active hashtag "SolarPunk" and soft-deleted hashtag "OldTag"
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/tags`
- **THEN** the system returns HTTP 200 OK with an array containing only "SolarPunk"

### Requirement: Tenanted Studio Hashtag Modification Endpoint
The system SHALL expose an HTTP PATCH endpoint at `/api/studios/{studio-id}/v1/tags/{tag-id}` allowing partial updates to mutable hashtag fields (`summary`, `tag`) adhering to Google AIP-134 with an optional `update_mask` parameter. If `tag` is updated, the new tag value MUST satisfy validity constraints and MUST NOT collide case-insensitively with another active hashtag in the tenant. If the update succeeds, the system SHALL refresh `entities.updated_on` to `CURRENT_TIMESTAMP` and return HTTP 200 OK with the updated hashtag. If the hashtag does not exist or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully update hashtag summary
- **GIVEN** an active hashtag with ID `tagId` for studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/tags/{tagId}?update_mask=summary` with body:
  ```json
  {
    "summary": "Updated aesthetic description"
  }
  ```
- **THEN** the system returns HTTP 200 OK
- **AND** the response reflects the updated summary and refreshed `updatedOn`

#### Scenario: Successfully update hashtag casing
- **GIVEN** an active hashtag with ID `tagId` and tag "solarpunk" for studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/tags/{tagId}?update_mask=tag` with body:
  ```json
  {
    "tag": "SolarPunk"
  }
  ```
- **THEN** the system returns HTTP 200 OK
- **AND** the response reflects the updated casing "SolarPunk"

#### Scenario: Patching nonexistent hashtag returns 404
- **GIVEN** no hashtag exists with name "nonexistent"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/tags/nonexistent` with body:
  ```json
  {
    "summary": "New summary"
  }
  ```
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Tenanted Studio Hashtag Soft Deletion Endpoint
The system SHALL expose an HTTP DELETE endpoint at `/api/studios/{studio-id}/v1/tags/{tag-id}`. The `{tag-id}` parameter SHALL accept either a UUID string or a tag name. Upon invocation, the system SHALL set `entities.deleted_on = CURRENT_TIMESTAMP` for the matching row via `HashtagDAO`. The rows in `njall_users.hashtags`, `njall_users.links`, and `njall_users.entities` SHALL NOT be physically removed. If the target hashtag exists and is active, the system SHALL return HTTP 204 No Content. If the hashtag does not exist or is already soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully soft delete hashtag by tag name
- **GIVEN** an active hashtag "SolarPunk" under studio "valiant"
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/tags/SolarPunk`
- **THEN** the system returns HTTP 204 No Content
- **AND** subsequent GET requests to `/api/studios/valiant/v1/tags/SolarPunk` return HTTP 404 Not Found

#### Scenario: Soft deleting nonexistent hashtag returns 404
- **GIVEN** no hashtag exists with name "unknown"
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/tags/unknown`
- **THEN** the system returns HTTP 404 Not Found
