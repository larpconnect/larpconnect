# 0040: Actor-Parented Collections Routes and Entity Generator Column

- Status: accepted, supersedes ADR-0039, amends ADR-0037, ADR-0038
- Date: 2026-11-13
- Supersedes: 0039-entity-owner-and-generator-provenance.md

## Context

In **Njall**, tenanted resources in the **User plane** use a Common Table Inheritance hierarchy rooted at `njall_users.entities`, governed by [0005: Sealed DAO and Hibernate Dual Session Architecture](0005-sealed-dao-and-hibernate-dual-session-architecture.md), [0025: User Space Studios API and Tenant Row-Level Security](0025-user-space-studios-api-and-tenant-row-level-security.md), [0027: Common Table Inheritance and Tenanted Links Architecture](0027-common-table-inheritance-and-tenanted-links-architecture.md), [0029: Tenanted Locations and Addresses Architecture](0029-tenanted-locations-and-addresses-architecture.md), [0033: Federated Hashtags and Case-Insensitive Functional Indexing](0033-federated-hashtags-and-case-insensitive-functional-indexing.md), [0035: Tenanted Events and Location Associations Architecture](0035-tenanted-events-and-location-associations-architecture.md), [0036: Tenanted Individuals Architecture](0036-tenanted-individuals-architecture.md), [0037: Tenanted Collections Architecture](0037-tenanted-collections-architecture.md), and [0038: Tenanted Actors and Federated Endpoints Architecture](0038-tenanted-actors-and-federated-endpoints-architecture.md).

[0039: Universal Entity Owner Provenance and ActivityStreams Generator Contract](0039-entity-owner-and-generator-provenance.md) is revisited and superseded here to resolve two structural and naming defects:
1. **Unparented Collections URL Hierarchy**: Every **Collections** **Entity** has an immutable generating **Actor** (**generator**), yet ADR-0037 and ADR-0039 kept collection routes unparented at `/api/studios/{studio-id}/v1/collections` rather than nesting them under `/api/studios/{studio-id}/v1/actors/{actor-id}/collections` alongside `/inbox` and `/outbox`.
2. **Database and Domain Naming Collision (`owner` vs `generator`)**: ADR-0039 named the root provenance column `njall_users.entities.owner` (`UUID owner()` on Java domain records), colliding directly with `njall_users.collections.owner_id` (`UUID ownerId()` on `Collection`), where **owner** denotes semantic attachment (for example, attaching a collection to an **Individual** or **Events** record) and **generator** denotes structural **Actor** provenance.

## Considered Options

- **Option 1: Bind `/actors/{actor-id}/collections` to `entities.generator`, Retain Optional `collections.owner_id`, and Rename `entities.owner` to `generator` via Flyway `V13`** (Selected)
- **Option 2: Collapse `collections.owner_id` into `entities.generator`** (Rejected: prevents semantic attachment of collections to non-**Actor** entities such as individuals or events).
- **Option 3: Retain `njall_users.entities.owner` in SQL and Rename Only Java Accessors** (Rejected: leaves SQL queries, seed scripts, and domain terminology split across conflicting meanings of `owner`).

## Decision

1. **Universal `generator` Column on `njall_users.entities`**:
   - Flyway migration `V13__rename_entities_owner_to_generator.sql` renames `njall_users.entities.owner` (introduced in `V12`) to `generator`, renames composite self-referencing foreign key `fk_entities_owner` to `fk_entities_generator` (`FOREIGN KEY (tenant_id, generator) REFERENCES njall_users.entities (tenant_id, id) ON DELETE CASCADE`), and replaces `idx_entities_owner` with `idx_entities_generator` on `(tenant_id, generator DESC)`.
   - `EntityBaseEntity`, all Common Table Inheritance domain records (`Agent`, `Collection`, `Event`, `Hashtag`, `Individual`, `Link`, `Location`, `ExpandedEntity`), and tenanted **DAO** contracts map immutable `generator` (`nullable = false`, `updatable = false`).
   - `njall_users.actors` remains consolidated without a subtype `owner_id` column, while `njall_users.collections.owner_id` is retained for semantic **owner** attachment with uniqueness `(tenant_id, owner_id, name)`.
