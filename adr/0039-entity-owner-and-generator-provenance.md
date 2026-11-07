# 0039: Universal Entity Owner Provenance and ActivityStreams Generator Contract

- Status: accepted, amends ADR-0027, ADR-0029, ADR-0033, ADR-0035, ADR-0036, ADR-0037, ADR-0038
- Date: 2026-11-07

## Context

In **Njall**, tenanted resources in the **User plane** use a Common Table Inheritance (**CTI**) hierarchy rooted at `njall_users.entities`, as established across [0027: Common Table Inheritance and Tenanted Links Architecture](0027-common-table-inheritance-and-tenanted-links-architecture.md), [0029: Locations and Addresses Spatial CTI Architecture](0029-locations-and-addresses-spatial-cti-architecture.md), [0033: Hashtags Three-Level CTI and Dual-ID Resolution](0033-hashtags-three-level-cti-and-dual-id-resolution.md), [0035: Tenanted Events Architecture](0035-tenanted-events-architecture.md), [0036: Tenanted Individuals Architecture](0036-tenanted-individuals-architecture.md), [0037: Tenanted Collections Architecture](0037-tenanted-collections-architecture.md), and [0038: Tenanted Actors and Federated Endpoints Architecture](0038-tenanted-actors-and-federated-endpoints-architecture.md).

Under **FeatherPub** and **ActivityStreams** conventions, every **Entity** in a **Studio** **Tenant** must be attributable to a generating **Actor**. Previously:
- `njall_users.entities` had no `owner` column, leaving links, hashtags, locations, events, and individuals without **Actor** provenance.
- [0038: Tenanted Actors and Federated Endpoints Architecture](0038-tenanted-actors-and-federated-endpoints-architecture.md) placed an `owner_id` column directly on `njall_users.actors` and exposed `ownerId` on the **Actor** REST contract, duplicating structural ownership on the subtype table.
- [0037: Tenanted Collections Architecture](0037-tenanted-collections-architecture.md) placed `owner_id` on `njall_users.collections` to represent semantic attachment (the **Entity** a **Collection** belongs to, such as an actor's inbox/outbox or a forum parent), which is distinct from the **Actor** that generated the **Collection**.

We need a uniform, immutable **Actor** provenance column on the **CTI** root table (`njall_users.entities.owner`) that is exposed across all studio REST API representations as an **ActivityStreams** `generator` URI while preserving semantic **Collection** attachment (`njall_users.collections.owner_id`).

## Decision

1. **Universal `owner` Column on `njall_users.entities`**:
   - Flyway migration `V12__entities_owner.sql` adds `owner UUID NOT NULL` to `njall_users.entities` with composite self-referencing foreign key `CONSTRAINT fk_entities_owner FOREIGN KEY (tenant_id, owner) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE` and index `idx_entities_owner` on `(tenant_id, owner DESC)`.
   - `EntityBaseEntity` maps `owner` with `nullable = false` and `updatable = false`. `owner` is strictly immutable after creation and cannot be modified via `PATCH`.
2. **Consolidation of `actors.owner_id` and Retention of `collections.owner_id`**:
   - Drop `owner_id`, `fk_actors_owner`, and `idx_actors_owner` from `njall_users.actors`, replacing subtype-level **Actor** ownership from ADR-0038 with `njall_users.entities.owner`.
   - Retain `njall_users.collections.owner_id` for semantic **Collection** attachment while using `njall_users.entities.owner` to record the **Actor** that generated the **Collection**.
3. **Application-Layer Validation of Actor Provenance**:
   - Database constraints enforce only that `(tenant_id, owner)` references a row in `njall_users.entities (tenant_id, id)`.
   - Tenanted **DAO** implementations (`DefaultAgentDAO`, `DefaultCollectionDAO`, `DefaultEventDAO`, `DefaultHashtagDAO`, `DefaultIndividualDAO`, `DefaultLinkDAO`, `DefaultLocationDAO`) validate within the active `@NjallUsers` transaction that `owner` resolves to an existing, non-deleted (`deleted_on IS NULL`) **Entity** in the same `tenant_id` with `entity_type = 'Actor'` (or self `owner = id` when creating a self-owned **Actor**).
4. **ActivityStreams `generator` API Contract**:
   - All **CTI** **Entity** REST responses (`ActorResponse`, `OrderedCollectionResponse`, `EventResponse`, `IndividualResponse`, `LinkResponse`, `LocationResponse`, `TagResponse`) and hydrated collection items (`ExpandedEntity`) include `"generator": "/api/studios/{studio-id}/v1/actors/{owner-id}"` as a URI string (never an embedded **Actor** object).
   - Creation (`POST`) requests accept `"generator"` as either a bare **Actor** UUID string or a canonical studio **Actor** URI (`/api/studios/{studio-id}/v1/actors/{actor-id}` matching the target `{studio-id}`):
     - `POST /api/studios/{studio-id}/v1/actors`: `generator` is optional and defaults to self-ownership (`owner = id`). Automatically provisioned `inbox` and `outbox` **Collections** set both `entities.owner = actor.id` and `collections.owner_id = actor.id`.
     - `POST /api/studios/{studio-id}/v1/collections`: `ownerId` is required and `generator` is optional; when `generator` is omitted, the system infers `generator = ownerId` (returning `400 Bad Request` if the resolved `generator` is not an active **Actor**).
     - `POST` for `events`, `individuals`, `links`, `locations`, and `tags` (including `tags:batchCreate`): `generator` is required on every creation request item.

## Consequences

- **Positive**: Every **CTI** **Entity** in **Njall** carries verifiable, tenant-isolated **Actor** provenance in `njall_users.entities.owner`; **Actor** self-ownership solves bootstrap creation without circular dependencies; **Collection** semantic attachment (`collections.owner_id`) is cleanly decoupled from structural generation (`entities.owner`); REST payloads conform to **ActivityStreams** `generator` URI conventions while accepting both UUIDs and URIs on input.
- **Negative**: Seed scripts and test fixtures creating non-**Actor** entities must first provision an active **Actor** in the same **Tenant** to serve as `owner`.

