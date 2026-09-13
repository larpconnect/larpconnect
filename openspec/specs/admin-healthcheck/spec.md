# admin-healthcheck Specification

## Purpose

Provides an administrative health probe at `/api/admin/v1/health` that inspects the health and responsiveness of the Apache Pekko framework and registered Dropwizard health checks.

## Requirements

### Requirement: Admin Health Endpoint Serves 200 OK When Healthy
The system SHALL accept HTTP GET requests at path `/api/admin/v1/health` and return an HTTP 200 OK status code with an empty response body when all registered health checks report healthy.

#### Scenario: Health check passes when Pekko is healthy
- **GIVEN** the HTTP server is running and the Pekko framework is healthy
- **WHEN** a client sends an HTTP GET request to `/api/admin/v1/health`
- **THEN** the response status code is 200 OK
- **AND** the response body is empty

### Requirement: Admin Health Endpoint Serves 500 When Unhealthy
The system SHALL return an HTTP 500 Internal Server Error status code with an empty response body when any registered health check reports unhealthy, throws an unhandled exception, or fails to respond within the configured ask timeout.

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

### Requirement: Public Access for Admin Health Endpoint
The system SHALL allow unauthenticated public access to the `/api/admin/v1/health` endpoint without requiring credentials or authorization headers.

#### Scenario: Unauthenticated request is processed
- **GIVEN** the HTTP server is running
- **WHEN** a client sends an HTTP GET request to `/api/admin/v1/health` without authorization headers
- **THEN** the server processes the request without returning an authentication challenge