2. **Application-Layer Validation of Active Actor Provenance**:
   - Tenanted **DAO** implementations validate within the active `@NjallUsers` transaction (`ActorGeneratorValidator`) that `generator` resolves to an existing, non-deleted (`deleted_on IS NULL`) **Entity** in the same `tenant_id` with `entity_type = 'Actor'` (or self `generator = id` when creating a self-generated **Actor**).
3. **Actor-Parented Collections Routes and Subordinate Route Delegation** (amends ADR-0037, ADR-0038):
   - Tenanted **Collections** HTTP endpoints move from `/api/studios/{studio-id}/v1/collections` to `/api/studios/{studio-id}/v1/actors/{actor-id}/collections` (`POST ""`, `GET/PATCH/DELETE "/{collection-id}"`, `POST "/{collection-id}:addItem"`, `POST "/{collection-id}:removeItem"`). `GET /api/studios/{studio-id}/v1/actors/{actor-id}/collections` continues to return `404 Not Found`, and legacy `/api/studios/{studio-id}/v1/collections` routes are removed.
   - `AgentRoute` (`com.larpconnect.njall.api.studios.actors`) delegates `/collections` subroutes to `CollectionsRoute` (`com.larpconnect.njall.api.studios.collections`), removing `CollectionsRoute` from top-level `StudioSubRoutes` while preserving downward/outward package dependencies ([ADR-0011](0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md)) and package size limits ([ADR-0032](0032-package-size-limits-and-subpackage-decomposition.md)).
   - Every collection operation binds `{actor-id}` to `njall_users.entities.generator` and verifies in `CollectionDAO` that `{actor-id}` is an active `'Actor'` in the resolved **Studio** **Tenant** and equals `collection.generator()`, returning `404 Not Found` on missing, soft-deleted, non-**Actor**, or mismatched `{actor-id}`.
   - Endpoints for `tags`, `locations`, `events`, `individuals`, and `links` remain unparented under `/api/studios/{studio-id}/v1/...`.
4. **FeatherPub / ActivityStreams `generator` API Contract**:
   - All Common Table Inheritance **Entity** REST **DTO** responses and hydrated collection items (`ExpandedEntity`) emit `"generator": "/api/studios/{studio-id}/v1/actors/{generator-id}"` as a canonical URI string.
   - `POST /api/studios/{studio-id}/v1/actors/{actor-id}/collections`: `CreateCollectionRequest` omits `generator` (derived exclusively from `{actor-id}`) and accepts optional `ownerId` defaulting to `{actor-id}` when omitted; an explicit `ownerId` must resolve to an active **Entity** in the same **Tenant** or fail with `400 Bad Request`.
   - `POST /api/studios/{studio-id}/v1/actors`: `generator` (UUID or canonical studio **Actor** URI) remains optional and defaults to self-generation (`generator = id`); provisioned `inbox` and `outbox` records set both `entities.generator = actor.id` and `collections.owner_id = actor.id`.
   - `POST` for unparented `events`, `individuals`, `links`, `locations`, and `tags` (including `tags:batchCreate`): `generator` (UUID or canonical studio **Actor** URI) remains required.

## Consequences

- **Positive**: Good, because collection URLs express structural **Actor** parentage consistently alongside `/inbox` and `/outbox`; `Collection` records and SQL schemas separate `generator` (structural **Actor** provenance) from `ownerId` / `owner_id` (semantic **owner** attachment) without naming collisions; self-owned **Actor** collection creation simplifies to `{"name": "..."}` while preserving non-**Actor** semantic attachment.
- **Negative**: Bad, because existing API callers, OpenAPI definitions, seed SQL scripts, and Cucumber integration suites referencing `/v1/collections` or `entities.owner` require coordinated migration.

