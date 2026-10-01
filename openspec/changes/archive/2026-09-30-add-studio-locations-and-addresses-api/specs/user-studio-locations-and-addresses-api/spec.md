## ADDED Requirements

### Requirement: Tenanted Studio Location Creation Endpoint
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/locations` allowing authenticated clients to create a **Locations** **Entity** for a specific **Studio**. The `{studio-id}` path segment SHALL accept either a public studio UUID or an **Alias** string, resolving in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL validate the request payload (`name` required non-blank, optional `summary`). If validation succeeds, the system SHALL persist the record in `njall_users.entities` (with generated UUIDv7, `tenant_id`, `entity_type = 'Location'`) and `njall_users.locations` (with `(tenant_id, id)` and `name`) using an active `@NjallUsers` session configured with `app.tenant_id = :tenantId`. The endpoint SHALL return HTTP 201 Created containing the public location `id`, `name`, `summary`, `createdOn`, and `updatedOn`. The response SHALL NOT expose the internal `tenant_id`.

#### Scenario: Successfully create location for studio by alias
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/locations` with body:
  ```json
  {
    "name": "Camp Whispering Pines",
    "summary": "Main outdoor event campsite"
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `name` equal to "Camp Whispering Pines", `summary` equal to "Main outdoor event campsite", and non-null timestamps `createdOn` and `updatedOn`
- **AND** the response body does not contain `tenantId`

#### Scenario: Creating location with blank name returns 400
- **GIVEN** an active studio exists with alias "valiant"
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/locations` with body:
  ```json
  {
    "name": ""
  }
  ```
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Creating location for nonexistent studio returns 404
- **GIVEN** no studio exists with alias "unknown"
- **WHEN** a client sends a POST request to `/api/studios/unknown/v1/locations` with a valid location payload
- **THEN** the system returns HTTP 404 Not Found without querying the database

### Requirement: Tenanted Studio Location Retrieval Endpoint
The system SHALL expose an HTTP GET endpoint at `/api/studios/{studio-id}/v1/locations/{location-id}` allowing clients to retrieve details of a specific **Locations** **Entity**. The system SHALL resolve `{studio-id}` in-memory against `StudioLookupCache`. If `{studio-id}` is missing or soft-deleted, or if `{location-id}` is not a valid UUID string, the system SHALL return HTTP 404 Not Found. Upon resolving `tenant_id`, the system SHALL query `LocationDAO` under `app.tenant_id = :tenantId`. If the location exists and is active (`deleted_on IS NULL`), the system SHALL return HTTP 200 OK. If the location does not exist, belongs to another **Tenant**, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully retrieve active location
- **GIVEN** an active studio "valiant" and a persisted location with UUID `locationId`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/locations/{locationId}`
- **THEN** the system returns HTTP 200 OK with content-type `application/json`
- **AND** the response body matches the location attributes and excludes `tenantId`

#### Scenario: Retrieving soft-deleted location returns 404
- **GIVEN** an active studio "valiant" and a location whose `deleted_on` timestamp is non-null
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/locations/{locationId}`
- **THEN** the system returns HTTP 404 Not Found

#### Scenario: Cross-tenant location access returns 404
- **GIVEN** studio "valiant" with a location `locationId`
- **AND** another distinct studio "haven"
- **WHEN** a client sends a GET request to `/api/studios/haven/v1/locations/{locationId}`
- **THEN** the system returns HTTP 404 Not Found due to Row-Level Security isolation

### Requirement: Tenanted Studio Location Modification Endpoint
The system SHALL expose an HTTP PATCH endpoint at `/api/studios/{studio-id}/v1/locations/{location-id}` allowing partial updates to mutable fields (`name`, `summary`) conforming to Google AIP-134. Upon receiving a valid request for an active location within the studio's tenant boundary, the system SHALL update `njall_users.locations` and `njall_users.entities`, refresh `updated_on`, and return HTTP 200 OK. If the location does not exist, belongs to another **Tenant**, or is soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully update location name and summary
- **GIVEN** an active location with ID `locationId` for studio "valiant"
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/locations/{locationId}` with body:
  ```json
  {
    "name": "Camp Whispering Pines - North",
    "summary": "Updated north campsite"
  }
  ```
- **THEN** the system returns HTTP 200 OK with the updated name and refreshed `updatedOn`

#### Scenario: Patching soft-deleted location returns 404
- **GIVEN** a location ID that has been soft-deleted
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/locations/{locationId}`
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Tenanted Studio Location Soft Deletion Endpoint
The system SHALL expose an HTTP DELETE endpoint at `/api/studios/{studio-id}/v1/locations/{location-id}`. Upon invocation, the system SHALL set `entities.deleted_on = CURRENT_TIMESTAMP` for the matching `(tenant_id, id)` row. The row in `njall_users.locations` and `njall_users.entities` SHALL NOT be physically removed. If the target location exists and is active, the system SHALL return HTTP 204 No Content. If the location does not exist or is already soft-deleted, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully soft delete location
- **GIVEN** an active location `locationId` under studio "valiant"
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/locations/{locationId}`
- **THEN** the system returns HTTP 204 No Content
- **AND** subsequent GET requests to `/api/studios/valiant/v1/locations/{locationId}` return HTTP 404 Not Found

### Requirement: Tenanted Location Address Creation Endpoint with GeoJSON Point
The system SHALL expose an HTTP POST endpoint at `/api/studios/{studio-id}/v1/locations/{location-id}/addresses` allowing clients to add an **Address** to an active **Locations** **Entity**. The request payload SHALL contain `addressType` (one of `PHYSICAL`, `MAILING`, `PO_BOX`, `BILLING`, `OTHER`), `addressLine1`, optional `addressLine2`, optional `addressLine3`, `locality`, `administrativeArea`, `postalCode`, `countryCode` (2-letter ISO 3166-1 alpha-2), and an optional `geom` GeoJSON Point object formatted as `{"type": "Point", "coordinates": [longitude, latitude]}` where longitude is between -180.0 and 180.0 and latitude is between -90.0 and 90.0. The system SHALL persist the address into `njall_users.addresses` under the parent location's `tenant_id` and `location_id`. The endpoint SHALL return HTTP 201 Created with the generated address `id`, `locationId`, and all address properties including formatted GeoJSON Point.

#### Scenario: Successfully add physical address with GeoJSON Point to location
- **GIVEN** an active studio "valiant" and an active location `locationId`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/locations/{locationId}/addresses` with body:
  ```json
  {
    "addressType": "PHYSICAL",
    "addressLine1": "123 Camp Road",
    "locality": "Pineville",
    "administrativeArea": "WA",
    "postalCode": "98101",
    "countryCode": "US",
    "geom": {
      "type": "Point",
      "coordinates": [-122.3321, 47.6062]
    }
  }
  ```
