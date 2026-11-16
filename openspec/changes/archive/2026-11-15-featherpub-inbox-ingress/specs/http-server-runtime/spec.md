## ADDED Requirements

### Requirement: AMQP Queue Service Lifecycle Coordination
When `njall.queue.enabled = true`, `DefaultServerManagerService` SHALL start `AmqpQueueService` and
await its `RUNNING` state before `HttpServerService.start()` binds the HTTP **Server** socket
(aborting startup if AMQP connection or queue declaration fails), and SHALL stop `AmqpQueueService`
during `CoordinatedShutdown` after HTTP unbinding and before terminating the `ActorSystem`.

#### Scenario: Server starts AMQP queue service prior to HTTP socket binding
- **GIVEN** a configured `DefaultServerManagerService` with `StudioLookupCacheService`,
  `AmqpQueueService`, and `HttpServerService`
- **WHEN** the **Server** manager service initiates startup
- **THEN** it starts `StudioLookupCacheService` and `AmqpQueueService` and awaits their `RUNNING`
  states
- **AND** it initiates `HttpServerService.start()` only after both services are running

#### Scenario: Server stops AMQP queue service cleanly during coordinated shutdown
- **GIVEN** a running **Server** manager service with an active `AmqpQueueService`
- **WHEN** shutdown is initiated
- **THEN** the **Server** unbinds HTTP sockets, stops `AmqpQueueService` (closing its AMQP 1.0
  publisher and connection), and stops `StudioLookupCacheService` before terminating the actor
  system
