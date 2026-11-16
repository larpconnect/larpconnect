## ADDED Requirements

### Requirement: AMQP 1.0 Configuration and Connection Lifecycle Service
The `:queue` **Library module** (`com.larpconnect.njall.queue`) SHALL parse `njall.queue` settings
into `@Immutable` `QueueConfig` (requiring non-blank credentials when enabled) and manage AMQP 1.0
connectivity via `AmqpQueueService` (`com.rabbitmq.client:amqp-client`), declaring durable queue
`njall.inbox.activities` on startup and closing publisher/connection links on shutdown.

#### Scenario: Queue service connects over AMQP 1.0 and declares durable inbox queue
- **GIVEN** a running RabbitMQ 4.3+ broker with native AMQP 1.0 enabled and valid `QueueConfig`
  credentials
- **WHEN** `AmqpQueueService.startAsync()` is invoked and awaited
- **THEN** the service establishes an AMQP 1.0 connection, ensures durable queue
  `njall.inbox.activities` exists, and opens an active publisher link

#### Scenario: Missing broker password fails fast during configuration ingestion
- **GIVEN** a `njall.queue` configuration with `enabled = true` and a blank `password`
- **WHEN** `QueueConfig.fromConfig` parses the configuration
- **THEN** configuration ingestion throws an `IllegalStateException` identifying the missing queue
  credentials

### Requirement: Durable Single-Activity AMQP 1.0 Envelope Publication
`InboxActivityPublisher` SHALL publish one durable AMQP 1.0 JSON message (`InboxQueueEnvelope` with
`tenantId`, `studioId`, `receivingActorId`, `targetActorIds`, `receivedAt`, `activity`,
`messageId = activity.id`, and W3C `traceparent`) per accepted **FeatherPub** **Activity**,
succeeding only on broker `ACCEPTED` disposition and failing with `QueuePublishException` otherwise.

#### Scenario: Accepted activity is published as one durable AMQP 1.0 message with traceparent
- **GIVEN** an active `InboxActivityPublisher` connected to a RabbitMQ 4.3+ AMQP 1.0 broker
- **WHEN** an `InboxQueueEnvelope` for **Activity** `"https://remote.example/activities/01"` with
  resolved `targetActorIds` and active W3C `traceparent` is published
- **THEN** the broker confirms `ACCEPTED` disposition
- **AND** one durable JSON message arrives on the configured inbox queue with `messageId` equal to
  `"https://remote.example/activities/01"`, `contentType` equal to `"application/json"`, and the
  propagated `traceparent` property

#### Scenario: Broker rejection or timeout fails publication future
- **GIVEN** an `InboxActivityPublisher` whose underlying AMQP 1.0 broker connection is unavailable
  or rejects the message disposition
- **WHEN** `publish` is invoked with an `InboxQueueEnvelope`
- **THEN** the returned `CompletionStage` completes exceptionally with `QueuePublishException`

### Requirement: Caffeine-Cached AMQP 1.0 Broker Health Probe
The `:queue` **Library module** SHALL provide `AmqpHealthCheck` in
`com.larpconnect.njall.queue.health` bound into `Multibinder<HealthCheck>`, checking AMQP 1.0
connectivity on `AmqpQueueService` with a 1-second timeout and caching `Result` values for 10
seconds (`expireAfterWrite`) in Caffeine with an injectable `Duration` and `Ticker`.

#### Scenario: AMQP health check returns cached result within 10-second window
- **GIVEN** `AmqpHealthCheck` evaluated a connected AMQP 1.0 broker and cached a healthy `Result`
- **WHEN** `AmqpHealthCheck` is evaluated again within 10 seconds
- **THEN** the cached healthy `Result` is returned without issuing an additional broker round-trip

#### Scenario: AMQP health check reports unhealthy when broker connection is down
- **GIVEN** the cached probe window has expired and the AMQP 1.0 broker connection is closed or
  unreachable
- **WHEN** `AmqpHealthCheck` is evaluated
- **THEN** `AmqpHealthCheck` returns an unhealthy `Result` describing the broker failure and caches
  it for 10 seconds
