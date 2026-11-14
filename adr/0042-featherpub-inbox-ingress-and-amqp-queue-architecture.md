# 0042: FeatherPub Inbox Ingress, Generator-Hierarchy Routing, and AMQP 1.0 Queue Architecture

- Status: accepted, amends ADR-0001, ADR-0007, ADR-0032, ADR-0038, ADR-0041
- Date: 2026-11-14

## Context

In **Njall**, tenanted **Studios** host **Actor** identities (`njall_users.actors` inheriting from
`njall_users.entities` with structural provenance `njall_users.entities.generator`) governed by
[0001: Pekko HTTP Runtime Architecture and Multimodule Layout](0001-pekko-http-runtime-architecture.md),
[0002: Dropwizard HealthCheck and Pekko Typed Health Actor Pattern](0002-dropwizard-healthcheck-actor-pattern.md),
[0007: Docker Compose Multi-Container Orchestration and Staged Packaging](0007-docker-compose-and-container-orchestration.md),
[0015: Docker Compose Secret Derivation and Local Environment Isolation](0015-docker-compose-secret-derivation-and-isolation.md),
[0017: OpenTelemetry SDK and Logback MDC Tracing Architecture](0017-opentelemetry-sdk-and-logback-mdc-tracing-architecture.md),
[0018: Caffeine-Cached Administrative Database Health Check](0018-caffeine-cached-database-health-check.md),
[0024: HAProxy Ingress Reverse Proxy and Network Segmentation](0024-haproxy-ingress-reverse-proxy-and-network-segmentation.md),
[0032: Package Size Limits and Subpackage Decomposition](0032-package-size-limits-and-subpackage-decomposition.md),
[0038: Tenanted Actors and Federated Endpoints Architecture](0038-tenanted-actors-and-federated-endpoints-architecture.md),
[0040: Actor-Parented Collections Routes and Entity Generator Column](0040-actor-parented-collections-and-entity-generator-column.md),
and [0041: Tenanted Systems, Campaigns, and Games Two-Level CTI and Domain Decomposition Architecture](0041-tenanted-systems-campaigns-and-games-architecture.md).

Replacing the placeholder `201 Created` `POST .../actors/{actor-id}/inbox` stub from
[ADR-0038](0038-tenanted-actors-and-federated-endpoints-architecture.md) with asynchronous
**FeatherPub** **Activity** ingress (`../docs/FEATHERPUB.md`) introduces four durable architectural
commitments:
1. **AMQP 1.0 Queue Boundary and `:queue` Module**: `AGENTS.md` mandates RabbitMQ 4.3+ over AMQP
   1.0, whereas the version catalog previously pointed at legacy AMQP 0-9-1
   (`com.rabbitmq:amqp-client`). Broker connectivity, durable publishing, and health probes require
   isolation in a dedicated `:queue` **Library module** and a `rabbitmq` service in Docker Compose.
2. **Pre-Queue Generator-Hierarchy Actor Routing**: Canonical **FeatherPub** URIs address **Actor**
   resources by UUID or `preferredUsername`, and any receiving **Actor** must route inbound
   **Activity** messages addressed to itself or any descendant **Actor** it transitively generated.
3. **AIP-233 Partial-Success Batch Ingress**: `POST .../inbox:batchMessage` must accept an AIP-233
   `{ "requests": [ ... ] }` envelope (not a **FeatherPub** object) and report partial failures per
   index rather than failing an entire batch atomically.
4. **Restart-Safe Eight-Day Deduplication & Package Capacity**: Sender retries span 7 days,
   requiring PostgreSQL-persisted 8-day deduplication fronted by Caffeine without breaching the
   20-type package cap in `api.studios.actors` or `data.domain.studios`.

## Considered Options

- **Option 1: Dedicated `:queue` Module (`com.rabbitmq.client:amqp-client` AMQP 1.0 + `rabbitmq:4.3-alpine`), Pre-Queue Recursive `generator` Ancestor Routing, AIP-233 Partial-Success `:batchMessage`, and Post-Publish Caffeine + PostgreSQL (`V15`) Deduplication in `inbox` Subpackages** (Selected)
- **Option 2: Retain `com.rabbitmq:amqp-client` `5.x` Inside `:api` and Deduplicate Only in Caffeine Memory** (Rejected: `com.rabbitmq:amqp-client` speaks AMQP 0-9-1 rather than AMQP 1.0, mixes broker lifecycles into `:api`, and loses 8-day idempotency across **Server** restarts).
- **Option 3: Blind Enqueue Before Routing/Validation or All-or-Nothing `:batchMessage` Atomic Rejection** (Rejected: prevents synchronous `400 Bad Request` / per-index `failedRequests` reporting for unroutable recipients and violates AIP-233 partial-success batch semantics).

## Decision

