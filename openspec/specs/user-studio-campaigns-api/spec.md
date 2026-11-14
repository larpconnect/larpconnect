# user-studio-campaigns-api Specification

## Purpose

Tenanted HTTP REST endpoints under `/api/studios/{studio-id}/v1/campaigns` for creating, retrieving, listing (with optional `?systemId` filtering), partially updating, and soft-deleting studio campaign entities extending entities via Common Table Inheritance (CTI) with PostgreSQL Row-Level Security (RLS) isolation.

## Requirements

### Requirement: Tenanted Studio Campaign Creation Endpoint
The system SHALL expose `POST /api/studios/{studio-id}/v1/campaigns` (`404` on missing/deleted **Studio** **Alias** or UUID) requiring active `'Actor'` `generator` and non-blank `title`, plus optional active `'System'` `system` (UUID or studio URI) and `summary` (`400` on invalid/inactive input), persisting `'Campaign'` in the **Tenant** and returning `201 Created` with canonical `generator` and `system` URIs.

#### Scenario: Successfully create campaign associated with a system
- **GIVEN** an active studio exists with alias "valiant"
- **AND** an active actor `01925b6a-93be-7000-8000-000000000010` and an active system `01925b6a-93be-7000-8000-000000000020` exist under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/campaigns` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "system": "/api/studios/valiant/v1/systems/01925b6a-93be-7000-8000-000000000020",
    "title": "Seattle By Night",
    "summary": "Ongoing Camarilla chronicle"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains generated UUID `id`, `generator` equal to "/api/studios/valiant/v1/actors/01925b6a-93be-7000-8000-000000000010", `system` equal to "/api/studios/valiant/v1/systems/01925b6a-93be-7000-8000-000000000020", `title` equal to "Seattle By Night", `summary` equal to "Ongoing Camarilla chronicle", and non-null `createdOn` and `updatedOn`

#### Scenario: Successfully create systemless campaign
- **GIVEN** an active studio exists with alias "valiant" and an active actor `01925b6a-93be-7000-8000-000000000010` exists under studio "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/campaigns` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "title": "Seasonal Anthology Series"
  }
  ```
- **THEN** the system returns HTTP 201 Created with `title` equal to "Seasonal Anthology Series", null `system`, and null `summary`

#### Scenario: Creating campaign with nonexistent or soft-deleted system returns 400
- **GIVEN** an active studio exists with alias "valiant" and an active actor `01925b6a-93be-7000-8000-000000000010`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/campaigns` with body:
  ```json
  {
    "generator": "01925b6a-93be-7000-8000-000000000010",
    "system": "01925b6a-93be-7000-8000-000000000999",
    "title": "Broken Reference Chronicle"
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating campaign with blank title returns 400
- **GIVEN** an active studio exists with alias "valiant" and an active actor `01925b6a-93be-7000-8000-000000000010`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/campaigns` with body `{"generator": "01925b6a-93be-7000-8000-000000000010", "title": "   "}`
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

### Requirement: Tenanted Studio Campaign Collection Listing Endpoint
The system SHALL expose `GET /api/studios/{studio-id}/v1/campaigns` (`404` on missing studio) returning active **Campaigns** ordered by `title ASC, created_on DESC`, supporting optional `?systemId` (UUID or studio **System** URI; `400` if malformed/cross-studio) and emitting `system: null` when a previously linked **System** has been soft-deleted.

#### Scenario: List active campaigns filtered by systemId
- **GIVEN** an active studio "valiant" has active campaign "Chronicle Alpha" associated with system `systemA` and active campaign "Chronicle Beta" associated with system `systemB`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/campaigns?systemId={systemA}`
- **THEN** the system returns HTTP 200 OK with a JSON array containing only "Chronicle Alpha"

#### Scenario: List all active campaigns excluding soft-deleted campaigns
- **GIVEN** an active studio "valiant" has two active campaigns and one soft-deleted campaign
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/campaigns`
- **THEN** the system returns HTTP 200 OK with a JSON array containing only the 2 active campaigns

### Requirement: Tenanted Studio Campaign Detail Retrieval Endpoint
The system SHALL expose `GET /api/studios/{studio-id}/v1/campaigns/{campaign-id}` returning `200 OK` with active `'Campaign'` **Entity** details in the resolved **Tenant** (excluding `tenantId`), or `404 Not Found` if `{studio-id}` or `{campaign-id}` is invalid, missing, cross-tenant, or soft-deleted.

#### Scenario: Successfully retrieve active campaign
- **GIVEN** an active studio "valiant" and an active campaign `campaignId`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/campaigns/{campaignId}`
- **THEN** the system returns HTTP 200 OK with the campaign details and excludes `tenantId`

#### Scenario: Retrieving cross-tenant or soft-deleted campaign returns 404
- **GIVEN** studio "valiant" with campaign `campaignId` and another active studio "haven"
- **WHEN** a client sends a GET request to `/api/studios/haven/v1/campaigns/{campaignId}`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Tenanted Studio Campaign Partial Update Endpoint
The system SHALL expose `PATCH /api/studios/{studio-id}/v1/campaigns/{campaign-id}` conforming to AIP-134 (`update_mask` over `title`, nullable `summary`, and nullable `system`), rejecting blank `title` or inactive/invalid `system` (`400`), refreshing `updated_on`, and returning `200 OK` (or `404` if missing).

#### Scenario: Successfully update campaign title and clear system association via update_mask
- **GIVEN** an active campaign `campaignId` associated with `systemA` under studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/campaigns/{campaignId}?update_mask=title,system` with body:
  ```json
  {
    "title": "System-Agnostic Anthology",
    "system": null
  }
  ```
- **THEN** the system returns HTTP 200 OK with `title` equal to "System-Agnostic Anthology", `system` equal to `null`, and refreshed `updatedOn`

#### Scenario: Patching campaign with inactive or non-System reference returns 400
- **GIVEN** an active campaign `campaignId` and an active individual `individualId` under studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/campaigns/{campaignId}` with body `{"system": "{individualId}"}`
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

### Requirement: Tenanted Studio Campaign Soft Deletion Endpoint
The system SHALL expose `DELETE /api/studios/{studio-id}/v1/campaigns/{campaign-id}` setting `entities.deleted_on` and `updated_on` to `CURRENT_TIMESTAMP` on the active `'Campaign'` row without physical deletion, returning `204 No Content` on success or `404 Not Found` if missing or already soft-deleted.

#### Scenario: Successfully soft delete campaign
- **GIVEN** an active campaign `campaignId` under studio "valiant"
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/campaigns/{campaignId}`
- **THEN** the system returns HTTP 204 No Content
- **AND** subsequent GET requests to `/api/studios/valiant/v1/campaigns/{campaignId}` return HTTP 404 Not Found
