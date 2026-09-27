# http-server-runtime Specification

## Purpose

Provides a foundational Pekko HTTP runtime server with Typesafe configuration, graceful coordinated shutdown, and a root health/status endpoint.

## Requirements

### Requirement: Root Endpoint Serves Empty OK Response
The system SHALL accept HTTP GET requests at path `/` and return an HTTP 200 OK status code with an empty response body and a valid W3C `traceparent` response header.

#### Scenario: Client requests the root endpoint
- **GIVEN** the HTTP server is running
- **WHEN** a client sends an HTTP GET request to `/`
- **THEN** the response status code is 200 OK
- **AND** the response body is empty
- **AND** the response headers SHALL contain a valid `traceparent` header.

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

### Requirement: Studio Lookup Cache Service Lifecycle Coordination
The system SHALL coordinate the lifecycle of the `StudioLookupCacheService` within the `ServerManagerService`. During server startup, the system SHALL start `StudioLookupCacheService` and await its running state before binding the HTTP server socket, ensuring the in-memory cache is fully pre-warmed before incoming traffic is accepted. If initial cache pre-population fails, server startup SHALL abort. During coordinated shutdown, the system SHALL stop `StudioLookupCacheService` to terminate background scheduled refresh tasks before shutting down the actor system.

#### Scenario: Server starts cache service prior to HTTP binding
- **GIVEN** a configured `DefaultServerManagerService` with `StudioLookupCacheService` and `HttpServerService`
- **WHEN** the server manager service initiates startup
- **THEN** it starts `StudioLookupCacheService` and awaits the RUNNING state
- **AND** it initiates `HttpServerService.start()` only after the cache service is running

#### Scenario: Server stops cache service during coordinated shutdown
- **GIVEN** a running server manager service
- **WHEN** shutdown is initiated
- **THEN** the server unbinds HTTP sockets and stops `StudioLookupCacheService`
- **AND** the background scheduled refresh executor is cleanly terminated