- **THEN** the system returns HTTP 201 Created with content-type `application/json`
- **AND** the response body contains a generated UUID `id`, `locationId` matching `locationId`, `addressType` equal to "PHYSICAL", and `geom` matching the GeoJSON Point coordinates `[-122.3321, 47.6062]`

#### Scenario: Adding address with invalid GeoJSON latitude returns 400
- **GIVEN** an active studio "valiant" and an active location `locationId`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/locations/{locationId}/addresses` with `geom.coordinates` having latitude 95.0
- **THEN** the system returns HTTP 400 Bad Request with an AIP-193 structured error response

#### Scenario: Adding address to soft-deleted location returns 404
- **GIVEN** an active studio "valiant" and a soft-deleted location `locationId`
- **WHEN** a client sends a POST request to `/api/studios/valiant/v1/locations/{locationId}/addresses` with a valid address payload
- **THEN** the system returns HTTP 404 Not Found

### Requirement: Tenanted Location Address Retrieval and Listing Endpoints
The system SHALL expose HTTP GET endpoints at `/api/studios/{studio-id}/v1/locations/{location-id}/addresses/{address-id}` and `/api/studios/{studio-id}/v1/locations/{location-id}/addresses`. The single item endpoint SHALL return HTTP 200 OK with the address details if found under the resolved tenant and location, or HTTP 404 Not Found otherwise. The collection endpoint SHALL return HTTP 200 OK with a JSON array of all addresses associated with the location under the resolved tenant.

#### Scenario: Successfully retrieve address by ID
- **GIVEN** an active studio "valiant", active location `locationId`, and address `addressId`
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/locations/{locationId}/addresses/{addressId}`
- **THEN** the system returns HTTP 200 OK with the address details

#### Scenario: Successfully list all addresses for location
- **GIVEN** an active location `locationId` with 2 associated addresses
- **WHEN** a client sends a GET request to `/api/studios/valiant/v1/locations/{locationId}/addresses`
- **THEN** the system returns HTTP 200 OK with a JSON array containing 2 address objects

### Requirement: Tenanted Location Address Modification Endpoint
The system SHALL expose an HTTP PATCH endpoint at `/api/studios/{studio-id}/v1/locations/{location-id}/addresses/{address-id}` allowing partial updates to mutable address fields (`addressType`, `addressLine1`, `addressLine2`, `addressLine3`, `locality`, `administrativeArea`, `postalCode`, `countryCode`, `geom`) conforming to Google AIP-134. Upon receiving a valid request for an address under the matching tenant and location, the system SHALL update `njall_users.addresses` and return HTTP 200 OK with the updated address object.

#### Scenario: Successfully update address lines and coordinates
- **GIVEN** an existing address `addressId` under location `locationId`
- **WHEN** a client sends a PATCH request to `/api/studios/valiant/v1/locations/{locationId}/addresses/{addressId}` with updated `addressLine1` and new `geom`
- **THEN** the system returns HTTP 200 OK reflecting the updated fields

### Requirement: Tenanted Location Address Deletion Endpoint
The system SHALL expose an HTTP DELETE endpoint at `/api/studios/{studio-id}/v1/locations/{location-id}/addresses/{address-id}`. Upon invocation, the system SHALL physically delete the matching address row from `njall_users.addresses`. If the address exists under the matching tenant and location, the system SHALL return HTTP 204 No Content. If the address does not exist, the system SHALL return HTTP 404 Not Found.

#### Scenario: Successfully delete address
- **GIVEN** an existing address `addressId` under location `locationId`
- **WHEN** a client sends a DELETE request to `/api/studios/valiant/v1/locations/{locationId}/addresses/{addressId}`
- **THEN** the system returns HTTP 204 No Content
- **AND** subsequent GET requests to `/api/studios/valiant/v1/locations/{locationId}/addresses/{addressId}` return HTTP 404 Not Found
