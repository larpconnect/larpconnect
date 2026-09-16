# admin-server-api Specification

## Purpose

Provides an administrative HTTP endpoint at `/api/admin/v1/servers` returning registered server topologies and contact points in camelCase JSON, isolating blocking persistence queries on a dedicated Pekko blocking dispatcher.

## Requirements

### Requirement: Administrative Server Listing Endpoint
The system SHALL accept HTTP GET requests at `/api/admin/v1/servers` and return an HTTP 200 OK status code with an `application/json` payload containing an array of registered servers and their associated contact points in camelCase format.

#### Scenario: Successful retrieval of servers and contacts
- **GIVEN** the HTTP server is running with database migrations applied and seed records present
- **WHEN** a client sends an HTTP GET request to `/api/admin/v1/servers`
- **THEN** the server responds with HTTP status 200 OK
- **AND** the response content-type is `application/json`
- **AND** the response body contains a JSON array of servers with fields `id`, `name`, `primaryDomain`, `createdOn`, and an array of `contacts`
- **AND** each contact contains `id`, `roleType`, `contactType`, `contact`, and `ordering`

### Requirement: Asynchronous Isolation of Blocking Persistence Queries
The administrative server routing layer SHALL handle HTTP requests asynchronously by dispatching requests via Pekko Typed message protocols to a `ServerAdminActor`, which SHALL be isolated on a dedicated Pekko blocking dispatcher (`larpconnect.blocking-dispatcher`) to prevent blocking `ServerDAO` operations from starving the Pekko HTTP routing dispatcher.

#### Scenario: Non-blocking execution of server query
- **GIVEN** an inbound HTTP GET request arrives at `/api/admin/v1/servers`
- **WHEN** the request is received by `DefaultAdminRoute`
- **THEN** the route dispatches a command to `ServerAdminActor` using the typed Ask pattern
- **AND** the database retrieval executes on the dedicated blocking dispatcher without stalling the HTTP routing thread
- **AND** the route completes the HTTP request with the serialized server list
