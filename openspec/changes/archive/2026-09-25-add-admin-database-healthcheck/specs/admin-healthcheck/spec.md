## MODIFIED Requirements

### Requirement: Admin Health Endpoint Serves 200 OK When Healthy
The system SHALL accept HTTP GET requests at path `/api/admin/v1/health` and return an HTTP 200 OK status code with an empty response body when all registered health checks, including Pekko runtime and administrative database probes, report healthy. Consecutive requests received within the configured probe cache window SHALL return the cached healthy status without re-executing underlying subsystem queries.

#### Scenario: Health check passes when Pekko is healthy
- **GIVEN** the HTTP server is running and the Pekko framework is healthy
- **WHEN** a client sends an HTTP GET request to `/api/admin/v1/health`
- **THEN** the response status code is 200 OK
- **AND** the response body is empty

#### Scenario: Cached health probe result served on subsequent requests
- **GIVEN** an initial health probe to `/api/admin/v1/health` completed successfully and cached a healthy result
- **WHEN** a client sends another HTTP GET request to `/api/admin/v1/health` within the 10-second cache window
- **THEN** the response status code is 200 OK
- **AND** the response body is empty
- **AND** no new database ping query is executed

### Requirement: Admin Health Endpoint Serves 500 When Unhealthy
The system SHALL return an HTTP 500 Internal Server Error status code with an empty response body when any registered health check reports unhealthy, throws an unhandled exception, encounters database connectivity failure, or fails to respond within the configured ask timeout.

#### Scenario: Health check fails when Pekko is terminating or unhealthy
- **GIVEN** the Pekko framework is terminating or an unhealthy state is detected
- **WHEN** a client sends an HTTP GET request to `/api/admin/v1/health`
- **THEN** the response status code is 500 Internal Server Error
- **AND** the response body is empty

#### Scenario: Health check actor ask times out
- **GIVEN** the health check actor does not respond within the configured ask timeout
- **WHEN** a client sends an HTTP GET request to `/api/admin/v1/health`
- **THEN** the response status code is 500 Internal Server Error
- **AND** the response body is empty

#### Scenario: Health check fails when database is unreachable or times out
- **GIVEN** the `njall_admin` database connection fails, is terminated, or times out during probe evaluation
- **WHEN** a client sends an HTTP GET request to `/api/admin/v1/health`
- **THEN** the response status code is 500 Internal Server Error
- **AND** the response body is empty
