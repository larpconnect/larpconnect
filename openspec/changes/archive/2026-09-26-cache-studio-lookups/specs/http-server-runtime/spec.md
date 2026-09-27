# http-server-runtime Specification Delta

## ADDED Requirements

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