1. **`:queue` Library Module, RabbitMQ AMQP 1.0 Client, and Compose Broker** (amends
   [ADR-0001](0001-pekko-http-runtime-architecture.md),
   [ADR-0007](0007-docker-compose-and-container-orchestration.md)):
   - Add `:queue` (`com.larpconnect.njall.queue.{config,publisher,health}`, `QueueModule` installed
     by `ServerModule`) using `com.rabbitmq.client:amqp-client:1.7.0` over native AMQP 1.0.
   - Start `AmqpQueueService` (declaring durable queue `njall.inbox.activities` and opening a
     publisher link) before `HttpServerService` binds sockets, stop it during `CoordinatedShutdown`,
     and register a 10-second Caffeine-cached `AmqpHealthCheck` in `Multibinder<HealthCheck>`.
   - Publish 1 durable AMQP 1.0 JSON message (`InboxQueueEnvelope`) per accepted **Activity**
     carrying `tenantId`, `studioId`, `receivingActorId`, `targetActorIds`, `receivedAt`,
     `activity`, `messageId = activity.id`, and W3C `traceparent`, requiring broker `ACCEPTED`
     disposition.
   - Add `rabbitmq` (`rabbitmq:4.3-alpine`, internal port `5672`, `NJALL_DB_SECRET`-derived
     credentials `njall_amqp_${NJALL_DB_SECRET}`, healthcheck barrier before `server`) to
     `docker-compose.yml`.
2. **Inbox Ingress Endpoints, `preferredUsername` Resolution, and AIP-233 Partial-Success Batching**
   (amends [ADR-0038](0038-tenanted-actors-and-federated-endpoints-architecture.md)):
   - Replace the `201 Created` `POST .../inbox` stub with `POST .../inbox` and
     `POST .../inbox:message` returning `202 Accepted` (keeping `POST .../outbox` at `201 Created`
     and `GET .../inbox` at `200 OK` `OrderedCollection`), and resolve `{actor-id}` by UUID or
     case-insensitive `preferredUsername` on `GET /actors/{actor-id}`, `GET .../inbox`, and all
     inbox `POST` endpoints.
   - Validate optional `Content-Digest` (`sha-256`, `sha3-256`) and the union-free 10-verb
     **FeatherPub** **Activity** contract (`Create`, `Update`, `Delete`, `Add`, `Remove`, `Move`,
     `Offer`, `Accept`, `Reject`, `Announce`), same-origin/subdomain `id`-to-`actor` authority, and
     temporal bounds (5-minute future skew, 8-day age window).
   - Expose `POST .../inbox:batchMessage` accepting `{ "requests": [ ... ] }` (1..100 items, not a
     **FeatherPub** object), returning `202 Accepted` with index-keyed `failedRequests` when >= 1
     item is accepted or `400 Bad Request` (with `failedRequests`) when 0 items succeed.
3. **Pre-Queue Recursive `generator` Routing, `V15` Deduplication, and `inbox` Subpackages** (amends
   [ADR-0032](0032-package-size-limits-and-subpackage-decomposition.md),
   [ADR-0041](0041-tenanted-systems-campaigns-and-games-architecture.md)):
   - Decompose inbox components into `com.larpconnect.njall.api.studios.inbox` (`InboxModule`,
     delegated from `AgentRoute`), `com.larpconnect.njall.data.dao.studios.inbox` (`InboxDaoModule`),
     and `com.larpconnect.njall.data.domain.inbox`.
   - Resolve addressed local **Studio** recipients from `to`/`cc` via a cycle-safe `WITH RECURSIVE`
     upward walk on `njall_users.entities.generator` confirming active lineage to `{actor-id}`.
   - Persist 8-day deduplication rows in `njall_users.inbox_activities`
     (`V15__inbox_activity_deduplication.sql`, PK `(tenant_id, target_actor_id, activity_id)`, RLS
     `app.tenant_id`) fronted by a Caffeine L1 cache, recording keys strictly after broker
     `ACCEPTED` confirmation.

## Consequences

- **Positive**: Good, because native AMQP 1.0 queuing in `:queue` cleanly decouples HTTP ingress
  from downstream **Entity** state mutations, upward `generator` traversal routes multi-level
  generated **Actor** hierarchies in `O(recipients * depth)` without full-subtree scans, AIP-233
  `failedRequests` isolates bad batch items without aborting valid deliveries, and post-publish
  PostgreSQL + Caffeine deduplication survives **Server** restarts without dropping retried messages
  on transient broker failures.
- **Negative**: Bad, because changing `POST .../inbox` from `201 Created` arbitrary JSON to
  `202 Accepted` validated **FeatherPub** **Activity** payloads is a breaking contract change for
  callers relying on the placeholder stub, and adding `rabbitmq:4.3-alpine` introduces an additional
  stateful container dependency to `docker-compose.yml`.
