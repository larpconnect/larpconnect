## Purpose

Provides a foundational Pekko HTTP runtime server with Typesafe configuration, graceful coordinated shutdown, and a root health/status endpoint.

## ADDED Requirements

### Requirement: Root Endpoint Serves Empty OK Response
The system SHALL accept HTTP GET requests at path `/` and return an HTTP 200 OK status code with an empty response body.

#### Scenario: Client requests the root endpoint
- **GIVEN** the HTTP server is running
- **WHEN** a client sends an HTTP GET request to `/`
- **THEN** the response status code is 200 OK
- **AND** the response body is empty

### Requirement: Configurable Server Port and Host via Typesafe Config
The system SHALL bind the HTTP server to a configurable host and port defaulting to `0.0.0.0:8080`. The configuration SHALL support HOCON files and environment variable overrides (`PORT` and `HOST`). The server SHALL support dynamic port binding (port 0) for ephemeral test environments.

#### Scenario: Server starts with default configuration
- **GIVEN** no configuration overrides are supplied
- **WHEN** the HTTP server initializes
- **THEN** it binds to host `0.0.0.0` and port `8080`

#### Scenario: Server port overridden via environment
- **GIVEN** the environment variable `PORT` is configured to `9090`
- **WHEN** the HTTP server initializes
- **THEN** it binds to port `9090`

### Requirement: Graceful Coordinated Shutdown
The system SHALL integrate with Pekko CoordinatedShutdown to ensure that upon receiving a JVM termination signal (SIGINT/SIGTERM), the HTTP server stops accepting new socket connections, completes inflight requests, and terminates the ActorSystem cleanly.

#### Scenario: Server unbinds cleanly on termination signal
- **GIVEN** the HTTP server is active and bound to a network socket
- **WHEN** a JVM shutdown signal is received
- **THEN** the server unbinds the TCP socket via CoordinatedShutdown PhaseServiceUnbind
- **AND** the underlying ActorSystem terminates gracefully
